package ch.ksfx.dao;

import ch.ksfx.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectDAO
{
    public void saveOrUpdateProject(Project project);
    public void deleteProject(Project project);
    public List<Project> getAllProjects();
    public Page<Project> getProjectsForPageable(Pageable pageable);
    public Project getProjectForId(Long projectId);
    /** The one project flagged default (see Project#getDefaultProject) - never null after the project migration. */
    public Project getDefaultProject();
}
