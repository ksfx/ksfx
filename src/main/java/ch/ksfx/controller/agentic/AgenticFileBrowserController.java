package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.AgenticConfigDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.services.agentic.AgentWorkspaceService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Web file browser over an agent's workspace directory (and, for project-grouped agents, the
 * project's shared directory as a second root - see AgentWorkspaceService for the layout). Kept
 * deliberately separate from AgentController: everything here is new surface, nothing existing
 * changes behavior. Works identically for Docker-isolated and plain agents because the container
 * bind-mounts the exact same host directory this browses (AgenticDockerService's -v mount).
 *
 * Same access model as the rest of /agentic/**: any authenticated KSFX user, no per-agent
 * ownership (deliberate for this personal-tool stage - see WebSecurityConfig).
 *
 * All mutating/list endpoints return the refreshed directory listing fragment for the current
 * directory, so the JS side (agentic-file-browser.js) is a dumb "swap the overlay body" loop with
 * one round trip per action.
 */
@Controller
@RequestMapping("/agentic/files")
public class AgenticFileBrowserController
{
    private static final String ROOT_WORKSPACE = "workspace";
    private static final String ROOT_SHARED = "shared";

    private final AgentDAO agentDAO;
    private final AgenticConfigDAO agenticConfigDAO;
    private final AgentWorkspaceService agentWorkspaceService;

    public AgenticFileBrowserController(AgentDAO agentDAO, AgenticConfigDAO agenticConfigDAO, AgentWorkspaceService agentWorkspaceService)
    {
        this.agentDAO = agentDAO;
        this.agenticConfigDAO = agenticConfigDAO;
        this.agentWorkspaceService = agentWorkspaceService;
    }

    @GetMapping("/{agentId}/list")
    public String list(@PathVariable Long agentId,
                       @RequestParam(defaultValue = ROOT_WORKSPACE) String root,
                       @RequestParam(defaultValue = "") String path,
                       Model model)
    {
        return renderListing(agentId, root, path, model);
    }

    @PostMapping("/{agentId}/upload")
    public String upload(@PathVariable Long agentId,
                         @RequestParam(defaultValue = ROOT_WORKSPACE) String root,
                         @RequestParam(defaultValue = "") String path,
                         @RequestParam("files") MultipartFile[] files,
                         Model model) throws IOException
    {
        Path directory = resolveDirectory(agentId, root, path);

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // getFileName() strips any client-supplied directory components; unlike chat
            // attachments (see ClaudeCliSessionService.sanitizeFilename) the remaining name is
            // kept as-is - a file manager should preserve umlauts etc., and the traversal check
            // below still rejects anything that escapes the directory.
            String fileName = java.nio.file.Paths.get(file.getOriginalFilename()).getFileName().toString();
            Path target = directory.resolve(fileName).normalize();

            if (!target.startsWith(directory)) {
                continue;
            }

            try (java.io.InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        return renderListing(agentId, root, path, model);
    }

    @PostMapping("/{agentId}/mkdir")
    public String mkdir(@PathVariable Long agentId,
                        @RequestParam(defaultValue = ROOT_WORKSPACE) String root,
                        @RequestParam(defaultValue = "") String path,
                        @RequestParam String name,
                        Model model) throws IOException
    {
        Path directory = resolveDirectory(agentId, root, path);

        Files.createDirectory(directory.resolve(requireSimpleName(name)));

        return renderListing(agentId, root, path, model);
    }

    /** Renames one direct child of the current directory; path stays on the directory itself. */
    @PostMapping("/{agentId}/rename")
    public String rename(@PathVariable Long agentId,
                         @RequestParam(defaultValue = ROOT_WORKSPACE) String root,
                         @RequestParam(defaultValue = "") String path,
                         @RequestParam String name,
                         @RequestParam String newName,
                         Model model) throws IOException
    {
        Path directory = resolveDirectory(agentId, root, path);
        Path source = directory.resolve(requireSimpleName(name));

        Files.move(source, directory.resolve(requireSimpleName(newName)));

        return renderListing(agentId, root, path, model);
    }

    /** Deletes one direct child of the current directory - recursively for directories. */
    @PostMapping("/{agentId}/delete")
    public String delete(@PathVariable Long agentId,
                         @RequestParam(defaultValue = ROOT_WORKSPACE) String root,
                         @RequestParam(defaultValue = "") String path,
                         @RequestParam String name,
                         Model model) throws IOException
    {
        Path directory = resolveDirectory(agentId, root, path);
        Path target = directory.resolve(requireSimpleName(name));

        if (Files.exists(target)) {
            // Depth-first (reverse order) so directories are empty by the time they're deleted.
            try (Stream<Path> walk = Files.walk(target)) {
                List<Path> toDelete = walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList());

                for (Path p : toDelete) {
                    Files.delete(p);
                }
            }
        }

        return renderListing(agentId, root, path, model);
    }

    /**
     * Download supporting both roots - the existing /agentic/download/{agentId}/** endpoint only
     * knows the agent workspace, and extending it would mean touching proven code; a second,
     * self-contained endpoint here keeps this feature isolated (same inline-disposition/UTF-8
     * conventions as that one, see AgentController.download()).
     */
    @GetMapping("/{agentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long agentId,
                                             @RequestParam(defaultValue = ROOT_WORKSPACE) String root,
                                             @RequestParam String path)
    {
        Path base;

        try {
            base = resolveRoot(agentId, root);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }

        Path target = base.resolve(path).normalize();

        if (!target.startsWith(base) || !Files.isRegularFile(target)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(target);
        MediaType contentType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        String safeFileName = target.getFileName().toString().replaceAll("[\r\n\"]", "");

        if ("text".equals(contentType.getType())) {
            contentType = new MediaType(contentType.getType(), contentType.getSubtype(), java.nio.charset.StandardCharsets.UTF_8);
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFileName + "\"")
                .body(resource);
    }

    private String renderListing(Long agentId, String root, String path, Model model)
    {
        Agent agent = agentDAO.getAgentForId(agentId);
        Path directory = resolveDirectory(agentId, root, path);
        Path base = resolveRoot(agentId, root);

        String relativePath = base.relativize(directory).toString().replace('\\', '/');

        List<FileEntry> entries = new ArrayList<>();

        try (Stream<Path> children = Files.list(directory)) {
            children.forEach(child -> entries.add(toEntry(child, base)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        entries.sort(Comparator.comparing((FileEntry e) -> !e.directory).thenComparing(e -> e.name.toLowerCase()));

        model.addAttribute("agent", agent);
        model.addAttribute("browserRoot", root);
        model.addAttribute("browserPath", relativePath);
        model.addAttribute("browserParentPath", relativePath.isEmpty() ? null : parentOf(relativePath));
        model.addAttribute("browserEntries", entries);
        model.addAttribute("hasSharedRoot", agent.getAgenticProject() != null);

        return "agentic/agent/agentic_file_browser :: fileList";
    }

    private String parentOf(String relativePath)
    {
        int lastSlash = relativePath.lastIndexOf('/');

        return lastSlash < 0 ? "" : relativePath.substring(0, lastSlash);
    }

    private FileEntry toEntry(Path child, Path base)
    {
        FileEntry entry = new FileEntry();
        entry.name = child.getFileName().toString();
        entry.path = base.relativize(child).toString().replace('\\', '/');
        entry.directory = Files.isDirectory(child);

        try {
            entry.size = entry.directory ? null : humanSize(Files.size(child));
            entry.modified = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(Files.getLastModifiedTime(child).toMillis()));
        } catch (IOException e) {
            entry.size = null;
            entry.modified = "";
        }

        return entry;
    }

    private String humanSize(long bytes)
    {
        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }

        if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }

        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /** The requested directory, traversal-checked against the chosen root - throws on escape attempts. */
    private Path resolveDirectory(Long agentId, String root, String path)
    {
        Path base = ensureRootExists(resolveRoot(agentId, root));
        Path directory = base.resolve(path == null ? "" : path).normalize();

        if (!directory.startsWith(base) || !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Not a browsable directory");
        }

        return directory;
    }

    private Path resolveRoot(Long agentId, String root)
    {
        Agent agent = agentDAO.getAgentForId(agentId);

        if (agent == null) {
            throw new IllegalArgumentException("Unknown agent " + agentId);
        }

        // Primary source is workspaceRoot-derived resolution (canonical, follows the config), but
        // an instance whose /agentic/config/ was never saved still has each agent's absolute path
        // persisted on the row itself (set at creation, see AgentController) - browsing should
        // keep working there, the chat page renders without a config too.
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();
        boolean configUsable = config != null && config.getWorkspaceRoot() != null && !config.getWorkspaceRoot().trim().isEmpty();
        boolean storedPathUsable = agent.getWorkspacePath() != null && !agent.getWorkspacePath().trim().isEmpty();

        if (ROOT_SHARED.equals(root)) {
            if (agent.getAgenticProject() == null) {
                throw new IllegalArgumentException("Agent has no project shared directory");
            }

            if (configUsable) {
                return agentWorkspaceService.resolveAgenticProjectWorkspace(agent.getAgenticProject(), config).resolve("shared").normalize();
            }

            if (storedPathUsable) {
                // workspacePath is <...>/project-<pid>/agent-<id> for project agents - shared sits
                // next to the agent directory (see AgentWorkspaceService.ensureWorkspace).
                return java.nio.file.Paths.get(agent.getWorkspacePath()).normalize().getParent().resolve("shared").normalize();
            }

            throw new IllegalArgumentException("Agentic is not configured on this instance - save /agentic/config/ once");
        }

        if (!ROOT_WORKSPACE.equals(root)) {
            throw new IllegalArgumentException("Unknown root");
        }

        if (configUsable) {
            return agentWorkspaceService.resolveWorkspace(agent, config).normalize();
        }

        if (storedPathUsable) {
            return java.nio.file.Paths.get(agent.getWorkspacePath()).normalize();
        }

        throw new IllegalArgumentException("Agentic is not configured on this instance - save /agentic/config/ once");
    }

    /**
     * The workspace/shared directory only comes into existence when an agent is created through
     * the UI or runs its first turn - an agent predating this feature (or a fresh instance with a
     * different workspaceRoot) may not have one yet, which used to surface as a bare HTTP 500 on
     * opening the browser. Creating it on demand is what ensureWorkspace would do anyway.
     */
    private Path ensureRootExists(Path base)
    {
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return base;
    }

    /** A plain name inside the current directory - no separators, no traversal, non-empty. */
    private String requireSimpleName(String name)
    {
        if (name == null || name.trim().isEmpty() || name.contains("/") || name.contains("\\") || name.equals(".") || name.equals("..")) {
            throw new IllegalArgumentException("Invalid name");
        }

        return name.trim();
    }

    /**
     * Surfaces the actual reason (unknown root, escape attempt, missing directory, ...) as a 400
     * with plain-text body instead of a bare 500 - agentic-file-browser.js shows the body in its
     * error alert, so a failure is diagnosable from the browser alone (same lesson as the voice
     * input's error alert).
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e)
    {
        return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(e.getMessage());
    }

    public static class FileEntry
    {
        public String name;
        public String path;
        public boolean directory;
        public String size;
        public String modified;

        public String getName() { return name; }
        public String getPath() { return path; }
        public boolean getDirectory() { return directory; }
        public String getSize() { return size; }
        public String getModified() { return modified; }
    }
}
