package ch.ksfx.dao;

import ch.ksfx.model.ProjectAgenticSettings;

public interface ProjectAgenticSettingsDAO
{
    public ProjectAgenticSettings getForProjectId(Long projectId);

    public void saveOrUpdateProjectAgenticSettings(ProjectAgenticSettings settings);
}
