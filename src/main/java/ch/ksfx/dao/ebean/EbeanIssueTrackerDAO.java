package ch.ksfx.dao.ebean;

import ch.ksfx.dao.IssueTrackerDAO;
import ch.ksfx.model.issues.IssueTracker;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanIssueTrackerDAO implements IssueTrackerDAO
{
    @Override
    public void saveOrUpdateIssueTracker(IssueTracker issueTracker)
    {
        if (issueTracker.getId() != null) {
            Ebean.update(issueTracker);
        } else {
            Ebean.save(issueTracker);
        }
    }

    @Override
    public void deleteIssueTracker(IssueTracker issueTracker)
    {
        Ebean.delete(issueTracker);
    }

    @Override
    public IssueTracker getIssueTrackerForId(Long issueTrackerId)
    {
        return Ebean.find(IssueTracker.class, issueTrackerId);
    }

    @Override
    public List<IssueTracker> getAllIssueTrackers()
    {
        return Ebean.find(IssueTracker.class).order().asc("name").findList();
    }
}
