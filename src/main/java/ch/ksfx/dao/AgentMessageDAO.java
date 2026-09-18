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

    /**
     * Up to {@code limit} messages newer than {@code afterMessageId}, oldest first - the "N
     * messages after" half of a search-result context window (see
     * {@link #getMessagesForAgentBefore} for the "before" half, already used by chat pagination).
     */
    public List<AgentMessage> getMessagesForAgentAfter(Long agentId, Long afterMessageId, int limit);

    public List<AgentMessage> getAssistantMessagesWithUsage();

    /**
     * Up to {@code limit} messages (across every agent, not just one) whose content contains
     * {@code term}, most recent first. Backs the cross-agent search overlay - see
     * AgenticSearchController.
     */
    public List<AgentMessage> searchMessages(String term, int limit);
}
