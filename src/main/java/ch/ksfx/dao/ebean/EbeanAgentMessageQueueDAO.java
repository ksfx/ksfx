package ch.ksfx.dao.ebean;

import ch.ksfx.dao.AgentMessageQueueDAO;
import ch.ksfx.model.AgentMessageQueueEntry;
import io.ebean.Ebean;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EbeanAgentMessageQueueDAO implements AgentMessageQueueDAO
{
    @Override
    public List<AgentMessageQueueEntry> getQueueForAgent(Long agentId)
    {
        return Ebean.find(AgentMessageQueueEntry.class).where().eq("agent.id", agentId).order().asc("id").findList();
    }

    @Override
    public AgentMessageQueueEntry getEntryForId(Long id)
    {
        return Ebean.find(AgentMessageQueueEntry.class, id);
    }

    @Override
    public void saveEntry(AgentMessageQueueEntry entry)
    {
        Ebean.save(entry);
    }

    @Override
    public void deleteEntry(AgentMessageQueueEntry entry)
    {
        Ebean.delete(entry);
    }
}
