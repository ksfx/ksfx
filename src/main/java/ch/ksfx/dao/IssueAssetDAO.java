package ch.ksfx.dao;

import ch.ksfx.model.issues.IssueAsset;

import java.util.List;

public interface IssueAssetDAO
{
    public void saveIssueAsset(IssueAsset issueAsset);
    public void deleteIssueAsset(IssueAsset issueAsset);
    public IssueAsset getIssueAssetForId(Long issueAssetId);

    /** The files attached to one issue (issue set - inline editor images never appear here), without loading their blobs. */
    public List<IssueAsset> getAssetsForIssue(Long issueId);
}
