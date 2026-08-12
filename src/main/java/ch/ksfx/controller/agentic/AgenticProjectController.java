package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.AgenticConfigDAO;
import ch.ksfx.dao.AgenticProjectDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.model.AgenticProject;
import ch.ksfx.services.agentic.AgentWorkspaceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;

@Controller
@RequestMapping("/agentic/projects")
public class AgenticProjectController
{
    private final AgenticProjectDAO agenticProjectDAO;
    private final AgentDAO agentDAO;
    private final AgenticConfigDAO agenticConfigDAO;
    private final AgentWorkspaceService agentWorkspaceService;

    public AgenticProjectController(AgenticProjectDAO agenticProjectDAO,
                                     AgentDAO agentDAO,
                                     AgenticConfigDAO agenticConfigDAO,
                                     AgentWorkspaceService agentWorkspaceService)
    {
        this.agenticProjectDAO = agenticProjectDAO;
        this.agentDAO = agentDAO;
        this.agenticConfigDAO = agenticConfigDAO;
        this.agentWorkspaceService = agentWorkspaceService;
    }

    @GetMapping("/")
    public String index(Pageable pageable, Model model)
    {
        Page<AgenticProject> agenticProjectsPage = agenticProjectDAO.getAgenticProjectsForPageable(pageable);

        model.addAttribute("agenticProjectsPage", agenticProjectsPage);

        return "agentic/project/agentic_project";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    public String edit(@PathVariable(value = "id", required = false) Long id, Model model)
    {
        AgenticProject agenticProject = id != null ? agenticProjectDAO.getAgenticProjectForId(id) : new AgenticProject();

        model.addAttribute("agenticProject", agenticProject);

        return "agentic/project/agentic_project_edit";
    }

    @PostMapping({"/edit", "/edit/{id}"})
    public String submit(@PathVariable(value = "id", required = false) Long id, @Valid @ModelAttribute AgenticProject agenticProject, BindingResult bindingResult, Model model)
    {
        if (bindingResult.hasErrors()) {
            return "agentic/project/agentic_project_edit";
        }

        if (agenticProject.getId() == null) {
            agenticProject.setCreatedAt(new Date());
        }

        agenticProjectDAO.saveOrUpdateAgenticProject(agenticProject);

        return "redirect:/agentic/projects/edit/" + agenticProject.getId();
    }

    /**
     * Unassigns and physically relocates each currently-assigned agent's workspace back to the
     * flat, ungrouped layout before removing the project row, so nothing is orphaned or blocked -
     * deleting a project just leaves its former agents ungrouped. The DB's own ON DELETE SET NULL
     * is only a backstop; this loop is the real mechanism.
     */
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable(value = "id") Long id, RedirectAttributes redirectAttributes) throws IOException
    {
        AgenticProject agenticProject = agenticProjectDAO.getAgenticProjectForId(id);
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        for (Agent agent : agentDAO.getAgentsForAgenticProject(id)) {
            if (config != null) {
                Path oldWorkspace = agentWorkspaceService.resolveWorkspace(agent, config);
                agent.setAgenticProject(null);
                Path newWorkspace = agentWorkspaceService.resolveWorkspace(agent, config);

                agentWorkspaceService.moveWorkspaceIfNeeded(oldWorkspace, newWorkspace);
                agent.setWorkspacePath(newWorkspace.toString());
            } else {
                agent.setAgenticProject(null);
            }

            agentDAO.saveOrUpdateAgent(agent);
        }

        agenticProjectDAO.deleteAgenticProject(agenticProject);

        redirectAttributes.addFlashAttribute("resultMessage", "Agentic Project deleted.");

        return "redirect:/agentic/projects/";
    }
}
