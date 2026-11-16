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
                .orderBy("defaultAccess desc, name asc")
                .findList();
    }

    @Override
    public AgenticAccess getDefaultAccessForProject(Long projectId)
    {
        return Ebean.find(AgenticAccess.class).where().eq("project.id", projectId).eq("defaultAccess", true)
                .setMaxRows(1)
                .findOne();
    }

    @Override
    public List<AgenticAccess> getAllAccesses()
    {
        return Ebean.find(AgenticAccess.class).fetch("project")
                .orderBy("project.name asc, defaultAccess desc, name asc")
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
        // agent.agentic_access_id and agent_message.agentic_access_id are ON DELETE SET NULL:
        // its agents fall back to the default access, its usage stays as "deleted access".
        Ebean.delete(access);
    }
}
