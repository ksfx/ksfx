package ch.ksfx.dao;

import ch.ksfx.model.issues.Issue;
import ch.ksfx.model.issues.IssueStatus;

import java.util.List;

public interface IssueDAO
{
    public void saveOrUpdateIssue(Issue issue);
    public void deleteIssue(Issue issue);
    public Issue getIssueForId(Long issueId);

    /**
     * Issues of one tracker, most recently updated first. {@code status} null means all statuses;
     * the UI's "Open" tab passes OPEN and IN_PROGRESS together (see the two-arg convenience
     * semantics in EbeanIssueDAO).
     */
    public List<Issue> getIssuesForTracker(Long issueTrackerId, List<IssueStatus> statuses);

    /** Per-status counts for the sidebar filter badges, without loading any issues. */
    public int countIssuesForTracker(Long issueTrackerId, List<IssueStatus> statuses);
}
