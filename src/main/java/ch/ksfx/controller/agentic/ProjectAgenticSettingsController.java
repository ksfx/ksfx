package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.MailDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgentUnit;
import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.Project;
import ch.ksfx.services.agentic.AgenticAccessService;
import ch.ksfx.services.agentic.AgentUnitService;
import ch.ksfx.services.agentic.AgenticUsageStatsService;
import ch.ksfx.services.project.UserScope;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A project's Agentic settings page: its accesses (accounts with their defaults, enabled switch
 * and rate-limit snapshots - managed by AgenticAccessController) and its usage view. OWNER-only
 * (UserScope.requireManage - instance admins included), and the OWNER sees the key in plaintext:
 * the project's key is the project's property, and a write-only field would only obscure a value
 * an OWNER could extract through an agent's environment anyway. No inheritance - what this page
 * shows is the whole truth about what the project's agents use.
 */
@Controller
@RequestMapping("/agentic/projects/{projectId}/settings")
public class ProjectAgenticSettingsController
{
    private final ProjectDAO projectDAO;
    private final AgenticUsageStatsService usageStatsService;
    private final AgenticAccessService agenticAccessService;
    private final UserScope userScope;

    @org.springframework.beans.factory.annotation.Autowired
    private MailDAO mailDAO;

    @org.springframework.beans.factory.annotation.Autowired
    private AgentUnitService agentUnitService;

    @org.springframework.beans.factory.annotation.Autowired
    private AgentDAO agentDAO;

    public ProjectAgenticSettingsController(ProjectDAO projectDAO,
                                            AgenticUsageStatsService usageStatsService, AgenticAccessService agenticAccessService,
                                            UserScope userScope)
    {
        this.projectDAO = projectDAO;
        this.usageStatsService = usageStatsService;
        this.agenticAccessService = agenticAccessService;
        this.userScope = userScope;
    }

    @GetMapping("/")
    public String index(@PathVariable Long projectId,
                        @RequestParam(value = "statsDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statsDate,
                        Model model)
    {
        Project project = requireProject(projectId);
        model.addAttribute("project", project);
        model.addAttribute("accessCards", agenticAccessService.cardsFor(project));
        model.addAttribute("mails", mailDAO.getRecentEntries(projectId, 30)); // what the project's agents sent
        addUnits(project, model);
        usageStatsService.addUsageStats(model, statsDate != null ? statsDate : LocalDate.now(), projectId);

        return "agentic/project/agentic_project_settings";
    }

    /** The project's units (AgentUnitController) with the number of agents in each. */
    private void addUnits(Project project, Model model)
    {
        List<AgentUnit> units = agentUnitService.unitsFor(project);
        Map<Long, Integer> agentCounts = new HashMap<>();

        for (Agent agent : agentDAO.getAgentsForProject(project.getId())) {
            AgentUnit unit = AgentUnitService.effectiveUnitOf(agent);

            if (unit != null) {
                agentCounts.merge(unit.getId(), 1, Integer::sum);
            }
        }

        model.addAttribute("units", units);
        model.addAttribute("unitAgentCounts", agentCounts);
    }

    private Project requireProject(Long projectId)
    {
        Project project = projectDAO.getProjectForId(projectId);

        if (project == null) {
            throw new IllegalArgumentException("No project with id " + projectId);
        }

        userScope.requireManage(project); // the account is project configuration - OWNER only (concept, section 5)

        return project;
    }
}
