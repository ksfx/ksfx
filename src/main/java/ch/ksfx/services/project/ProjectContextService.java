package ch.ksfx.services.project;

import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.Project;
import org.springframework.stereotype.Service;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpSession;

/**
 * The "current project" of a browser session - the context every project-scoped page works in
 * (concept, section 3). Held in the session like the display mode; defaults to the default
 * project. "Follow deep links": a page that opens a specific entity calls {@link #follow}, so
 * opening another project's wiki or agent switches the context to that project instead of
 * refusing - URLs stay stable and shareable.
 */
@Service
public class ProjectContextService
{
    public static final String SESSION_ATTRIBUTE = "currentProjectId";

    private final ProjectDAO projectDAO;

    public ProjectContextService(ProjectDAO projectDAO)
    {
        this.projectDAO = projectDAO;
    }

    public Project current(HttpSession session)
    {
        Object id = session != null ? session.getAttribute(SESSION_ATTRIBUTE) : null;
        Project project = id instanceof Long ? projectDAO.getProjectForId((Long) id) : null;

        if (project == null) {
            project = projectDAO.getDefaultProject();

            if (project != null && session != null) {
                session.setAttribute(SESSION_ATTRIBUTE, project.getId());
            }
        }

        return project;
    }

    public void set(HttpSession session, Project project)
    {
        if (session != null && project != null && project.getId() != null) {
            session.setAttribute(SESSION_ATTRIBUTE, project.getId());
        }
    }

    /** Same as {@link #set} - named for its intent at the call sites that open a specific entity. */
    public void follow(HttpSession session, Project project)
    {
        set(session, project);
    }

    /** {@link #follow} for code that has no session at hand (controller helpers): the current request's session. */
    public void follow(Project project)
    {
        set(currentSession(), project);
    }

    public Project current()
    {
        return current(currentSession());
    }

    private HttpSession currentSession()
    {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        return attributes instanceof ServletRequestAttributes ? ((ServletRequestAttributes) attributes).getRequest().getSession(true) : null;
    }
}
