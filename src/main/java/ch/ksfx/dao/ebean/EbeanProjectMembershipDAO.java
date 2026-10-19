package ch.ksfx.dao.ebean;

import ch.ksfx.dao.ProjectMembershipDAO;
import ch.ksfx.model.ProjectMembership;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanProjectMembershipDAO implements ProjectMembershipDAO
{
    @Override
    public List<ProjectMembership> getMembershipsForUser(Long userId)
    {
        return Ebean.find(ProjectMembership.class).where().eq("user.id", userId).order().asc("project.name").findList();
    }

    @Override
    public List<ProjectMembership> getMembershipsForProject(Long projectId)
    {
        return Ebean.find(ProjectMembership.class).where().eq("project.id", projectId).order().asc("user.username").findList();
    }

    @Override
    public ProjectMembership getMembership(Long userId, Long projectId)
    {
        return Ebean.find(ProjectMembership.class).where().eq("user.id", userId).eq("project.id", projectId).setMaxRows(1).findOne();
    }

    @Override
    public void saveOrUpdateMembership(ProjectMembership membership)
    {
        if (membership.getId() != null) {
            Ebean.update(membership);
        } else {
            Ebean.save(membership);
        }
    }

    @Override
    public void deleteMembership(ProjectMembership membership)
    {
        Ebean.delete(membership);
    }
}
