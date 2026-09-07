package ch.ksfx.dao;

import ch.ksfx.model.AgentMessage;

import java.util.List;

public interface AgentMessageDAO
{
    public void saveAgentMessage(AgentMessage agentMessage);
    public void deleteAgentMessage(AgentMessage agentMessage);
    public AgentMessage getAgentMessageForId(Long agentMessageId);

    /** The most recent {@code limit} messages, oldest first (ready to render top-to-bottom). */
    public List<AgentMessage> getRecentMessagesForAgent(Long agentId, int limit);

    /**
     * Up to {@code limit} messages older than {@code beforeMessageId}, oldest first - the "load
     * older" page one screenful before whatever's currently the oldest loaded message.
     */
    public List<AgentMessage> getMessagesForAgentBefore(Long agentId, Long beforeMessageId, int limit);

    /** Just the single most recent message, or null - avoids loading the whole history for it. */
    public AgentMessage getLastMessageForAgent(Long agentId);

    public List<AgentMessage> getAssistantMessagesWithUsage();
}
