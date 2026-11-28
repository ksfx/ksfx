package ch.ksfx.dao;

import ch.ksfx.model.AgentUnit;

import java.util.List;

public interface AgentUnitDAO
{
    public AgentUnit getAgentUnitForId(Long id);

    /** A project's units in sidebar order. */
    public List<AgentUnit> getUnitsForProject(Long projectId);

    public void saveOrUpdateAgentUnit(AgentUnit unit);

    public void deleteAgentUnit(AgentUnit unit);
}
