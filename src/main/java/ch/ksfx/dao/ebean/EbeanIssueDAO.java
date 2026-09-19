package ch.ksfx.dao.ebean;

import ch.ksfx.dao.IssueDAO;
import ch.ksfx.model.issues.Issue;
import ch.ksfx.model.issues.IssueStatus;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanIssueDAO implements IssueDAO
{
    @Override
    public void saveOrUpdateIssue(Issue issue)
    {
        if (issue.getId() != null) {
            Ebean.update(issue);
        } else {
            Ebean.save(issue);
        }
    }

    @Override
    public void deleteIssue(Issue issue)
    {
        Ebean.delete(issue);
    }

    @Override
    public Issue getIssueForId(Long issueId)
    {
        return Ebean.find(Issue.class, issueId);
    }

    @Override
    public List<Issue> getIssuesForTracker(Long issueTrackerId, List<IssueStatus> statuses)
    {
        return scoped(issueTrackerId, statuses).order().desc("updatedAt").findList();
    }

    @Override
    public int countIssuesForTracker(Long issueTrackerId, List<IssueStatus> statuses)
    {
        return scoped(issueTrackerId, statuses).findCount();
    }

    private ExpressionList<Issue> scoped(Long issueTrackerId, List<IssueStatus> statuses)
    {
        ExpressionList<Issue> query = Ebean.find(Issue.class)
                .fetch("labels").fetch("assigneeUser").fetch("assigneeAgent")
                .where().eq("issueTracker.id", issueTrackerId);

        if (statuses != null && !statuses.isEmpty()) {
            query = query.in("status", statuses);
        }

        return query;
    }
}
