package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgenticAccessDAO;
import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgenticAccess;
import ch.ksfx.services.agentic.AgenticAccessService;
import ch.ksfx.model.AgenticAuthMode;
import ch.ksfx.model.Project;
import ch.ksfx.services.project.UserScope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A project's accesses (accounts, see {@link AgenticAccess}) with their defaults (model,
 * permission mode) and enabled switch: add, edit, delete. OWNER-only like the rest of the
 * project's Agentic settings; the OWNER sees the key in plaintext on the edit form (the cards only
 * show it masked). There is no default access - every agent names its own - so an access that
 * agents still run on cannot be deleted.
 */
@Controller
@RequestMapping("/agentic/projects/{projectId}/accesses")
public class AgenticAccessController
{
    private final ProjectDAO projectDAO;
    private final AgenticAccessDAO agenticAccessDAO;
    private final UserScope userScope;
    private final AgenticAccessService agenticAccessService;

    public AgenticAccessController(ProjectDAO projectDAO, AgenticAccessDAO agenticAccessDAO, UserScope userScope, AgenticAccessService agenticAccessService)
    {
        this.projectDAO = projectDAO;
        this.agenticAccessDAO = agenticAccessDAO;
        this.userScope = userScope;
        this.agenticAccessService = agenticAccessService;
    }

    @GetMapping("/new")
    public String create(@PathVariable Long projectId, Model model)
    {
        Project project = requireProject(projectId);
        AgenticAccess access = new AgenticAccess();
        access.setProject(project);

        model.addAttribute("project", project);
        model.addAttribute("access", access);

        return "agentic/project/agentic_access_edit";
    }

    @GetMapping("/{accessId}/edit")
    public String edit(@PathVariable Long projectId, @PathVariable Long accessId, Model model)
    {
        Project project = requireProject(projectId);

        model.addAttribute("project", project);
        model.addAttribute("access", requireAccess(project, accessId));

        return "agentic/project/agentic_access_edit";
    }

    @PostMapping("/save")
    public String save(@PathVariable Long projectId,
                       @RequestParam(value = "id", required = false) Long accessId,
                       @RequestParam("name") String name,
                       @RequestParam("authMode") AgenticAuthMode authMode,
                       @RequestParam(value = "apiKey", required = false) String apiKey,
                       @RequestParam(value = "defaultModel", required = false) String defaultModel,
                       @RequestParam(value = "defaultPermissionMode", required = false) String defaultPermissionMode,
                       @RequestParam(value = "enabled", defaultValue = "false") boolean enabled,
                       Model model, RedirectAttributes redirectAttributes)
    {
        Project project = requireProject(projectId);
        AgenticAccess access = accessId != null ? requireAccess(project, accessId) : new AgenticAccess();

        access.setName(name != null ? name.trim() : "");
        access.setAuthMode(authMode);
        access.setApiKey(authMode == AgenticAuthMode.OAUTH || apiKey == null || apiKey.trim().isEmpty() ? null : apiKey.trim());
        access.setDefaultModel(defaultModel == null || defaultModel.trim().isEmpty() ? null : defaultModel.trim());
        access.setDefaultPermissionMode(defaultPermissionMode == null || defaultPermissionMode.trim().isEmpty() ? "default" : defaultPermissionMode.trim());
        access.setEnabled(enabled);

        if (access.getName().isEmpty()) {
            model.addAttribute("project", project);
            model.addAttribute("access", access);
            model.addAttribute("nameError", "Please give the access a name, e.g. \"Company API key\".");

            return "agentic/project/agentic_access_edit";
        }

        if (access.getId() == null) {
            access.setProject(project);
            access.setCreatedAt(new Date());
        }

        agenticAccessDAO.saveOrUpdateAgenticAccess(access);
        redirectAttributes.addFlashAttribute("message", "Access '" + access.getName() + "' saved.");

        return redirectToSettings(projectId);
    }

    @PostMapping("/{accessId}/delete")
    public String delete(@PathVariable Long projectId, @PathVariable Long accessId, RedirectAttributes redirectAttributes)
    {
        Project project = requireProject(projectId);
        AgenticAccess access = requireAccess(project, accessId);

        // No agent may end up without an access: switch them over first.
        List<Agent> users = agenticAccessService.agentsUsing(access);

        if (!users.isEmpty()) {
            redirectAttributes.addFlashAttribute("accessError", "'" + access.getName() + "' is still used by "
                    + users.stream().map(Agent::getName).collect(Collectors.joining(", "))
                    + " - switch them to another access first.");

            return redirectToSettings(projectId);
        }

        agenticAccessDAO.deleteAgenticAccess(access);
        redirectAttributes.addFlashAttribute("message", "Access '" + access.getName() + "' deleted.");

        return redirectToSettings(projectId);
    }

    private String redirectToSettings(Long projectId)
    {
        return "redirect:/agentic/projects/" + projectId + "/settings/";
    }

    private Project requireProject(Long projectId)
    {
        Project project = projectDAO.getProjectForId(projectId);

        if (project == null) {
            throw new IllegalArgumentException("No project with id " + projectId);
        }

        userScope.requireManage(project); // accesses are project configuration - OWNER only

        return project;
    }

    private AgenticAccess requireAccess(Project project, Long accessId)
    {
        AgenticAccess access = agenticAccessDAO.getAgenticAccessForId(accessId);

        if (access == null || access.getProject() == null || !access.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("No access with id " + accessId + " in project " + project.getId());
        }

        return access;
    }
}
