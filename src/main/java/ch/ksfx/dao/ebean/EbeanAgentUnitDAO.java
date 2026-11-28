package ch.ksfx.dao.ebean;

import ch.ksfx.dao.AgentUnitDAO;
import ch.ksfx.model.AgentUnit;
import io.ebean.Ebean;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EbeanAgentUnitDAO implements AgentUnitDAO
{
    @Override
    public AgentUnit getAgentUnitForId(Long id)
    {
        return id != null ? Ebean.find(AgentUnit.class, id) : null;
    }

    @Override
    public List<AgentUnit> getUnitsForProject(Long projectId)
    {
        return Ebean.find(AgentUnit.class).where().eq("project.id", projectId)
                .orderBy("sortOrder asc, name asc")
                .findList();
    }

    @Override
    public void saveOrUpdateAgentUnit(AgentUnit unit)
    {
        if (unit.getId() != null) {
            Ebean.update(unit);
        } else {
            Ebean.save(unit);
        }
    }

    @Override
    public void deleteAgentUnit(AgentUnit unit)
    {
        // agent.agent_unit_id is ON DELETE SET NULL: the unit's agents just lose their unit.
        Ebean.delete(unit);
    }
}
