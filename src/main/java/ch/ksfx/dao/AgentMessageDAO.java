package ch.ksfx.dao;

import ch.ksfx.model.AgentMessage;

import java.util.List;

public interface AgentMessageDAO
{
    public void saveAgentMessage(AgentMessage agentMessage);
    public List<AgentMessage> getMessagesForAgent(Long agentId);
    public List<AgentMessage> getAssistantMessagesWithUsage();
}
