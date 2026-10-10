package ch.ksfx.controller.agentic;

import org.springframework.web.bind.annotation.RequestParam;
import ch.ksfx.services.project.ProjectDeletionService;
import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.AgenticConfigDAO;
import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.model.Project;
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
public class ProjectController
{
    private final ProjectDAO projectDAO;
    private final AgentDAO agentDAO;
    private final AgenticConfigDAO agenticConfigDAO;
    private final AgentWorkspaceService agentWorkspaceService;
    private final AgenticDockerService agenticDockerService;
    private final ProjectDeletionService projectDeletionService;

    public ProjectController(ProjectDAO projectDAO,
                                     AgentDAO agentDAO,
                                     AgenticConfigDAO agenticConfigDAO,
                                     AgentWorkspaceService agentWorkspaceService,
                                     AgenticDockerService agenticDockerService,
                                     ProjectDeletionService projectDeletionService)
    {
        this.projectDAO = projectDAO;
        this.agentDAO = agentDAO;
        this.agenticConfigDAO = agenticConfigDAO;
        this.agentWorkspaceService = agentWorkspaceService;
        this.agenticDockerService = agenticDockerService;
        this.projectDeletionService = projectDeletionService;
    }

    @GetMapping("/")
    public String index(Pageable pageable, Model model)
    {
        Page<Project> projectsPage = projectDAO.getProjectsForPageable(pageable);

        model.addAttribute("projectsPage", projectsPage);

        return "agentic/project/agentic_project";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    public String edit(@PathVariable(value = "id", required = false) Long id, Model model)
    {
        Project project = id != null ? projectDAO.getProjectForId(id) : new Project();

        // Refresh the displayed container status rather than showing a possibly-stale DB snapshot - a
        // plain read-only inspect (never ensureContainer, which would resurrect a container the user
        // just explicitly Stopped just because this page happened to reload).
        if (project.getId() != null && project.getDockerIsolationEnabled()) {
            agenticDockerService.refreshStatus(project);
        }

        model.addAttribute("project", project);
        addDockerSetupDescription(project, model);

        return "agentic/project/agentic_project_edit";
    }

    @PostMapping({"/edit", "/edit/{id}"})
    public String submit(@PathVariable(value = "id", required = false) Long id, @Valid @ModelAttribute Project project, BindingResult bindingResult, Model model)
    {
        if (bindingResult.hasErrors()) {
            addDockerSetupDescription(project, model);
            return "agentic/project/agentic_project_edit";
        }

        // dockerContainerName/Status/LastCheckedAt and createdAt aren't form fields - without
        // copying them forward from the persisted row, every save would silently wipe them back to
        // their Java defaults (same class of bug already fixed for AgentController/AgenticConfigController).
        Project previous = project.getId() != null ? projectDAO.getProjectForId(project.getId()) : null;

        if (previous != null) {
            project.setDockerContainerName(previous.getDockerContainerName());
            project.setDockerContainerStatus(previous.getDockerContainerStatus());
            project.setDockerContainerLastCheckedAt(previous.getDockerContainerLastCheckedAt());
            project.setCreatedAt(previous.getCreatedAt());
        } else {
            project.setCreatedAt(new Date());
        }

        boolean turningOn = project.getDockerIsolationEnabled() && (previous == null || !previous.getDockerIsolationEnabled());

        projectDAO.saveOrUpdateProject(project);

        if (turningOn) {
            AgenticConfig config = agenticConfigDAO.getAgenticConfig();

            if (config != null) {
                try {
                    agenticDockerService.ensureContainer(project, config);
                } catch (IOException ignored) {
                    // non-fatal - the save already succeeded; ensureContainer also runs lazily
                    // pre-turn (see ClaudeCliSessionService.executeTurn) as the real safety net
                }
            }
        }

        return "redirect:/agentic/projects/edit/" + project.getId();
    }

    /**
     * Read-only "what do I actually get" preview of the exact docker commands Enable Docker
     * Isolation runs - see AgenticDockerService.describeSetup, which is pure/side-effect-free so
     * this preview can never drift from what actually executes (same pattern as
     * ClaudeCliSessionService.buildAutoAppendedSystemPrompt's system-prompt preview). Shown
     * regardless of whether isolation is currently on, since the point is to inform the decision to
     * turn it on in the first place; silently skipped if Agentic isn't configured yet.
     */
    private void addDockerSetupDescription(Project project, Model model)
    {
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        if (config != null) {
            model.addAttribute("dockerSetupDescription", agenticDockerService.describeSetup(project, config));
        }
    }

    @GetMapping("/{id}/docker/start")
    public String dockerStart(@PathVariable(value = "id") Long id, RedirectAttributes redirectAttributes)
    {
        Project project = projectDAO.getProjectForId(id);
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        try {
            agenticDockerService.ensureContainer(project, config);
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
        Project project = projectDAO.getProjectForId(id);

        try {
            agenticDockerService.stop(project);
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
        Project project = projectDAO.getProjectForId(id);
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        try {
            agenticDockerService.throwAway(project, config);
            redirectAttributes.addFlashAttribute("resultMessage", "Container thrown away and rebuilt.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Could not rebuild container: " + e.getMessage());
        }

        return "redirect:/agentic/projects/edit/" + id;
    }

    /**
     * Step 1 of deleting a project: the confirmation page with everything that would go
     * (ProjectDeletionService#summary) and the type-the-name field. Deleting cascades through ALL
     * of the project's content since 2026-10-10 - before that it only ungrouped the agents.
     */
    @GetMapping("/delete/{id}")
    public String deleteConfirm(@PathVariable(value = "id") Long id, Model model, RedirectAttributes redirectAttributes)
    {
        Project project = projectDAO.getProjectForId(id);

        if (project == null) {
            return "redirect:/agentic/projects/";
        }

        String blocker = projectDeletionService.deletionBlocker(project);

        if (blocker != null) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", blocker);
            return "redirect:/agentic/projects/";
        }

        model.addAttribute("project", project);
        model.addAttribute("summary", projectDeletionService.summary(project));

        return "agentic/project/agentic_project_delete";
    }

    /** Step 2: only with the project's exact name typed in. */
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable(value = "id") Long id, @RequestParam(value = "confirmName", required = false) String confirmName,
                         RedirectAttributes redirectAttributes)
    {
        Project project = projectDAO.getProjectForId(id);

        if (project == null) {
            return "redirect:/agentic/projects/";
        }

        if (confirmName == null || !confirmName.trim().equals(project.getName())) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Name did not match - nothing was deleted.");
            return "redirect:/agentic/projects/delete/" + id;
        }

        try {
            projectDeletionService.delete(project);
            redirectAttributes.addFlashAttribute("resultMessage", "Project '" + project.getName() + "' and all its content deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Could not delete project: " + e.getMessage());
        }

        return "redirect:/agentic/projects/";
    }
}
