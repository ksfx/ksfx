package ch.ksfx.services.agentic;

import java.nio.file.StandardCopyOption;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.model.Project;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves and creates the per-agent working directory that the headless Claude CLI process runs
 * in. This directory is the only v1 containment boundary for a subprocess that otherwise has the
 * same OS-level privileges as the KSFX server itself.
 */
@Service
public class AgentWorkspaceService
{
    public Path resolveProjectWorkspace(Project project, AgenticConfig config)
    {
        return Paths.get(config.getWorkspaceRoot(), "project-" + project.getId());
    }

    public Path resolveWorkspace(Agent agent, AgenticConfig config)
    {
        if (agent.getProject() == null) {
            return Paths.get(config.getWorkspaceRoot(), "agent-" + agent.getId());
        }

        return resolveProjectWorkspace(agent.getProject(), config).resolve("agent-" + agent.getId());
    }

    /** Container-side mount point of a project's provider directory (see {@link #resolveProjectProviderDir}). */
    public static final String CONTAINER_PROVIDER_DIR = "/provider/claude";

    /**
     * Where the Claude CLI keeps an agent's state - transcripts (needed for --resume), memory,
     * sessions, settings: {@code <workspace root>/provider/claude/project-<id>/agent-<id>}, passed
     * to the CLI as CLAUDE_CONFIG_DIR (concept "Backing up agent and project workspaces", KSFX
     * wiki). Next to the workspaces, not inside them: provider state is infrastructure the agent
     * should neither see nor delete through its own tools, and it must survive a container Throw
     * Away and land in the workspace backup. The project level exists because a container gets
     * only its own project's subtree mounted; the agent level because sessions and memory are per
     * agent. Future providers get their own namespace under {@code provider/}.
     */
    public Path resolveProjectProviderDir(Project project, AgenticConfig config)
    {
        return Paths.get(config.getWorkspaceRoot(), "provider", "claude", "project-" + project.getId());
    }

    public Path resolveProviderDir(Agent agent, AgenticConfig config)
    {
        if (agent.getProject() == null) {
            return Paths.get(config.getWorkspaceRoot(), "provider", "claude", "agent-" + agent.getId());
        }

        return resolveProjectProviderDir(agent.getProject(), config).resolve("agent-" + agent.getId());
    }

    /** The same directory as seen from inside the project's container (the project subtree is mounted at {@link #CONTAINER_PROVIDER_DIR}). */
    public String containerProviderDir(Agent agent)
    {
        return CONTAINER_PROVIDER_DIR + "/agent-" + agent.getId();
    }

    /**
     * Transcript retention for platform agents (KSFX issue #12). The CLI deletes transcripts older
     * than cleanupPeriodDays (default 30) - for a human CLI user reasonable, for an agent that rests
     * for a month (spiderings may run for years untouched) it silently destroys the resume thread:
     * the next turn fails with "No conversation found with session ID". 3650 is the CLI's own
     * suggestion for "keep for a long time". Context length is unaffected - the CLI compacts on its
     * own; this only decides whether the thread still exists at all.
     */
    static final int TRANSCRIPT_RETENTION_DAYS = 3650;

    private static final ObjectMapper SETTINGS_MAPPER = new ObjectMapper();

    public Path ensureProviderDir(Agent agent, AgenticConfig config) throws IOException
    {
        Path providerDir = resolveProviderDir(agent, config);

        Files.createDirectories(providerDir);
        ensureTranscriptRetention(providerDir);

        return providerDir;
    }

    /**
     * Pins cleanupPeriodDays in the agent's settings.json (runs before every turn, idempotent):
     * missing file -> created with just this key; file without the key -> key merged in, every
     * other key kept; key already present (any value, e.g. set deliberately) -> untouched. A file
     * that cannot be parsed is left alone - the CLI itself pauses its cleanup while a settings file
     * is unreadable, so nothing is deleted in that state either. Never fails the turn.
     */
    void ensureTranscriptRetention(Path providerDir)
    {
        Path settingsFile = providerDir.resolve("settings.json");

        try {
            ObjectNode settings;

            if (Files.isRegularFile(settingsFile)) {
                JsonNode existing = SETTINGS_MAPPER.readTree(settingsFile.toFile());

                if (!(existing instanceof ObjectNode) || existing.has("cleanupPeriodDays")) {
                    return;
                }

                settings = (ObjectNode) existing;
            } else {
                settings = SETTINGS_MAPPER.createObjectNode();
            }

            settings.put("cleanupPeriodDays", TRANSCRIPT_RETENTION_DAYS);

            Path temp = providerDir.resolve("settings.json.ksfx-tmp");
            SETTINGS_MAPPER.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), settings);
            Files.move(temp, settingsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            // Unparseable or unwritable: leave it - see the method comment.
        }
    }

    public Path ensureWorkspace(Agent agent, AgenticConfig config) throws IOException
    {
        Path workspace = resolveWorkspace(agent, config);

        Files.createDirectories(workspace);

        if (agent.getProject() != null) {
            Files.createDirectories(resolveProjectWorkspace(agent.getProject(), config).resolve("shared"));
        }

        return workspace;
    }

    /**
     * Physically relocates an agent's workspace after its Project assignment changes, so
     * existing files/history aren't silently orphaned. No-op if the paths are already the same or
     * the source doesn't exist yet (agent never ran); refuses to clobber an existing destination.
     */
    public void moveWorkspaceIfNeeded(Path oldWorkspace, Path newWorkspace) throws IOException
    {
        if (oldWorkspace.equals(newWorkspace) || !Files.exists(oldWorkspace) || Files.exists(newWorkspace)) {
            return;
        }

        Files.createDirectories(newWorkspace.getParent());
        Files.move(oldWorkspace, newWorkspace);
    }
}
