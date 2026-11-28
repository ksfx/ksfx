package ch.ksfx.services.agentic;

import ch.ksfx.dao.AgentUnitDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgentUnit;
import ch.ksfx.model.Project;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Units (see {@link AgentUnit}): the sidebar grouping of a project's agents and the small
 * bookkeeping behind it - find-or-create by name from the agent form, new units at the end,
 * moving a unit up/down.
 */
@Service
public class AgentUnitService
{
    private final AgentUnitDAO agentUnitDAO;

    public AgentUnitService(AgentUnitDAO agentUnitDAO)
    {
        this.agentUnitDAO = agentUnitDAO;
    }

    public List<AgentUnit> unitsFor(Project project)
    {
        return project != null ? agentUnitDAO.getUnitsForProject(project.getId()) : Collections.emptyList();
    }

    /** The agent's unit if it belongs to the agent's own project, else null. */
    public static AgentUnit effectiveUnitOf(Agent agent)
    {
        AgentUnit unit = agent.getAgentUnit();

        return unit != null && agent.getProject() != null && unit.getProject() != null
                && unit.getProject().getId().equals(agent.getProject().getId()) ? unit : null;
    }

    /** The project's unit of that name (case-insensitive), created at the end if there is none. */
    public AgentUnit findOrCreate(Project project, String name)
    {
        String trimmed = name.trim();
        List<AgentUnit> units = unitsFor(project);

        for (AgentUnit unit : units) {
            if (unit.getName().equalsIgnoreCase(trimmed)) {
                return unit;
            }
        }

        AgentUnit unit = new AgentUnit();
        unit.setProject(project);
        unit.setName(trimmed);
        unit.setSortOrder(units.isEmpty() ? 0 : units.get(units.size() - 1).getSortOrder() + 1);
        unit.setCreatedAt(new Date());
        agentUnitDAO.saveOrUpdateAgentUnit(unit);

        return unit;
    }

    /** Swaps the unit with its neighbour above (up) or below; renumbers the project's units 0..n on the way. */
    public void move(AgentUnit unit, boolean up)
    {
        List<AgentUnit> units = new ArrayList<>(unitsFor(unit.getProject()));
        int index = -1;

        for (int i = 0; i < units.size(); i++) {
            if (units.get(i).getId().equals(unit.getId())) {
                index = i;
            }
        }

        int other = up ? index - 1 : index + 1;

        if (index < 0 || other < 0 || other >= units.size()) {
            return;
        }

        Collections.swap(units, index, other);

        for (int i = 0; i < units.size(); i++) {
            if (units.get(i).getSortOrder() != i) {
                units.get(i).setSortOrder(i);
                agentUnitDAO.saveOrUpdateAgentUnit(units.get(i));
            }
        }
    }

    /**
     * The sidebar: agents without a unit (listed on top, as before units existed), then one group
     * per unit in unit order - only units that have agents.
     */
    public Sidebar sidebarFor(List<Agent> agents)
    {
        Sidebar sidebar = new Sidebar();
        Map<Long, Group> groups = new LinkedHashMap<>();

        for (Agent agent : agents) {
            AgentUnit unit = effectiveUnitOf(agent);

            if (unit == null) {
                sidebar.ungrouped.add(agent);
            } else {
                groups.computeIfAbsent(unit.getId(), id -> new Group(unit)).agents.add(agent);
            }
        }

        sidebar.groups.addAll(groups.values());
        sidebar.groups.sort((a, b) -> a.unit.getSortOrder() != b.unit.getSortOrder()
                ? Integer.compare(a.unit.getSortOrder(), b.unit.getSortOrder())
                : a.unit.getName().compareToIgnoreCase(b.unit.getName()));

        return sidebar;
    }

    public static class Sidebar
    {
        private final List<Agent> ungrouped = new ArrayList<>();
        private final List<Group> groups = new ArrayList<>();

        public List<Agent> getUngrouped() { return ungrouped; }
        public List<Group> getGroups() { return groups; }
    }

    public static class Group
    {
        private final AgentUnit unit;
        private final List<Agent> agents = new ArrayList<>();

        Group(AgentUnit unit)
        {
            this.unit = unit;
        }

        public AgentUnit getUnit() { return unit; }
        public List<Agent> getAgents() { return agents; }
    }
}
