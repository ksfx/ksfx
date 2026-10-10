package ch.ksfx.controller.admin.gitsync;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The instance-wide "KSFX - GIT Sync" admin page became per-project with the project migration
 * (2026-10-10) and lives at /agentic/projects/{id}/git/ now; bookmarks of the old URL land on the
 * project list, from where each project's Git page is one click away.
 */
@Controller
public class LegacyGitSyncRedirectController
{
    @GetMapping({"/admin/gitsync", "/admin/gitsync/"})
    public String redirectToProjects()
    {
        return "redirect:/agentic/projects/";
    }
}
