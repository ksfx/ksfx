package ch.ksfx.dao.ebean;

import ch.ksfx.dao.IssueLabelDAO;
import ch.ksfx.model.issues.IssueLabel;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanIssueLabelDAO implements IssueLabelDAO
{
    @Override
    public void saveOrUpdateIssueLabel(IssueLabel issueLabel)
    {
        if (issueLabel.getId() != null) {
            Ebean.update(issueLabel);
        } else {
            Ebean.save(issueLabel);
        }
    }

    @Override
    public void deleteIssueLabel(IssueLabel issueLabel)
    {
        Ebean.delete(issueLabel);
    }

    @Override
    public IssueLabel getIssueLabelForId(Long issueLabelId)
    {
        return Ebean.find(IssueLabel.class, issueLabelId);
    }

    @Override
    public List<IssueLabel> getLabelsForTracker(Long issueTrackerId)
    {
        return Ebean.find(IssueLabel.class).where().eq("issueTracker.id", issueTrackerId).order().asc("name").findList();
    }

    @Override
    public IssueLabel getLabelForTrackerAndName(Long issueTrackerId, String name)
    {
        return Ebean.find(IssueLabel.class).where().eq("issueTracker.id", issueTrackerId).eq("name", name).findUnique();
    }
}
