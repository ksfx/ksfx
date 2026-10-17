package ch.ksfx.controller;

import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.Project;
import ch.ksfx.services.project.ProjectContextService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

/**
 * Target of the header's project switcher (layout.html): sets the session's current project and
 * returns to the page the user was on - or, if that page belongs to a specific entity of the old
 * project (a wiki page, an agent chat...), to that area's root, which then resolves inside the new
 * project. Only same-site paths are accepted as return targets.
 */
@Controller
public class ProjectSwitchController
{
    private final ProjectDAO projectDAO;
    private final ProjectContextService projectContextService;

    public ProjectSwitchController(ProjectDAO projectDAO, ProjectContextService projectContextService)
    {
        this.projectDAO = projectDAO;
        this.projectContextService = projectContextService;
    }

    @GetMapping("/projects/switch/{id}")
    public String switchProject(@PathVariable Long id, @RequestParam(required = false) String returnTo, HttpSession session)
    {
        Project project = projectDAO.getProjectForId(id);

        if (project != null) {
            projectContextService.set(session, project);
        }

        return "redirect:" + areaRoot(returnTo);
    }

    /**
     * Entity-specific paths would re-follow the OLD project (deep links win, see
     * ProjectContextService#follow), so they collapse to their area's root; everything else
     * returns as-is.
     */
    static String areaRoot(String returnTo)
    {
        if (returnTo == null || !returnTo.startsWith("/") || returnTo.startsWith("//")) {
            return "/dashboard/";
        }

        for (String area : new String[] {"/wiki/", "/issues/", "/agentic/chat/", "/agentic/edit/", "/activity/activityedit/", "/activity/activityinstances/", "/publishing/publishingconfigurationedit/", "/publishing/publishingresourceedit/"}) {
            if (returnTo.startsWith(area) && returnTo.length() > area.length()) {
                return area.startsWith("/agentic") ? "/agentic/" : area.startsWith("/activity") ? "/activity/" : area.startsWith("/publishing") ? "/publishing/" : area;
            }
        }

        return returnTo;
    }
}
