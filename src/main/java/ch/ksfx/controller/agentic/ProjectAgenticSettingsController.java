package ch.ksfx.controller.agentic;

import ch.ksfx.dao.ProjectAgenticSettingsDAO;
import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.Project;
import ch.ksfx.model.ProjectAgenticSettings;
import ch.ksfx.services.agentic.AgenticAccessService;
import ch.ksfx.services.agentic.AgenticUsageStatsService;
import ch.ksfx.services.project.UserScope;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.time.LocalDate;

/**
 * A project's Agentic settings (concept "Agentic settings per project"): policy defaults,
 * enablement, the project's accesses (accounts, managed by AgenticAccessController) with their
 * rate-limit snapshots, and its usage view. OWNER-only
 * (UserScope.requireManage - instance admins included), and the OWNER sees the key in plaintext:
 * the project's key is the project's property, and a write-only field would only obscure a value
 * an OWNER could extract through an agent's environment anyway. No inheritance - what this page
 * shows is the whole truth about what the project's agents use.
 */
@Controller
@RequestMapping("/agentic/projects/{projectId}/settings")
public class ProjectAgenticSettingsController
{
    private final ProjectDAO projectDAO;
    private final ProjectAgenticSettingsDAO settingsDAO;
    private final AgenticUsageStatsService usageStatsService;
    private final AgenticAccessService agenticAccessService;
    private final UserScope userScope;

    public ProjectAgenticSettingsController(ProjectDAO projectDAO, ProjectAgenticSettingsDAO settingsDAO,
                                            AgenticUsageStatsService usageStatsService, AgenticAccessService agenticAccessService,
                                            UserScope userScope)
    {
        this.projectDAO = projectDAO;
        this.settingsDAO = settingsDAO;
        this.usageStatsService = usageStatsService;
        this.agenticAccessService = agenticAccessService;
        this.userScope = userScope;
    }

    @GetMapping("/")
    public String index(@PathVariable Long projectId,
                        @RequestParam(value = "statsDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statsDate,
                        Model model)
    {
        Project project = requireProject(projectId);
        ProjectAgenticSettings settings = settingsDAO.getForProjectId(projectId);

        if (settings == null) {
            settings = new ProjectAgenticSettings();
            settings.setProject(project);
        }

        model.addAttribute("project", project);
        model.addAttribute("settings", settings);
        model.addAttribute("accessCards", agenticAccessService.cardsFor(project));
        usageStatsService.addUsageStats(model, statsDate != null ? statsDate : LocalDate.now(), projectId);

        return "agentic/project/agentic_project_settings";
    }

    @PostMapping("/")
    public String save(@PathVariable Long projectId, @Valid @ModelAttribute("settings") ProjectAgenticSettings settings,
                       BindingResult bindingResult, Model model)
    {
        Project project = requireProject(projectId);

        if (bindingResult.hasErrors()) {
            model.addAttribute("project", project);
            model.addAttribute("accessCards", agenticAccessService.cardsFor(project));
            usageStatsService.addUsageStats(model, LocalDate.now(), projectId);

            return "agentic/project/agentic_project_settings";
        }

        ProjectAgenticSettings previous = settingsDAO.getForProjectId(projectId);

        if (previous != null) {
            settings.setId(previous.getId());
        }

        settings.setProject(project);
        settingsDAO.saveOrUpdateProjectAgenticSettings(settings);

        return "redirect:/agentic/projects/" + projectId + "/settings/";
    }

    private Project requireProject(Long projectId)
    {
        Project project = projectDAO.getProjectForId(projectId);

        if (project == null) {
            throw new IllegalArgumentException("No project with id " + projectId);
        }

        userScope.requireManage(project); // the account is project configuration - OWNER only (concept, section 5)

        return project;
    }
}
