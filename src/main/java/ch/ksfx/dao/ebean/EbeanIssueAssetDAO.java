package ch.ksfx.dao.ebean;

import ch.ksfx.dao.IssueAssetDAO;
import ch.ksfx.model.issues.IssueAsset;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanIssueAssetDAO implements IssueAssetDAO
{
    @Override
    public void saveIssueAsset(IssueAsset issueAsset)
    {
        Ebean.save(issueAsset);
    }

    @Override
    public void deleteIssueAsset(IssueAsset issueAsset)
    {
        Ebean.delete(issueAsset);
    }

    @Override
    public IssueAsset getIssueAssetForId(Long issueAssetId)
    {
        return Ebean.find(IssueAsset.class, issueAssetId);
    }

    @Override
    public List<IssueAsset> getAssetsForIssue(Long issueId)
    {
        // select() deliberately leaves out `content` - same blob-loading trap as
        // EbeanWikiAssetDAO.getAssetsForPage, see that comment.
        return Ebean.find(IssueAsset.class)
                .select("fileName, contentType, fileSize, createdAt")
                .fetch("uploadedByUser").fetch("uploadedByAgent")
                .where().eq("issue.id", issueId)
                .order().asc("id")
                .findList();
    }
}
