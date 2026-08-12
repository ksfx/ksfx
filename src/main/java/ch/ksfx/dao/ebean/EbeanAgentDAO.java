package ch.ksfx.dao.ebean;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.model.Agent;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Iterator;
import java.util.List;

@Repository
public class EbeanAgentDAO implements AgentDAO
{
    @Override
    public void saveOrUpdateAgent(Agent agent)
    {
        if (agent.getId() != null) {
            Ebean.update(agent);
        } else {
            Ebean.save(agent);
        }
    }

    @Override
    public void deleteAgent(Agent agent)
    {
        Ebean.delete(agent);
    }

    @Override
    public List<Agent> getAllAgents()
    {
        return Ebean.find(Agent.class).findList();
    }

    @Override
    public Page<Agent> getAgentsForPageable(Pageable pageable)
    {
        ExpressionList expressionList = Ebean.find(Agent.class).where();

        expressionList.setFirstRow(new Long(pageable.getOffset()).intValue());
        expressionList.setMaxRows(pageable.getPageSize());

        boolean hasOrder = false;

        if (!pageable.getSort().isUnsorted()) {
            Iterator<Sort.Order> orderIterator = pageable.getSort().iterator();
            while (orderIterator.hasNext()) {
                Sort.Order order = orderIterator.next();

                if (!order.getProperty().equals("UNSORTED")) {
                    if (order.isAscending()) {
                        expressionList.order().asc(order.getProperty());
                        hasOrder = true;
                    }

                    if (order.isDescending()) {
                        expressionList.order().desc(order.getProperty());
                        hasOrder = true;
                    }
                }
            }
        }

        if (!hasOrder) {
            expressionList.order().asc("name");
        }

        return new PageImpl<Agent>(expressionList.findList(), pageable, expressionList.findCount());
    }

    @Override
    public Agent getAgentForId(Long agentId)
    {
        return Ebean.find(Agent.class, agentId);
    }

    @Override
    public Agent getAgentForApiToken(String apiToken)
    {
        return Ebean.find(Agent.class).where().eq("apiToken", apiToken).findUnique();
    }

    @Override
    public List<Agent> getAgentsForAgenticProject(Long agenticProjectId)
    {
        return Ebean.find(Agent.class).where().eq("agenticProject.id", agenticProjectId).order().asc("name").findList();
    }

    @Override
    public List<Agent> getAgentsWithoutAgenticProject()
    {
        return Ebean.find(Agent.class).where().isNull("agenticProject").order().asc("name").findList();
    }
}
