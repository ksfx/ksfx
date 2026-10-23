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

    /**
     * Caddy's on-demand-TLS "ask" endpoint (PUBLIC - permitAll in WebSecurityConfig): before
     * issuing a certificate for a port-link subdomain, Caddy calls this with ?domain=...; 200
     * allows, 404 refuses. Valid = the configured port link template matches and the embedded
     * port is mapped by some Docker-isolated project - independent of whether anything runs on it
     * (the cert should exist before the first dev server starts). Exposes nothing but a boolean
     * over a self-chosen domain name.
     */
    @GetMapping("/ports/ask")
    public org.springframework.http.ResponseEntity<String> ask(@org.springframework.web.bind.annotation.RequestParam(required = false) String domain)
    {
        if (projectPortsService.isValidPortLinkDomain(domain)) {
            return org.springframework.http.ResponseEntity.ok("ok");
        }

        return org.springframework.http.ResponseEntity.status(404).body("unknown domain");
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
