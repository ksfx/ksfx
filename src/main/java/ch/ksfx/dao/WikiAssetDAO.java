package ch.ksfx.dao;

import ch.ksfx.model.wiki.WikiAsset;

import java.util.List;

public interface WikiAssetDAO
{
    public void saveWikiAsset(WikiAsset wikiAsset);
    public void deleteWikiAsset(WikiAsset wikiAsset);
    public WikiAsset getWikiAssetForId(Long wikiAssetId);

    /** The files attached to one page (wikiPage set - inline editor images never appear here), without loading their blobs. */
    public List<WikiAsset> getAssetsForPage(Long wikiPageId);
}
