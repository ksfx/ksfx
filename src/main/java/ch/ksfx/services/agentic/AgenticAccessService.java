package ch.ksfx.services.agentic;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.AgenticAccessDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgenticAccess;
import ch.ksfx.model.Project;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Which access (account) an agent runs on - one rule for the turn itself, the voice cleanup, the
 * chat header and the forms: the agent's own access if it belongs to the agent's project, else the
 * project's default access; null if the project has none.
 */
@Service("agenticAccessService")
public class AgenticAccessService
{
    private final AgenticAccessDAO agenticAccessDAO;
    private final AgentDAO agentDAO;
    private final AgenticUsageStatsService usageStatsService;

    public AgenticAccessService(AgenticAccessDAO agenticAccessDAO, AgentDAO agentDAO, AgenticUsageStatsService usageStatsService)
    {
        this.agenticAccessDAO = agenticAccessDAO;
        this.agentDAO = agentDAO;
        this.usageStatsService = usageStatsService;
    }

    public AgenticAccess effectiveAccessFor(Agent agent)
    {
        if (agent == null || agent.getProject() == null) {
            return null;
        }

        AgenticAccess own = agent.getAgenticAccess();

        if (own != null && own.getProject() != null && own.getProject().getId().equals(agent.getProject().getId())) {
            return own;
        }

        return agenticAccessDAO.getDefaultAccessForProject(agent.getProject().getId());
    }

    public List<AgenticAccess> accessesFor(Project project)
    {
        return project != null && project.getId() != null ? agenticAccessDAO.getAccessesForProject(project.getId()) : new ArrayList<>();
    }

    /** The agents that currently run on {@code access} - incl. those that use it as the project default. */
    public List<Agent> agentsUsing(AgenticAccess access)
    {
        List<Agent> agents = new ArrayList<>();
        Long projectId = access.getProject().getId();

        for (Agent agent : agentDAO.getAgentsForProject(projectId)) {
            AgenticAccess own = agent.getAgenticAccess();
            boolean ownValid = own != null && own.getProject() != null && own.getProject().getId().equals(projectId);

            if (ownValid ? own.getId().equals(access.getId()) : access.getDefaultAccess()) {
                agents.add(agent);
            }
        }

        return agents;
    }

    /** One card per access for the project's Agentic Settings page: the access, its status, usage and agents. */
    public List<AccessCard> cardsFor(Project project)
    {
        List<AccessCard> cards = new ArrayList<>();
        java.util.Map<Long, AgenticUsageStatsService.AccessUsage> usage = usageStatsService.usageByAccess(project.getId());

        for (AgenticAccess access : accessesFor(project)) {
            AccessCard card = new AccessCard();
            card.access = access;
            card.agents = agentsUsing(access);
            card.usage = usage.getOrDefault(access.getId(), new AgenticUsageStatsService.AccessUsage());
            cards.add(card);
        }

        return cards;
    }

    /** Short status text for a pill or a dropdown option: "OK", "Warning", "Blocked until Mon 14:00", "no status yet". */
    public static String statusLabel(AgenticAccess access)
    {
        switch (access.getStatusKey()) {
            case "ok":
                return "OK";
            case "warning":
                return "Warning";
            case "blocked":
                return access.getClaudeRateLimitResetsAt() != null
                        ? "Blocked until " + new java.text.SimpleDateFormat("EEE HH:mm", java.util.Locale.ENGLISH).format(access.getClaudeRateLimitResetsAt())
                        : "Blocked";
            default:
                return "no status yet";
        }
    }

    public static class AccessCard
    {
        private AgenticAccess access;
        private List<Agent> agents;
        private AgenticUsageStatsService.AccessUsage usage;

        public AgenticAccess getAccess() { return access; }
        public List<Agent> getAgents() { return agents; }
        public AgenticUsageStatsService.AccessUsage getUsage() { return usage; }
        public String getStatusLabel() { return statusLabel(access); }

        /** "5 h limit", "7 day limit" - the CLI's rateLimitType, readable; null if unknown. */
        public String getLimitLabel()
        {
            String type = access.getClaudeRateLimitType();

            if (type == null) {
                return null;
            }

            if (type.contains("five_hour")) {
                return "5 h limit";
            }

            if (type.contains("seven_day")) {
                return "7 day limit";
            }

            return type.replace('_', ' ');
        }

        /** Age of the rate-limit snapshot: "12 min ago", "3 h ago", "2 d ago"; null if there is none. */
        public String getSnapshotAge()
        {
            if (access.getClaudeRateLimitUpdatedAt() == null) {
                return null;
            }

            long minutes = (System.currentTimeMillis() - access.getClaudeRateLimitUpdatedAt().getTime()) / 60000;

            if (minutes < 1) {
                return "just now";
            }

            if (minutes < 60) {
                return minutes + " min ago";
            }

            return minutes < 48 * 60 ? (minutes / 60) + " h ago" : (minutes / (24 * 60)) + " d ago";
        }
    }
}
