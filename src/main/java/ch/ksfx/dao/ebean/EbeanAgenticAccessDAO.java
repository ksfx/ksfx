package ch.ksfx.dao.ebean;

import ch.ksfx.dao.AgenticAccessDAO;
import ch.ksfx.model.AgenticAccess;
import io.ebean.Ebean;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EbeanAgenticAccessDAO implements AgenticAccessDAO
{
    @Override
    public AgenticAccess getAgenticAccessForId(Long id)
    {
        return id != null ? Ebean.find(AgenticAccess.class, id) : null;
    }

    @Override
    public List<AgenticAccess> getAccessesForProject(Long projectId)
    {
        return Ebean.find(AgenticAccess.class).where().eq("project.id", projectId)
                .orderBy("name asc")
                .findList();
    }

    @Override
    public List<AgenticAccess> getAllAccesses()
    {
        return Ebean.find(AgenticAccess.class).fetch("project")
                .orderBy("project.name asc, name asc")
                .findList();
    }

    @Override
    public void saveOrUpdateAgenticAccess(AgenticAccess access)
    {
        if (access.getId() != null) {
            Ebean.update(access);
        } else {
            Ebean.save(access);
        }
    }

    @Override
    public void deleteAgenticAccess(AgenticAccess access)
    {
        // Only called for an access no agent uses (AgenticAccessController refuses otherwise);
        // agent_message.agentic_access_id is ON DELETE SET NULL - its usage stays as "deleted access".
        Ebean.delete(access);
    }
}
