package ch.ksfx.services.project;

import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.dao.ProjectMembershipDAO;
import ch.ksfx.dao.user.UserDAO;
import ch.ksfx.model.Project;
import ch.ksfx.model.ProjectMembership;
import ch.ksfx.model.ProjectMembershipLevel;
import ch.ksfx.model.user.Role;
import ch.ksfx.model.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * The one rule for what a USER may do with a project (concept "Project membership for users",
 * section 9): three questions - read, write, manage - answered from the user's membership level,
 * with instance administrators (role ADMIN) allowed everything everywhere. The counterpart of
 * AgentScope for agents. Step 1 uses it for the switcher, the session context and the admin
 * screens; step 2 put the require* guards in front of every entity lookup and write endpoint.
 */
@Service
public class UserScope
{
    public static final String ADMIN_ROLE = "ADMIN";

    private final UserDAO userDAO;
    private final ProjectDAO projectDAO;
    private final ProjectMembershipDAO membershipDAO;

    public UserScope(UserDAO userDAO, ProjectDAO projectDAO, ProjectMembershipDAO membershipDAO)
    {
        this.userDAO = userDAO;
        this.projectDAO = projectDAO;
        this.membershipDAO = membershipDAO;
    }

    /** The logged-in user, re-read from the DB (the principal is the User loaded at login and may be stale), or null. */
    public User currentUser()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User principal = (User) authentication.getPrincipal();

            return principal.getId() != null ? userDAO.getUser(principal.getId()) : null;
        }

        return null;
    }

    public boolean isAdmin(User user)
    {
        if (user == null || user.getRoles() == null) {
            return false;
        }

        for (Role role : user.getRoles()) {
            if (ADMIN_ROLE.equals(role.getName())) {
                return true;
            }
        }

        return false;
    }

    public ProjectMembershipLevel levelOf(User user, Project project)
    {
        if (user == null || project == null || project.getId() == null) {
            return null;
        }

        ProjectMembership membership = membershipDAO.getMembership(user.getId(), project.getId());

        return membership != null ? membership.getLevel() : null;
    }

    public boolean canRead(User user, Project project)
    {
        return isAdmin(user) || levelOf(user, project) != null;
    }

    public boolean canWrite(User user, Project project)
    {
        ProjectMembershipLevel level = levelOf(user, project);

        return isAdmin(user) || level == ProjectMembershipLevel.WRITER || level == ProjectMembershipLevel.OWNER;
    }

    public boolean canManage(User user, Project project)
    {
        return isAdmin(user) || levelOf(user, project) == ProjectMembershipLevel.OWNER;
    }

    // ---- step 2: the same three questions as guards. A failed guard is a 403 (error/403.html);
    // the controllers call these right where they resolve an entity or are about to write.

    public void requireRead(Project project)
    {
        if (!canRead(currentUser(), project)) {
            throw forbidden("You are not a member of this project.");
        }
    }

    public void requireWrite(Project project)
    {
        if (!canWrite(currentUser(), project)) {
            throw forbidden("You may read this project but not change it.");
        }
    }

    public void requireManage(Project project)
    {
        if (!canManage(currentUser(), project)) {
            throw forbidden("Only a project owner may configure this project.");
        }
    }

    public void requireAdmin()
    {
        if (!isAdmin(currentUser())) {
            throw forbidden("Instance administrators only.");
        }
    }

    /**
     * Read for a GET, write for anything else - the guard for the shared "resolve the entity by id"
     * helpers that both the view and the save handlers go through (requireWiki, requireTracker,
     * follow(), ...). Writes that travel as GET links (run, delete, schedule...) call requireWrite
     * themselves.
     */
    public void requireAccess(Project project)
    {
        if (isReadRequest()) {
            requireRead(project);
        } else {
            requireWrite(project);
        }
    }

    private boolean isReadRequest()
    {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        String method = attributes instanceof ServletRequestAttributes ? ((ServletRequestAttributes) attributes).getRequest().getMethod() : "GET";
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method);
    }

    private ResponseStatusException forbidden(String reason)
    {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, reason);
    }

    /** The projects the user may see, in name order - all of them for an administrator. */
    public List<Project> accessibleProjects(User user)
    {
        if (user == null) {
            return new ArrayList<>();
        }

        if (isAdmin(user)) {
            return projectDAO.getAllProjects();
        }

        List<Project> projects = new ArrayList<>();

        for (ProjectMembership membership : membershipDAO.getMembershipsForUser(user.getId())) {
            projects.add(membership.getProject());
        }

        return projects;
    }
}
