package ch.ksfx.dao;

import ch.ksfx.model.issues.IssueTracker;

import java.util.List;

public interface IssueTrackerDAO
{
    public void saveOrUpdateIssueTracker(IssueTracker issueTracker);

    /** Deleting a tracker cascades to its labels, issues, comments and attachments (see the DB FK definitions). */
    public void deleteIssueTracker(IssueTracker issueTracker);

    public IssueTracker getIssueTrackerForId(Long issueTrackerId);

    /** Every tracker, ordered by name - backs the sidebar switcher dropdown. */
    public List<IssueTracker> getAllIssueTrackers();
}
