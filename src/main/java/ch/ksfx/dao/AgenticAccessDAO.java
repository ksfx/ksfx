package ch.ksfx.dao;

import ch.ksfx.model.AgenticAccess;

import java.util.List;

public interface AgenticAccessDAO
{
    public AgenticAccess getAgenticAccessForId(Long id);

    /** A project's accesses, the default one first, then by name. */
    public List<AgenticAccess> getAccessesForProject(Long projectId);

    public AgenticAccess getDefaultAccessForProject(Long projectId);

    /** Every access of every project, for the instance admin's overview. */
    public List<AgenticAccess> getAllAccesses();

    public void saveOrUpdateAgenticAccess(AgenticAccess access);

    public void deleteAgenticAccess(AgenticAccess access);
}
