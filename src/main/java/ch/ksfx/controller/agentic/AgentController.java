package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.AgentMessageDAO;
import ch.ksfx.dao.AgentScheduleDAO;
import ch.ksfx.dao.AgenticConfigDAO;
import ch.ksfx.dao.AgenticProjectDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgentMessage;
import ch.ksfx.model.AgentSchedule;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.model.AgenticProject;
import ch.ksfx.services.agentic.AgentWorkspaceService;
import ch.ksfx.services.agentic.ClaudeCliSessionService;
import ch.ksfx.services.scheduler.SchedulerService;
import ch.ksfx.util.StacktraceUtil;
import org.quartz.SchedulerException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/agentic")
public class AgentController
{
    private final AgentDAO agentDAO;
    private final AgentMessageDAO agentMessageDAO;
    private final AgenticConfigDAO agenticConfigDAO;
    private final AgenticProjectDAO agenticProjectDAO;
    private final AgentWorkspaceService agentWorkspaceService;
    private final ClaudeCliSessionService claudeCliSessionService;
    private final AgentScheduleDAO agentScheduleDAO;
    private final SchedulerService schedulerService;

    public AgentController(AgentDAO agentDAO,
                            AgentMessageDAO agentMessageDAO,
                            AgenticConfigDAO agenticConfigDAO,
                            AgenticProjectDAO agenticProjectDAO,
                            AgentWorkspaceService agentWorkspaceService,
                            ClaudeCliSessionService claudeCliSessionService,
                            AgentScheduleDAO agentScheduleDAO,
                            SchedulerService schedulerService)
    {
        this.agentDAO = agentDAO;
        this.agentMessageDAO = agentMessageDAO;
        this.agenticConfigDAO = agenticConfigDAO;
        this.agenticProjectDAO = agenticProjectDAO;
        this.agentWorkspaceService = agentWorkspaceService;
        this.claudeCliSessionService = claudeCliSessionService;
        this.agentScheduleDAO = agentScheduleDAO;
        this.schedulerService = schedulerService;
    }

    @GetMapping("/")
    public String index(Pageable pageable, Model model)
    {
        Page<Agent> agentsPage = agentDAO.getAgentsForPageable(pageable);

        model.addAttribute("agentsPage", agentsPage);

        return "agentic/agent/agent";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    public String edit(@PathVariable(value = "id", required = false) Long agentId, Model model)
    {
        Agent agent = agentId != null ? agentDAO.getAgentForId(agentId) : new Agent();

        model.addAttribute("agent", agent);
        model.addAttribute("allAgenticProjects", agenticProjectDAO.getAllAgenticProjects());

        return "agentic/agent/agent_edit";
    }

    @PostMapping({"/edit", "/edit/{id}"})
    public String submit(@PathVariable(value = "id", required = false) Long agentId, @Valid @ModelAttribute Agent agent, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes)
    {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allAgenticProjects", agenticProjectDAO.getAllAgenticProjects());
            return "agentic/agent/agent_edit";
        }

        boolean isNew = agent.getId() == null;

        // agent is a freshly-bound object containing only the submitted form fields - workspacePath,
        // claudeSessionId and apiToken are read-only display values, not <input>s, so without this
        // they'd be silently wiped to null on every edit.
        Agent previousAgent = isNew ? null : agentDAO.getAgentForId(agent.getId());

        if (previousAgent != null) {
            agent.setWorkspacePath(previousAgent.getWorkspacePath());
            agent.setClaudeSessionId(previousAgent.getClaudeSessionId());
            agent.setApiToken(previousAgent.getApiToken());
            agent.setCreatedAt(previousAgent.getCreatedAt());
        }

        // An unselected "-- No Agentic Project --" option binds to a stub with a null id (see
        // NoteController's identical NoteCategory handling) rather than a null agenticProject.
        if (agent.getAgenticProject() != null && agent.getAgenticProject().getId() == null) {
            agent.setAgenticProject(null);
        }

        if (isNew) {
            agent.setCreatedAt(new Date());
            agent.setApiToken(java.util.UUID.randomUUID().toString().replace("-", ""));
        }

        agentDAO.saveOrUpdateAgent(agent);

        if (isNew) {
            AgenticConfig config = agenticConfigDAO.getAgenticConfig();

            if (config != null && config.getWorkspaceRoot() != null && !config.getWorkspaceRoot().isEmpty()) {
                try {
                    agent.setWorkspacePath(agentWorkspaceService.ensureWorkspace(agent, config).toString());
                    agentDAO.saveOrUpdateAgent(agent);
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("resultError", true);
                    redirectAttributes.addFlashAttribute("resultMessage", "Agent created, but workspace directory could not be created: " + e.getMessage() + StacktraceUtil.getStackTrace(e));
                }
            }
        } else if (previousAgent != null) {
            AgenticConfig config = agenticConfigDAO.getAgenticConfig();

            if (config != null) {
                try {
                    Path oldWorkspace = agentWorkspaceService.resolveWorkspace(previousAgent, config);
                    Path newWorkspace = agentWorkspaceService.resolveWorkspace(agent, config);

                    agentWorkspaceService.moveWorkspaceIfNeeded(oldWorkspace, newWorkspace);
                    agent.setWorkspacePath(newWorkspace.toString());
                    agentDAO.saveOrUpdateAgent(agent);
                } catch (IOException e) {
                    redirectAttributes.addFlashAttribute("resultError", true);
                    redirectAttributes.addFlashAttribute("resultMessage", "Agent updated, but workspace could not be moved: " + e.getMessage());
                }
            }
        }

        return "redirect:/agentic/edit/" + agent.getId();
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable(value = "id") Long agentId, RedirectAttributes redirectAttributes) throws SchedulerException
    {
        Agent agent = agentDAO.getAgentForId(agentId);

        // Remove any live Quartz jobs for this agent's schedules before the cascade delete removes
        // their rows, so nothing orphaned lingers in the in-memory scheduler until next restart.
        for (AgentSchedule schedule : agentScheduleDAO.getSchedulesForAgent(agentId)) {
            schedulerService.deleteJob("AgentSchedule" + schedule.getId(), "AgentSchedules");
        }

        agentDAO.deleteAgent(agent);

        redirectAttributes.addFlashAttribute("resultMessage", "Agent deleted.");

        return "redirect:/agentic/";
    }

