package ch.ksfx.dao.ebean;

import ch.ksfx.dao.AgentMessageDAO;
import ch.ksfx.model.AgentMessage;
import ch.ksfx.model.AgentMessageRole;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

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
    public List<AgentMessage> getMessagesForAgent(Long agentId)
    {
        return Ebean.find(AgentMessage.class).where().eq("agent.id", agentId).order().asc("id").findList();
    }

    @Override
    public List<AgentMessage> getAssistantMessagesWithUsage()
    {
        return Ebean.find(AgentMessage.class).where().eq("role", AgentMessageRole.ASSISTANT).isNotNull("inputTokens").order().desc("createdAt").findList();
    }
}
