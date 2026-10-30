package ch.ksfx.services.issues;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.ProjectMembershipDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.Project;
import ch.ksfx.model.ProjectMembership;
import ch.ksfx.model.user.User;
import ch.ksfx.services.project.AgentScope;
import ch.ksfx.services.project.UserScope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Who an issue of a given project may be assigned to - one rule for the issue form and the agent
 * API (KSFX issues #15, #16):
 * <ul>
 *     <li><b>People:</b> the project's members. Nobody outside it - a viewer only ever sees people
 *     they actually share this project with.</li>
 *     <li><b>Agents:</b> enabled agents that can work on the project (AgentScope: its own agents,
 *     plus agents of admin projects) - and, for a human viewer, only those whose own project the
 *     viewer may read, so the list never reveals agents of projects the viewer cannot see.</li>
 * </ul>
 * Both lists are sorted case-insensitively by name. Saving re-checks the submitted value against
 * the same rule, so a crafted request cannot assign outside it; an issue's CURRENT assignee stays
 * accepted (and listed) even if no longer eligible, so editing an old issue never silently drops it.
 */
@Service
public class IssueAssigneeService
{
    private final ProjectMembershipDAO membershipDAO;
    private final AgentDAO agentDAO;
    private final UserScope userScope;

    public IssueAssigneeService(ProjectMembershipDAO membershipDAO, AgentDAO agentDAO, UserScope userScope)
    {
        this.membershipDAO = membershipDAO;
        this.agentDAO = agentDAO;
        this.userScope = userScope;
    }

    /** Members of the project, sorted by username. */
    public List<User> userCandidates(Project project)
    {
        Map<Long, User> users = new LinkedHashMap<>();

        for (ProjectMembership membership : membershipDAO.getMembershipsForProject(project.getId())) {
            users.put(membership.getUser().getId(), membership.getUser());
        }

        List<User> sorted = new ArrayList<>(users.values());
        sorted.sort(Comparator.comparing(u -> u.getUsername().toLowerCase()));

        return sorted;
    }

    /** Agents that can work on the project; viewer = the human looking at the form (null for agent callers - AgentScope already limits those). */
    public List<Agent> agentCandidates(Project project, User viewer)
    {
        List<Agent> agents = new ArrayList<>();

        for (Agent agent : agentDAO.getAllAgents()) {
            if (!agent.getEnabled() || !AgentScope.canAccess(agent, project)) {
                continue;
            }

            if (viewer != null && !userScope.canRead(viewer, agent.getProject())) {
                continue;
            }

            agents.add(agent);
        }

        agents.sort(Comparator.comparing(a -> a.getName().toLowerCase()));

        return agents;
    }

    /** May "user:{id}" / "agent:{id}" be assigned to an issue of this project? */
    public boolean isAllowed(Project project, String assignee, User viewer)
    {
        if (assignee == null || assignee.isEmpty()) {
            return true;
        }

        try {
            if (assignee.startsWith("user:")) {
                Long id = Long.valueOf(assignee.substring("user:".length()));
                return userCandidates(project).stream().anyMatch(u -> u.getId().equals(id));
            }

            if (assignee.startsWith("agent:")) {
                Long id = Long.valueOf(assignee.substring("agent:".length()));
                return agentCandidates(project, viewer).stream().anyMatch(a -> a.getId().equals(id));
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return false;
    }
}
