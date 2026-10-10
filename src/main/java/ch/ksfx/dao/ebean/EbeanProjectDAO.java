package ch.ksfx.dao.ebean;

import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.Project;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Iterator;
import java.util.List;

@Repository
public class EbeanProjectDAO implements ProjectDAO
{
    @Override
    public void saveOrUpdateProject(Project project)
    {
        if (project.getId() != null) {
            Ebean.update(project);
        } else {
            Ebean.save(project);
        }
    }

    @Override
    public void deleteProject(Project project)
    {
        Ebean.delete(project);
    }

    @Override
    public List<Project> getAllProjects()
    {
        return Ebean.find(Project.class).order().asc("name").findList();
    }

    @Override
    public Page<Project> getProjectsForPageable(Pageable pageable)
    {
        ExpressionList expressionList = Ebean.find(Project.class).where();

        expressionList.setFirstRow(new Long(pageable.getOffset()).intValue());
        expressionList.setMaxRows(pageable.getPageSize());

        boolean hasOrder = false;

        if (!pageable.getSort().isUnsorted()) {
            Iterator<Sort.Order> orderIterator = pageable.getSort().iterator();
            while (orderIterator.hasNext()) {
                Sort.Order order = orderIterator.next();

                if (!order.getProperty().equals("UNSORTED")) {
                    if (order.isAscending()) {
                        expressionList.order().asc(order.getProperty());
                        hasOrder = true;
                    }

                    if (order.isDescending()) {
                        expressionList.order().desc(order.getProperty());
                        hasOrder = true;
                    }
                }
            }
        }

        if (!hasOrder) {
            expressionList.order().asc("name");
        }

        return new PageImpl<Project>(expressionList.findList(), pageable, expressionList.findCount());
    }

    @Override
    public Project getProjectForId(Long projectId)
    {
        return Ebean.find(Project.class, projectId);
    }

    @Override
    public Project getDefaultProject()
    {
        return Ebean.find(Project.class).where().eq("defaultProject", true).setMaxRows(1).findOne();
    }
}
