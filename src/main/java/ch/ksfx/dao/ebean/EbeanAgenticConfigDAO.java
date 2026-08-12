package ch.ksfx.dao.ebean;

import ch.ksfx.dao.AgenticConfigDAO;
import ch.ksfx.model.AgenticConfig;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * There is exactly one {@link AgenticConfig} row per KSFX instance.
 */
@Repository
public class EbeanAgenticConfigDAO implements AgenticConfigDAO
{
    @Override
    public AgenticConfig getAgenticConfig()
    {
        List<AgenticConfig> configs = Ebean.find(AgenticConfig.class).setMaxRows(1).findList();

        return configs.isEmpty() ? null : configs.get(0);
    }

    @Override
    public void saveOrUpdateAgenticConfig(AgenticConfig agenticConfig)
    {
        if (agenticConfig.getId() != null) {
            Ebean.update(agenticConfig);
        } else {
            Ebean.save(agenticConfig);
        }
    }
}
