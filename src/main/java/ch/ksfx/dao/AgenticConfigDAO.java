package ch.ksfx.dao;

import ch.ksfx.model.AgenticConfig;

public interface AgenticConfigDAO
{
    public AgenticConfig getAgenticConfig();
    public void saveOrUpdateAgenticConfig(AgenticConfig agenticConfig);
}
