package ch.ksfx.controller.admin.projects;

import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.dao.ProjectMembershipDAO;
import ch.ksfx.dao.user.UserDAO;
import ch.ksfx.model.Project;
import ch.ksfx.model.ProjectMembership;
import ch.ksfx.model.ProjectMembershipLevel;
import ch.ksfx.model.user.User;
import ch.ksfx.services.project.UserScope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Who belongs to which project at which level - instance administrators only (concept, Q2: an
 * OWNER manages the project's content and configuration, never its audience). Two views of the
 * same table: per project (reached from the project form) and per user (the user form shows the
 * list and links here). The write endpoints take a returnTo so both views share them.
 */
@Controller
@RequestMapping("/admin/memberships")
public class ProjectMembershipController
{
    private final ProjectMembershipDAO membershipDAO;
    private final ProjectDAO projectDAO;
    private final UserDAO userDAO;
    private final UserScope userScope;

    public ProjectMembershipController(ProjectMembershipDAO membershipDAO, ProjectDAO projectDAO, UserDAO userDAO, UserScope userScope)
    {
        this.membershipDAO = membershipDAO;
        this.projectDAO = projectDAO;
        this.userDAO = userDAO;
        this.userScope = userScope;
    }

    @GetMapping("/project/{projectId}")
    public String forProject(@PathVariable Long projectId, Model model)
    {
        requireAdmin();
        Project project = projectDAO.getProjectForId(projectId);

        if (project == null) {
            return "redirect:/agentic/projects/";
        }

        List<ProjectMembership> memberships = membershipDAO.getMembershipsForProject(projectId);
        List<User> candidates = new ArrayList<>();

        for (User user : userDAO.getAllUsers()) {
            boolean member = false;

            for (ProjectMembership membership : memberships) {
                if (membership.getUser().getId().equals(user.getId())) {
                    member = true;
                }
            }

            if (!member) {
                candidates.add(user);
            }
        }

        model.addAttribute("project", project);
        model.addAttribute("memberships", memberships);
        model.addAttribute("candidates", candidates);
        model.addAttribute("levels", ProjectMembershipLevel.values());
        model.addAttribute("returnTo", "/admin/memberships/project/" + projectId);

        return "admin/projects/memberships";
    }

    @PostMapping("/set")
    public String set(@RequestParam Long userId, @RequestParam Long projectId, @RequestParam String level,
                      @RequestParam(required = false) String returnTo, RedirectAttributes redirectAttributes)
    {
        requireAdmin();
        User user = userDAO.getUser(userId);
        Project project = projectDAO.getProjectForId(projectId);
        ProjectMembershipLevel membershipLevel;

        try {
            membershipLevel = ProjectMembershipLevel.valueOf(level.toUpperCase());
        } catch (Exception e) {
            membershipLevel = null;
        }

        if (user == null || project == null || membershipLevel == null) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "User, project and level are required.");
            return "redirect:" + safeReturn(returnTo);
        }

        ProjectMembership membership = membershipDAO.getMembership(userId, projectId);

        if (membership == null) {
            membership = new ProjectMembership();
            membership.setUser(user);
            membership.setProject(project);
            membership.setCreatedAt(new Date());
        }

        membership.setLevel(membershipLevel);
        membershipDAO.saveOrUpdateMembership(membership);
        redirectAttributes.addFlashAttribute("resultMessage", user.getUsername() + " is " + membershipLevel + " in " + project.getName() + ".");

        return "redirect:" + safeReturn(returnTo);
    }

    @PostMapping("/remove")
    public String remove(@RequestParam Long userId, @RequestParam Long projectId,
                         @RequestParam(required = false) String returnTo, RedirectAttributes redirectAttributes)
    {
        requireAdmin();
        ProjectMembership membership = membershipDAO.getMembership(userId, projectId);

        if (membership != null) {
            membershipDAO.deleteMembership(membership);
            redirectAttributes.addFlashAttribute("resultMessage", membership.getUser().getUsername() + " removed from " + membership.getProject().getName() + ".");
        }

        return "redirect:" + safeReturn(returnTo);
    }

    private void requireAdmin()
    {
        if (!userScope.isAdmin(userScope.currentUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Instance administrators only");
        }
    }

    private String safeReturn(String returnTo)
    {
        return returnTo != null && returnTo.startsWith("/") && !returnTo.startsWith("//") ? returnTo : "/agentic/projects/";
    }
}
