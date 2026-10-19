package ch.ksfx.dao;

import ch.ksfx.model.ProjectMembership;

import java.util.List;

public interface ProjectMembershipDAO
{
    public List<ProjectMembership> getMembershipsForUser(Long userId);
    public List<ProjectMembership> getMembershipsForProject(Long projectId);
    public ProjectMembership getMembership(Long userId, Long projectId);
    public void saveOrUpdateMembership(ProjectMembership membership);
    public void deleteMembership(ProjectMembership membership);
}
