package ch.ksfx.controller.dashboard;

import ch.ksfx.model.Project;
import ch.ksfx.services.project.ProjectContextService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/** The current project's dashboard (phase 3 of the project concept, 2026-10-12 - one global dashboard before). */
@Controller
@RequestMapping("/dashboard")
public class DashboardController
{
    private final ProjectContextService projectContextService;

    public DashboardController(ProjectContextService projectContextService)
    {
        this.projectContextService = projectContextService;
    }

    @RequestMapping("/")
    public String index(Model model)
    {
        Project project = projectContextService.current();

        model.addAttribute("dashboardContent", project != null && project.getDashboardContent() != null ? project.getDashboardContent() : "");

        return "dashboard/dashboard";
    }
}
