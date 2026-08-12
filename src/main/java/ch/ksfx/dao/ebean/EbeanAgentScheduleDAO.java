package ch.ksfx.dao.ebean;

import ch.ksfx.dao.AgentScheduleDAO;
import ch.ksfx.model.AgentSchedule;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanAgentScheduleDAO implements AgentScheduleDAO
{
    @Override
    public void saveOrUpdateAgentSchedule(AgentSchedule agentSchedule)
    {
        if (agentSchedule.getId() != null) {
            Ebean.update(agentSchedule);
        } else {
            Ebean.save(agentSchedule);
        }
    }

    @Override
    public void deleteAgentSchedule(AgentSchedule agentSchedule)
    {
        Ebean.delete(agentSchedule);
    }

    @Override
    public AgentSchedule getAgentScheduleForId(Long agentScheduleId)
    {
        return Ebean.find(AgentSchedule.class, agentScheduleId);
    }

    @Override
    public List<AgentSchedule> getSchedulesForAgent(Long agentId)
    {
        return Ebean.find(AgentSchedule.class).where().eq("agent.id", agentId).order().asc("name").findList();
    }

    @Override
    public List<AgentSchedule> getAllEnabledSchedules()
    {
        return Ebean.find(AgentSchedule.class).where().eq("cronScheduleEnabled", true).findList();
    }
}
