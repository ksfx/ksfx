package ch.ksfx.dao;

import ch.ksfx.model.AgentMessage;

import java.util.List;

public interface AgentMessageDAO
{
    public void saveAgentMessage(AgentMessage agentMessage);
    public void deleteAgentMessage(AgentMessage agentMessage);
    public AgentMessage getAgentMessageForId(Long agentMessageId);
    public List<AgentMessage> getMessagesForAgent(Long agentId);
    public List<AgentMessage> getAssistantMessagesWithUsage();
}
