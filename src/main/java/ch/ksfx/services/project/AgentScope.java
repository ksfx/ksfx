package ch.ksfx.services.project;

import ch.ksfx.model.Agent;
import ch.ksfx.model.Project;

/**
 * The one rule for what an agent may touch (concept, section 9 Q6): its own project - or
 * everything, if its project is an admin project. Used by every agent API endpoint that
 * resolves a wiki, tracker or target agent, and by the system prompt when it lists them, so the
 * agent never sees ids it may not use. Humans are not covered here (no human permissions yet).
 */
public final class AgentScope
{
    private AgentScope()
    {
    }

    public static boolean canAccess(Agent agent, Project project)
    {
        if (agent == null || agent.getProject() == null) {
            return false;
        }

        if (agent.getProject().getAdminProject()) {
            return true;
        }

        return project != null && project.getId() != null && project.getId().equals(agent.getProject().getId());
    }
}
