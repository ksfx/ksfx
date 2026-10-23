package ch.ksfx.dao.ebean;

import ch.ksfx.dao.ProjectAgenticSettingsDAO;
import ch.ksfx.model.ProjectAgenticSettings;
import io.ebean.Ebean;
import org.springframework.stereotype.Service;

@Service
public class EbeanProjectAgenticSettingsDAO implements ProjectAgenticSettingsDAO
{
    @Override
    public ProjectAgenticSettings getForProjectId(Long projectId)
    {
        return Ebean.find(ProjectAgenticSettings.class).where().eq("project.id", projectId).findOne();
    }

    @Override
    public void saveOrUpdateProjectAgenticSettings(ProjectAgenticSettings settings)
    {
        if (settings.getId() != null) {
            Ebean.update(settings);
        } else {
            Ebean.save(settings);
        }
    }
}