    @GetMapping("/chat/{id}")
    public String chat(@PathVariable(value = "id") Long agentId, Model model)
    {
        Agent agent = agentDAO.getAgentForId(agentId);
        List<AgentMessage> messages = agentMessageDAO.getMessagesForAgent(agentId);

        List<AgenticProject> allAgenticProjects = agenticProjectDAO.getAllAgenticProjects();
        Map<Long, List<Agent>> agentsByAgenticProject = new LinkedHashMap<>();

        for (AgenticProject p : allAgenticProjects) {
            agentsByAgenticProject.put(p.getId(), agentDAO.getAgentsForAgenticProject(p.getId()));
        }

        model.addAttribute("agent", agent);
        model.addAttribute("messages", messages);
        model.addAttribute("agentRunning", claudeCliSessionService.isRunning(agentId));
        model.addAttribute("allAgenticProjects", allAgenticProjects);
        model.addAttribute("agentsByAgenticProject", agentsByAgenticProject);
        model.addAttribute("unassignedAgents", agentDAO.getAgentsWithoutAgenticProject());

        return "agentic/agent/agent_chat";
    }

    @PostMapping(value = "/chat/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter chatMessage(@PathVariable(value = "id") Long agentId,
                                   @RequestParam("message") String message,
                                   @RequestParam(value = "files", required = false) MultipartFile[] files)
    {
        SseEmitter emitter = new SseEmitter(0L);

        claudeCliSessionService.runTurn(agentId, message, files, emitter);

        return emitter;
    }

    /**
     * Polled by the sidebar (agentic-chat.js) so agents that are currently working show a live
     * status even when you're not looking at their chat. agentId -> status text, running agents only.
     */
    @GetMapping("/status")
    @ResponseBody
    public Map<Long, String> status()
    {
        return claudeCliSessionService.getAllStatuses();
    }

    /**
     * Serves a file previously uploaded as a chat attachment (see ClaudeCliSessionService.
     * saveAttachmentsAndBuildNote, which stores it under the agent's workspace uploads/ dir and
     * records {@code fileName} there as its stored name). "inline" disposition lets the browser
     * render viewable types (images, PDFs) directly in a new tab instead of forcing a download.
     */
    @GetMapping("/download/{agentId}/{fileName:.+}")
    @ResponseBody
    public ResponseEntity<Resource> download(@PathVariable(value = "agentId") Long agentId, @PathVariable(value = "fileName") String fileName) throws IOException
    {
        Agent agent = agentDAO.getAgentForId(agentId);
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        if (agent == null || config == null) {
            return ResponseEntity.notFound().build();
        }

        Path uploadsDir = agentWorkspaceService.resolveWorkspace(agent, config).resolve("uploads").normalize();
        Path target = uploadsDir.resolve(fileName).normalize();

        if (!target.startsWith(uploadsDir) || !Files.isRegularFile(target)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(target);
        MediaType contentType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        String safeFileName = target.getFileName().toString().replaceAll("[\r\n\"]", "");

        // Uploads are written as-is from the browser's multipart body, which is UTF-8 for any
        // text typed/generated on the web - without this, browsers fall back to Latin-1 for
        // inline "text/*" display and mangle non-ASCII characters (e.g. German umlauts).
        if ("text".equals(contentType.getType())) {
            contentType = new MediaType(contentType.getType(), contentType.getSubtype(), java.nio.charset.StandardCharsets.UTF_8);
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFileName + "\"")
                .body(resource);
    }
}
