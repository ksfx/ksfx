package ch.ksfx.dao;

import ch.ksfx.model.issues.IssueComment;

import java.util.List;

public interface IssueCommentDAO
{
    public void saveIssueComment(IssueComment issueComment);
    public void deleteIssueComment(IssueComment issueComment);
    public IssueComment getIssueCommentForId(Long issueCommentId);

    /** All comments on one issue, oldest first (a conversation reads top-down). */
    public List<IssueComment> getCommentsForIssue(Long issueId);

    public int countCommentsForIssue(Long issueId);
}
