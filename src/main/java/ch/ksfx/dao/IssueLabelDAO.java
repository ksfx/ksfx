package ch.ksfx.dao;

import ch.ksfx.model.issues.IssueLabel;

import java.util.List;

public interface IssueLabelDAO
{
    public void saveOrUpdateIssueLabel(IssueLabel issueLabel);
    public void deleteIssueLabel(IssueLabel issueLabel);
    public IssueLabel getIssueLabelForId(Long issueLabelId);
    public List<IssueLabel> getLabelsForTracker(Long issueTrackerId);
    public IssueLabel getLabelForTrackerAndName(Long issueTrackerId, String name);
}
