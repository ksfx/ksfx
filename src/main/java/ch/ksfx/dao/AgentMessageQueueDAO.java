package ch.ksfx.dao;

import ch.ksfx.model.AgentMessageQueueEntry;

import java.util.List;

public interface AgentMessageQueueDAO
{
    /** Oldest first. */
    public List<AgentMessageQueueEntry> getQueueForAgent(Long agentId);

    public AgentMessageQueueEntry getEntryForId(Long id);

    public void saveEntry(AgentMessageQueueEntry entry);

    public void deleteEntry(AgentMessageQueueEntry entry);
}
