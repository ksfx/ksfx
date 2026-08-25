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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.AntPathMatcher;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
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

    /**
     * "Agentic" in the main nav used to land on a standalone list/table page (agent.html, since
     * removed) before you could actually talk to an agent - one extra click for the common case.
     * Now it goes straight to a chat window; the sidebar there already covers everything that page
     * offered (grouped agent list, New Agent, chat/edit navigation) except Delete, which moved to
     * the edit page, and Settings/Projects links, which moved to the sidebar footer.
     */
    @GetMapping("/")
    public String index()
    {
        return redirectToFirstAgentOrNew();
    }

    private String redirectToFirstAgentOrNew()
    {
        Page<Agent> firstPage = agentDAO.getAgentsForPageable(PageRequest.of(0, 1));

        if (firstPage.isEmpty()) {
            return "redirect:/agentic/edit";
        }

        return "redirect:/agentic/chat/" + firstPage.getContent().get(0).getId();
    }

    @GetMapping({"/edit", "/edit/{id}"})
    public String edit(@PathVariable(value = "id", required = false) Long agentId, Model model)
    {
        Agent agent = agentId != null ? agentDAO.getAgentForId(agentId) : new Agent();

        model.addAttribute("agent", agent);
        model.addAttribute("allAgenticProjects", agenticProjectDAO.getAllAgenticProjects());
        model.addAttribute("autoAppendedSystemPrompt", claudeCliSessionService.buildAutoAppendedSystemPrompt(agent));

        return "agentic/agent/agent_edit";
    }

    @PostMapping({"/edit", "/edit/{id}"})
    public String submit(@PathVariable(value = "id", required = false) Long agentId, @Valid @ModelAttribute Agent agent, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes)
    {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allAgenticProjects", agenticProjectDAO.getAllAgenticProjects());
            model.addAttribute("autoAppendedSystemPrompt", claudeCliSessionService.buildAutoAppendedSystemPrompt(agent));
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

        // Not a plain "redirect:/agentic/" - that would cost a second hop through index()'s own
        // redirect, and flash attributes don't survive a redirect they weren't re-added for.
        return redirectToFirstAgentOrNew();
    }

    @GetMapping("/edit/{id}/reset-session")
    public String resetSession(@PathVariable(value = "id") Long agentId, RedirectAttributes redirectAttributes)
    {
        Agent agent = agentDAO.getAgentForId(agentId);

        if (claudeCliSessionService.isRunning(agentId)) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Agent is currently busy with a turn - try again once it finishes.");
        } else {
            claudeCliSessionService.resetSession(agent);
            redirectAttributes.addFlashAttribute("resultMessage", "Session reset. The next message will start a new conversation; chat history is preserved.");
        }

        return "redirect:/agentic/edit/" + agentId;
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

        boolean agentRunning = claudeCliSessionService.isRunning(agentId);
        ClaudeCliSessionService.PartialTurn partialTurn = agentRunning ? claudeCliSessionService.getPartialTurn(agentId) : null;

        model.addAttribute("agent", agent);
        model.addAttribute("messages", messages);
        model.addAttribute("agentRunning", agentRunning);
        // Both null (not just agentRunning itself) when nothing's actually running yet - executeTurn
        // registers the RunningTurnState a moment after runningStatus, so there's a brief window
        // where isRunning() is already true but getPartialTurn() is still null; the template/JS
        // treat that the same as "no output yet, but still show the busy indicator".
        model.addAttribute("partialText", partialTurn != null ? partialTurn.getText() : null);
        model.addAttribute("partialToolActivity", partialTurn != null ? partialTurn.getToolActivityJson() : null);
        model.addAttribute("allAgenticProjects", allAgenticProjects);
        model.addAttribute("agentsByAgenticProject", agentsByAgenticProject);
        model.addAttribute("unassignedAgents", agentDAO.getAgentsWithoutAgenticProject());

        return "agentic/agent/agent_chat";
    }

    /**
     * Lets a freshly (re)loaded chat page catch up on a turn that's already running - see
     * ClaudeCliSessionService.attachToRunningTurn. GET, not POST like {@link #chatMessage}, since
     * this doesn't start anything, just subscribes to output a turn already in flight (started by
     * this same browser before navigating away, another tab, a schedule, or another agent
     * messaging this one) keeps producing - see agentic-chat.js's use of EventSource, which (unlike
     * the fetch()+ReadableStream the send flow needs for POST) works natively for a plain GET SSE
     * stream.
     */
    @GetMapping(value = "/chat/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter attach(@PathVariable(value = "id") Long agentId)
    {
        SseEmitter emitter = new SseEmitter(0L);

        if (!claudeCliSessionService.attachToRunningTurn(agentId, emitter)) {
            emitter.complete();
        }

        return emitter;
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
     * Manually ends a running turn - see ClaudeCliSessionService.stopTurn. Called from the sidebar
     * (agentic-chat.js), not tied to the currently-open agent's chat page.
     */
    @PostMapping("/chat/{id}/stop")
    @ResponseBody
    public Map<String, Boolean> stop(@PathVariable(value = "id") Long agentId)
    {
        return Collections.singletonMap("stopped", claudeCliSessionService.stopTurn(agentId));
    }

    /**
     * Deletes a single turn (one row - a user message, an assistant reply, a system error, or an
     * incoming agent-to-agent message; see {@link AgentMessage}'s own Javadoc) from this agent's
     * transcript. Scoped to the {@code id} in the URL, not just {@code messageId}, so a message
     * can't be deleted by guessing its id while looking at a different agent's chat page.
     */
    @PostMapping("/chat/{id}/message/{messageId}/delete")
    @ResponseBody
    public Map<String, Boolean> deleteMessage(@PathVariable(value = "id") Long agentId, @PathVariable Long messageId)
    {
        AgentMessage message = agentMessageDAO.getAgentMessageForId(messageId);

        if (message == null || message.getAgent() == null || !message.getAgent().getId().equals(agentId)) {
            return Collections.singletonMap("deleted", false);
        }

        agentMessageDAO.deleteAgentMessage(message);

        return Collections.singletonMap("deleted", true);
    }

    /**
     * Serves a file from the agent's workspace - either a chat attachment the user uploaded (see
     * ClaudeCliSessionService.saveAttachmentsAndBuildNote, under uploads/) or a file the agent
     * itself produced during a turn (see ClaudeCliSessionService's workspace-diffing in
     * executeTurn). The relative path can contain subfolders (e.g. "uploads/172...-report.pdf" or
     * "output/deck.pptx"), so this uses a trailing "/**" mapping + manual extraction rather than a
     * {fileName:.+} path variable - confirmed by testing that the latter does NOT span multiple
     * path segments on this Spring version, only the last one. "inline" disposition lets the
     * browser render viewable types (images, PDFs) directly in a new tab instead of forcing a
     * download.
     */
    @GetMapping("/download/{agentId}/**")
    @ResponseBody
    public ResponseEntity<Resource> download(@PathVariable(value = "agentId") Long agentId, HttpServletRequest request) throws IOException
    {
        String bestMatchingPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String pathWithinHandlerMapping = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String fileName = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, pathWithinHandlerMapping);

        Agent agent = agentDAO.getAgentForId(agentId);
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        if (agent == null || config == null) {
            return ResponseEntity.notFound().build();
        }

        Path workspaceDir = agentWorkspaceService.resolveWorkspace(agent, config).normalize();
        Path target = workspaceDir.resolve(fileName).normalize();

        if (!target.startsWith(workspaceDir) || !Files.isRegularFile(target)) {
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
