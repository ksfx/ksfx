package ch.ksfx.controller.admin.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import ch.ksfx.services.project.UserScope;
import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.Project;
import ch.ksfx.services.project.ProjectContextService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Edits the CURRENT project's dashboard (Project.dashboardContent) - reached from the project form or the Admin menu. */
@Controller
@RequestMapping("/admin/dashboardmanager")
public class DashboardManagerController
{
    @Autowired
    private UserScope userScope;

    private final ProjectDAO projectDAO;
    private final ProjectContextService projectContextService;

    public DashboardManagerController(ProjectDAO projectDAO, ProjectContextService projectContextService)
    {
        this.projectDAO = projectDAO;
        this.projectContextService = projectContextService;
    }

    @GetMapping("/")
    public String editDashboardContent(Model model)
    {
        Project project = projectContextService.current();
        userScope.requireManage(project);
        model.addAttribute("project", project);
        model.addAttribute("dashboardContent", project.getDashboardContent() != null ? project.getDashboardContent() : "");

        return "admin/dashboard/dashboard_manager";
    }

    @PostMapping("/")
    public String editDashboardContent(@RequestParam(value = "content", required = true) String content)
    {
        Project project = projectDAO.getProjectForId(projectContextService.current().getId());
        userScope.requireManage(project);
        project.setDashboardContent(content);
        projectDAO.saveOrUpdateProject(project);

        return "redirect:/admin/dashboardmanager/";
    }
}
