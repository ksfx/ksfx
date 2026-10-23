package ch.ksfx.controller.agentic;

import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.Project;
import ch.ksfx.services.agentic.ProjectPortsService;
import ch.ksfx.services.project.UserScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * Async listen-status for the breadcrumb ports dropdown (see ProjectPortsService): fetched by the
 * browser when the dropdown opens, never during page rendering. Knocks only the project's own
 * derived ports; members only.
 */
@RestController
public class ProjectPortsController
{
    private final ProjectDAO projectDAO;
    private final ProjectPortsService projectPortsService;
    private final UserScope userScope;

    public ProjectPortsController(ProjectDAO projectDAO, ProjectPortsService projectPortsService, UserScope userScope)
    {
        this.projectDAO = projectDAO;
        this.projectPortsService = projectPortsService;
        this.userScope = userScope;
    }

    @GetMapping("/agentic/projects/{projectId}/ports/status")
    public Map<Integer, Boolean> status(@PathVariable Long projectId)
    {
        Project project = projectDAO.getProjectForId(projectId);

        if (project == null || !project.getDockerIsolationEnabled()) {
            return Collections.emptyMap();
        }

        userScope.requireRead(project);

        return projectPortsService.knock(project);
    }
}
