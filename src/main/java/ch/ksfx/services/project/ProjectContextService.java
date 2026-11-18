package ch.ksfx.services.project;

import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.Project;
import ch.ksfx.model.user.User;

import java.util.List;
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
    private final UserScope userScope;

    public ProjectContextService(ProjectDAO projectDAO, UserScope userScope)
    {
        this.projectDAO = projectDAO;
        this.userScope = userScope;
    }

    /**
     * The session's project, validated against the user's memberships (step 1 of the membership
     * concept): a project the user may not read is replaced by the first one they may, the
     * default project is only the fallback for users who may read it (administrators always).
     * Null when the user has no project at all - NoProjectInterceptor sends those to /noproject
     * before any controller runs, so controllers may rely on a non-null context.
     */
    public Project current(HttpSession session)
    {
        User user = userScope.currentUser();
        Object id = session != null ? session.getAttribute(SESSION_ATTRIBUTE) : null;
        Project project = id instanceof Long ? projectDAO.getProjectForId((Long) id) : null;

        if (project != null && user != null && !userScope.canRead(user, project)) {
            project = null;
        }

        if (project == null) {
            Project fallback = projectDAO.getDefaultProject();

            if (user != null && !userScope.canRead(user, fallback)) {
                List<Project> accessible = userScope.accessibleProjects(user);
                fallback = accessible.isEmpty() ? null : accessible.get(0);
            }

            project = fallback;

            if (project != null && session != null) {
                session.setAttribute(SESSION_ATTRIBUTE, project.getId());
            }
        }

        return project;
    }

    /** Only projects the user may read become the context; anything else is silently ignored (the entity pages themselves answer with 403, see follow()). */
    public void set(HttpSession session, Project project)
    {
        if (session != null && project != null && project.getId() != null) {
            User user = userScope.currentUser();

            if (user == null || userScope.canRead(user, project)) {
                session.setAttribute(SESSION_ATTRIBUTE, project.getId());
            }
        }
    }

    /** Same as {@link #set} - named for its intent at the call sites that open a specific entity. */
    public void follow(HttpSession session, Project project)
    {
        userScope.requireAccess(project); // step 2: a deep link into a foreign project is a 403, a POST into a read-only one too
        set(session, project);
    }

    /** For templates: may the user change the current project's content (render-time, so correct after a deep link was followed)? */
    public boolean canWrite()
    {
        User user = userScope.currentUser();
        Project project = current();
        return user != null && project != null && userScope.canWrite(user, project);
    }

    /** For templates: may the user configure the current project? */
    public boolean canManage()
    {
        User user = userScope.currentUser();
        Project project = current();
        return user != null && project != null && userScope.canManage(user, project);
    }

    /** {@link #follow} for code that has no session at hand (controller helpers): the current request's session. */
    public void follow(Project project)
    {
        follow(currentSession(), project);
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
