package ch.ksfx.dao.ebean;

import ch.ksfx.dao.IssueCommentDAO;
import ch.ksfx.model.issues.IssueComment;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanIssueCommentDAO implements IssueCommentDAO
{
    @Override
    public void saveIssueComment(IssueComment issueComment)
    {
        Ebean.save(issueComment);
    }

    @Override
    public void deleteIssueComment(IssueComment issueComment)
    {
        Ebean.delete(issueComment);
    }

    @Override
    public IssueComment getIssueCommentForId(Long issueCommentId)
    {
        return Ebean.find(IssueComment.class, issueCommentId);
    }

    @Override
    public List<IssueComment> getCommentsForIssue(Long issueId)
    {
        return Ebean.find(IssueComment.class)
                .fetch("createdByUser").fetch("createdByAgent")
                .where().eq("issue.id", issueId)
                .order().asc("id")
                .findList();
    }

    @Override
    public int countCommentsForIssue(Long issueId)
    {
        return Ebean.find(IssueComment.class).where().eq("issue.id", issueId).findCount();
    }
}
