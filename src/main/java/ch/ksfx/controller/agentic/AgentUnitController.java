package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentUnitDAO;
import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.AgentUnit;
import ch.ksfx.model.Project;
import ch.ksfx.services.agentic.AgentUnitService;
import ch.ksfx.services.project.UserScope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * A project's units (see {@link AgentUnit}) on its Agentic Settings page: add, rename, move up/down,
 * delete. OWNER-only like agents and accesses. Deleting a unit leaves its agents without a unit.
 */
@Controller
@RequestMapping("/agentic/projects/{projectId}/units")
public class AgentUnitController
{
    private final ProjectDAO projectDAO;
    private final AgentUnitDAO agentUnitDAO;
    private final AgentUnitService agentUnitService;
    private final UserScope userScope;

    public AgentUnitController(ProjectDAO projectDAO, AgentUnitDAO agentUnitDAO, AgentUnitService agentUnitService, UserScope userScope)
    {
        this.projectDAO = projectDAO;
        this.agentUnitDAO = agentUnitDAO;
        this.agentUnitService = agentUnitService;
        this.userScope = userScope;
    }

    @PostMapping("/add")
    public String add(@PathVariable Long projectId, @RequestParam("name") String name, RedirectAttributes redirectAttributes)
    {
        Project project = requireProject(projectId);

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("unitError", "Please give the unit a name.");
        } else {
            AgentUnit unit = agentUnitService.findOrCreate(project, name);
            redirectAttributes.addFlashAttribute("message", "Unit '" + unit.getName() + "' saved.");
        }

        return redirectToSettings(projectId);
    }

    @PostMapping("/{unitId}/rename")
    public String rename(@PathVariable Long projectId, @PathVariable Long unitId, @RequestParam("name") String name, RedirectAttributes redirectAttributes)
    {
        AgentUnit unit = requireUnit(requireProject(projectId), unitId);

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("unitError", "A unit needs a name.");
        } else {
            unit.setName(name.trim());
            agentUnitDAO.saveOrUpdateAgentUnit(unit);
            redirectAttributes.addFlashAttribute("message", "Unit renamed to '" + unit.getName() + "'.");
        }

        return redirectToSettings(projectId);
    }

    @PostMapping("/{unitId}/move")
    public String move(@PathVariable Long projectId, @PathVariable Long unitId, @RequestParam("direction") String direction)
    {
        agentUnitService.move(requireUnit(requireProject(projectId), unitId), "up".equals(direction));

        return redirectToSettings(projectId);
    }

    @PostMapping("/{unitId}/delete")
    public String delete(@PathVariable Long projectId, @PathVariable Long unitId, RedirectAttributes redirectAttributes)
    {
        AgentUnit unit = requireUnit(requireProject(projectId), unitId);
        agentUnitDAO.deleteAgentUnit(unit);
        redirectAttributes.addFlashAttribute("message", "Unit '" + unit.getName() + "' deleted - its agents are now listed without a unit.");

        return redirectToSettings(projectId);
    }

    private String redirectToSettings(Long projectId)
    {
        return "redirect:/agentic/projects/" + projectId + "/settings/#units";
    }

    private Project requireProject(Long projectId)
    {
        Project project = projectDAO.getProjectForId(projectId);

        if (project == null) {
            throw new IllegalArgumentException("No project with id " + projectId);
        }

        userScope.requireManage(project); // units are project configuration - OWNER only

        return project;
    }

    private AgentUnit requireUnit(Project project, Long unitId)
    {
        AgentUnit unit = agentUnitDAO.getAgentUnitForId(unitId);

        if (unit == null || unit.getProject() == null || !unit.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("No unit with id " + unitId + " in project " + project.getId());
        }

        return unit;
    }
}
