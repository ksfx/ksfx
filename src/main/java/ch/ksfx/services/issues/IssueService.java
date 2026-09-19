package ch.ksfx.services.issues;

import ch.ksfx.dao.IssueDAO;
import ch.ksfx.dao.IssueLabelDAO;
import ch.ksfx.model.issues.Issue;
import ch.ksfx.model.issues.IssueLabel;
import ch.ksfx.model.issues.IssueTracker;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Shared issue/label logic for IssueController (web) and AgentIssueApiController (agent API) -
 * mainly label resolution, so both callers attach labels the same way: the web form sends existing
 * label ids, the agent API sends label *names* (auto-created if unknown, so agents never manage
 * label ids - same convenience idea as WikiService.resolveOrCreateFolderPath).
 */
@Service
public class IssueService
{
    /** Neutral grey for labels auto-created by name via the agent API - editable later on the labels page. */
    public static final String DEFAULT_LABEL_COLOR = "#8a8a93";

    private final IssueDAO issueDAO;
    private final IssueLabelDAO issueLabelDAO;

    public IssueService(IssueDAO issueDAO, IssueLabelDAO issueLabelDAO)
    {
        this.issueDAO = issueDAO;
        this.issueLabelDAO = issueLabelDAO;
    }

    /** Touches updatedAt and saves - every mutation (field edit, status change, new comment) goes through this. */
    public void touchAndSave(Issue issue)
    {
        issue.setUpdatedAt(new Date());
        issueDAO.saveOrUpdateIssue(issue);
    }

    public List<IssueLabel> resolveLabelsByIds(Long issueTrackerId, List<Long> labelIds)
    {
        List<IssueLabel> labels = new ArrayList<>();

        if (labelIds == null) {
            return labels;
        }

        for (Long labelId : labelIds) {
            IssueLabel label = issueLabelDAO.getIssueLabelForId(labelId);

            // Silently dropping a label from another tracker (rather than erroring) keeps a stale
            // form submit from failing outright - the label simply doesn't stick.
            if (label != null && label.getIssueTracker().getId().equals(issueTrackerId)) {
                labels.add(label);
            }
        }

        return labels;
    }

    public List<IssueLabel> resolveOrCreateLabelsByNames(IssueTracker tracker, List<String> names)
    {
        List<IssueLabel> labels = new ArrayList<>();

        if (names == null) {
            return labels;
        }

        for (String rawName : names) {
            String name = rawName == null ? "" : rawName.trim();

            if (name.isEmpty()) {
                continue;
            }

            IssueLabel label = issueLabelDAO.getLabelForTrackerAndName(tracker.getId(), name);

            if (label == null) {
                label = new IssueLabel();
                label.setIssueTracker(tracker);
                label.setName(name);
                label.setColor(DEFAULT_LABEL_COLOR);
                label.setCreatedAt(new Date());
                issueLabelDAO.saveOrUpdateIssueLabel(label);
            }

            labels.add(label);
        }

        return labels;
    }
}
