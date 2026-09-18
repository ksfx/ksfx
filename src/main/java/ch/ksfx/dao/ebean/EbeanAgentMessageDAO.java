package ch.ksfx.dao.ebean;

import ch.ksfx.dao.AgentMessageDAO;
import ch.ksfx.model.AgentMessage;
import ch.ksfx.model.AgentMessageRole;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class EbeanAgentMessageDAO implements AgentMessageDAO
{
    @Override
    public void saveAgentMessage(AgentMessage agentMessage)
    {
        Ebean.save(agentMessage);
    }

    @Override
    public void deleteAgentMessage(AgentMessage agentMessage)
    {
        Ebean.delete(agentMessage);
    }

    @Override
    public AgentMessage getAgentMessageForId(Long agentMessageId)
    {
        return Ebean.find(AgentMessage.class, agentMessageId);
    }

    @Override
    public List<AgentMessage> getRecentMessagesForAgent(Long agentId, int limit)
    {
        return oldestFirst(Ebean.find(AgentMessage.class).fetch("fromAgent")
                .where().eq("agent.id", agentId).eq("internal", false)
                .order().desc("id")
                .setMaxRows(limit)
                .findList());
    }

    @Override
    public List<AgentMessage> getMessagesForAgentBefore(Long agentId, Long beforeMessageId, int limit)
    {
        return oldestFirst(Ebean.find(AgentMessage.class).fetch("fromAgent")
                .where().eq("agent.id", agentId).eq("internal", false).lt("id", beforeMessageId)
                .order().desc("id")
                .setMaxRows(limit)
                .findList());
    }

    @Override
    public AgentMessage getLastMessageForAgent(Long agentId)
    {
        return Ebean.find(AgentMessage.class)
                .where().eq("agent.id", agentId).eq("internal", false)
                .order().desc("id")
                .setMaxRows(1)
                .findOneOrEmpty()
                .orElse(null);
    }

    /**
     * Both paged queries above fetch newest-first (so `LIMIT` keeps the *most recent* rows, not
     * the oldest overall) but the chat UI renders top-to-bottom oldest-first - flip in Java rather
     * than adding a second DB round-trip or a window-function query just to get the ordering back.
     */
    private List<AgentMessage> oldestFirst(List<AgentMessage> newestFirst)
    {
        Collections.reverse(newestFirst);
        return newestFirst;
    }

    @Override
    public List<AgentMessage> getMessagesForAgentAfter(Long agentId, Long afterMessageId, int limit)
    {
        // Already oldest-first by construction (ascending id) - no reversal needed, unlike the
        // "before"/"recent" queries above.
        return Ebean.find(AgentMessage.class).fetch("fromAgent")
                .where().eq("agent.id", agentId).eq("internal", false).gt("id", afterMessageId)
                .order().asc("id")
                .setMaxRows(limit)
                .findList();
    }

    @Override
    public List<AgentMessage> getAssistantMessagesWithUsage()
    {
        return Ebean.find(AgentMessage.class).where().eq("role", AgentMessageRole.ASSISTANT).isNotNull("inputTokens").order().desc("createdAt").findList();
    }

    @Override
    public List<AgentMessage> searchMessages(String term, int limit)
    {
        return Ebean.find(AgentMessage.class).fetch("agent").fetch("fromAgent")
                .where().eq("internal", false).icontains("content", term)
                .order().desc("id")
                .setMaxRows(limit)
                .findList();
    }
}
