package ch.ksfx.dao.ebean;

import ch.ksfx.dao.AgenticProjectDAO;
import ch.ksfx.model.AgenticProject;
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
public class EbeanAgenticProjectDAO implements AgenticProjectDAO
{
    @Override
    public void saveOrUpdateAgenticProject(AgenticProject agenticProject)
    {
        if (agenticProject.getId() != null) {
            Ebean.update(agenticProject);
        } else {
            Ebean.save(agenticProject);
        }
    }

    @Override
    public void deleteAgenticProject(AgenticProject agenticProject)
    {
        Ebean.delete(agenticProject);
    }

    @Override
    public List<AgenticProject> getAllAgenticProjects()
    {
        return Ebean.find(AgenticProject.class).order().asc("name").findList();
    }

    @Override
    public Page<AgenticProject> getAgenticProjectsForPageable(Pageable pageable)
    {
        ExpressionList expressionList = Ebean.find(AgenticProject.class).where();

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

        return new PageImpl<AgenticProject>(expressionList.findList(), pageable, expressionList.findCount());
    }

    @Override
    public AgenticProject getAgenticProjectForId(Long agenticProjectId)
    {
        return Ebean.find(AgenticProject.class, agenticProjectId);
    }
}
