package ch.ksfx.dao;

import ch.ksfx.model.Agent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AgentDAO
{
    public void saveOrUpdateAgent(Agent agent);
    public void deleteAgent(Agent agent);
    public List<Agent> getAllAgents();
    public Page<Agent> getAgentsForPageable(Pageable pageable);
    public Agent getAgentForId(Long agentId);
    public Agent getAgentForApiToken(String apiToken);
    public List<Agent> getAgentsForAgenticProject(Long agenticProjectId);
    public List<Agent> getAgentsWithoutAgenticProject();
}
