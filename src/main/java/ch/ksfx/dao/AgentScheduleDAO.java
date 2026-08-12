package ch.ksfx.dao;

import ch.ksfx.model.AgentSchedule;

import java.util.List;

public interface AgentScheduleDAO
{
    public void saveOrUpdateAgentSchedule(AgentSchedule agentSchedule);
    public void deleteAgentSchedule(AgentSchedule agentSchedule);
    public AgentSchedule getAgentScheduleForId(Long agentScheduleId);
    public List<AgentSchedule> getSchedulesForAgent(Long agentId);
    public List<AgentSchedule> getAllEnabledSchedules();
}
