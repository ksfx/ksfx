package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.AgenticConfigDAO;
import ch.ksfx.dao.AgenticProjectDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.model.AgenticProject;
import ch.ksfx.services.agentic.AgentWorkspaceService;
import ch.ksfx.services.agentic.AgenticDockerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;

@Controller
@RequestMapping("/agentic/projects")
public class AgenticProjectController
{
    private final AgenticProjectDAO agenticProjectDAO;
    private final AgentDAO agentDAO;
    private final AgenticConfigDAO agenticConfigDAO;
    private final AgentWorkspaceService agentWorkspaceService;
    private final AgenticDockerService agenticDockerService;

    public AgenticProjectController(AgenticProjectDAO agenticProjectDAO,
                                     AgentDAO agentDAO,
                                     AgenticConfigDAO agenticConfigDAO,
                                     AgentWorkspaceService agentWorkspaceService,
                                     AgenticDockerService agenticDockerService)
    {
        this.agenticProjectDAO = agenticProjectDAO;
        this.agentDAO = agentDAO;
        this.agenticConfigDAO = agenticConfigDAO;
        this.agentWorkspaceService = agentWorkspaceService;
        this.agenticDockerService = agenticDockerService;
    }

    @GetMapping("/")
    public String index(Pageable pageable, Model model)
    {
        Page<AgenticProject> agenticProjectsPage = agenticProjectDAO.getAgenticProjectsForPageable(pageable);

        model.addAttribute("agenticProjectsPage", agenticProjectsPage);

        return "agentic/project/agentic_project";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    public String edit(@PathVariable(value = "id", required = false) Long id, Model model)
    {
        AgenticProject agenticProject = id != null ? agenticProjectDAO.getAgenticProjectForId(id) : new AgenticProject();

        // Refresh the displayed container status rather than showing a possibly-stale DB snapshot - a
        // plain read-only inspect (never ensureContainer, which would resurrect a container the user
        // just explicitly Stopped just because this page happened to reload).
        if (agenticProject.getId() != null && agenticProject.getDockerIsolationEnabled()) {
            agenticDockerService.refreshStatus(agenticProject);
        }

        model.addAttribute("agenticProject", agenticProject);
        addDockerSetupDescription(agenticProject, model);

        return "agentic/project/agentic_project_edit";
    }

    @PostMapping({"/edit", "/edit/{id}"})
    public String submit(@PathVariable(value = "id", required = false) Long id, @Valid @ModelAttribute AgenticProject agenticProject, BindingResult bindingResult, Model model)
    {
        if (bindingResult.hasErrors()) {
            addDockerSetupDescription(agenticProject, model);
            return "agentic/project/agentic_project_edit";
        }

        // dockerContainerName/Status/LastCheckedAt and createdAt aren't form fields - without
        // copying them forward from the persisted row, every save would silently wipe them back to
        // their Java defaults (same class of bug already fixed for AgentController/AgenticConfigController).
        AgenticProject previous = agenticProject.getId() != null ? agenticProjectDAO.getAgenticProjectForId(agenticProject.getId()) : null;

        if (previous != null) {
            agenticProject.setDockerContainerName(previous.getDockerContainerName());
            agenticProject.setDockerContainerStatus(previous.getDockerContainerStatus());
            agenticProject.setDockerContainerLastCheckedAt(previous.getDockerContainerLastCheckedAt());
            agenticProject.setCreatedAt(previous.getCreatedAt());
        } else {
            agenticProject.setCreatedAt(new Date());
        }

        boolean turningOn = agenticProject.getDockerIsolationEnabled() && (previous == null || !previous.getDockerIsolationEnabled());

        agenticProjectDAO.saveOrUpdateAgenticProject(agenticProject);

        if (turningOn) {
            AgenticConfig config = agenticConfigDAO.getAgenticConfig();

            if (config != null) {
                try {
                    agenticDockerService.ensureContainer(agenticProject, config);
                } catch (IOException ignored) {
                    // non-fatal - the save already succeeded; ensureContainer also runs lazily
                    // pre-turn (see ClaudeCliSessionService.executeTurn) as the real safety net
                }
            }
        }

        return "redirect:/agentic/projects/edit/" + agenticProject.getId();
    }

    /**
     * Read-only "what do I actually get" preview of the exact docker commands Enable Docker
     * Isolation runs - see AgenticDockerService.describeSetup, which is pure/side-effect-free so
     * this preview can never drift from what actually executes (same pattern as
     * ClaudeCliSessionService.buildAutoAppendedSystemPrompt's system-prompt preview). Shown
     * regardless of whether isolation is currently on, since the point is to inform the decision to
     * turn it on in the first place; silently skipped if Agentic isn't configured yet.
     */
    private void addDockerSetupDescription(AgenticProject agenticProject, Model model)
    {
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        if (config != null) {
            model.addAttribute("dockerSetupDescription", agenticDockerService.describeSetup(agenticProject, config));
        }
    }

    @GetMapping("/{id}/docker/start")
    public String dockerStart(@PathVariable(value = "id") Long id, RedirectAttributes redirectAttributes)
    {
        AgenticProject agenticProject = agenticProjectDAO.getAgenticProjectForId(id);
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        try {
            agenticDockerService.ensureContainer(agenticProject, config);
            redirectAttributes.addFlashAttribute("resultMessage", "Container started.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Could not start container: " + e.getMessage());
        }

        return "redirect:/agentic/projects/edit/" + id;
    }

    @GetMapping("/{id}/docker/stop")
    public String dockerStop(@PathVariable(value = "id") Long id, RedirectAttributes redirectAttributes)
    {
        AgenticProject agenticProject = agenticProjectDAO.getAgenticProjectForId(id);

        try {
            agenticDockerService.stop(agenticProject);
            redirectAttributes.addFlashAttribute("resultMessage", "Container stopped.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Could not stop container: " + e.getMessage());
        }

        return "redirect:/agentic/projects/edit/" + id;
    }

    @GetMapping("/{id}/docker/throwaway")
    public String dockerThrowAway(@PathVariable(value = "id") Long id, RedirectAttributes redirectAttributes)
    {
        AgenticProject agenticProject = agenticProjectDAO.getAgenticProjectForId(id);
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        try {
            agenticDockerService.throwAway(agenticProject, config);
            redirectAttributes.addFlashAttribute("resultMessage", "Container thrown away and rebuilt.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Could not rebuild container: " + e.getMessage());
        }

        return "redirect:/agentic/projects/edit/" + id;
    }

    /**
     * Unassigns and physically relocates each currently-assigned agent's workspace back to the
     * flat, ungrouped layout before removing the project row, so nothing is orphaned or blocked -
     * deleting a project just leaves its former agents ungrouped. The DB's own ON DELETE SET NULL
     * is only a backstop; this loop is the real mechanism.
     */
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable(value = "id") Long id, RedirectAttributes redirectAttributes) throws IOException
    {
        AgenticProject agenticProject = agenticProjectDAO.getAgenticProjectForId(id);
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        for (Agent agent : agentDAO.getAgentsForAgenticProject(id)) {
            if (config != null) {
                Path oldWorkspace = agentWorkspaceService.resolveWorkspace(agent, config);
                agent.setAgenticProject(null);
                Path newWorkspace = agentWorkspaceService.resolveWorkspace(agent, config);

                agentWorkspaceService.moveWorkspaceIfNeeded(oldWorkspace, newWorkspace);
                agent.setWorkspacePath(newWorkspace.toString());
            } else {
                agent.setAgenticProject(null);
            }

            agentDAO.saveOrUpdateAgent(agent);
        }

        if (agenticProject.getDockerIsolationEnabled()) {
            agenticDockerService.deleteContainer(agenticProject); // never throws - never blocks this cascade
        }

        agenticProjectDAO.deleteAgenticProject(agenticProject);

        redirectAttributes.addFlashAttribute("resultMessage", "Agentic Project deleted.");

        return "redirect:/agentic/projects/";
    }
}
