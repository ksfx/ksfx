package ch.ksfx.controller.api;

import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.ApiClient;
import ch.ksfx.model.Project;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Project resolution for the external API (phase 3 of the project concept, 2026-10-12). Three
 * tiers, first match wins: an explicit {@code projectId} in the request, the calling API client's
 * bound project (ApiClient.project), the default project. Listings use the same tiers but fall
 * back to "all projects" instead of the default project when neither is given - an unbound client
 * without a filter keeps seeing everything, as before.
 */
@Component
public class ApiProjectScope
{
    private final ProjectDAO projectDAO;

    public ApiProjectScope(ProjectDAO projectDAO)
    {
        this.projectDAO = projectDAO;
    }

    public static class UnknownProjectException extends RuntimeException
    {
        public UnknownProjectException(Long projectId)
        {
            super("No project with id " + projectId);
        }
    }

    /** The project a create/update should land in. Never null. Throws for an unknown explicit id. */
    public Project forWrite(Long requestedProjectId)
    {
        Project explicit = explicit(requestedProjectId);

        if (explicit != null) {
            return explicit;
        }

        Project bound = boundProject();

        return bound != null ? bound : projectDAO.getDefaultProject();
    }

    /** The project id a listing is limited to, or null for "all projects". Throws for an unknown explicit id. */
    public Long forList(Long requestedProjectId)
    {
        Project explicit = explicit(requestedProjectId);

        if (explicit != null) {
            return explicit.getId();
        }

        Project bound = boundProject();

        return bound != null ? bound.getId() : null;
    }

    private Project explicit(Long requestedProjectId)
    {
        if (requestedProjectId == null) {
            return null;
        }

        Project project = projectDAO.getProjectForId(requestedProjectId);

        if (project == null) {
            throw new UnknownProjectException(requestedProjectId);
        }

        return project;
    }

    private Project boundProject()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof ApiClient) {
            ApiClient client = (ApiClient) authentication.getPrincipal();

            // re-read: the principal was loaded at authentication time and may carry a lazy reference
            return client.getProject() != null ? projectDAO.getProjectForId(client.getProject().getId()) : null;
        }

        return null;
    }
}
