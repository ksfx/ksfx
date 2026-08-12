package ch.ksfx.dao;

import ch.ksfx.model.AgenticProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AgenticProjectDAO
{
    public void saveOrUpdateAgenticProject(AgenticProject agenticProject);
    public void deleteAgenticProject(AgenticProject agenticProject);
    public List<AgenticProject> getAllAgenticProjects();
    public Page<AgenticProject> getAgenticProjectsForPageable(Pageable pageable);
    public AgenticProject getAgenticProjectForId(Long agenticProjectId);
}
