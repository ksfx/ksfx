package ch.ksfx.dao.ebean;

import ch.ksfx.dao.WikiAssetDAO;
import ch.ksfx.model.wiki.WikiAsset;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanWikiAssetDAO implements WikiAssetDAO
{
    @Override
    public void saveWikiAsset(WikiAsset wikiAsset)
    {
        Ebean.save(wikiAsset);
    }

    @Override
    public void deleteWikiAsset(WikiAsset wikiAsset)
    {
        Ebean.delete(wikiAsset);
    }

    @Override
    public WikiAsset getWikiAssetForId(Long wikiAssetId)
    {
        return Ebean.find(WikiAsset.class, wikiAssetId);
    }

    @Override
    public List<WikiAsset> getAssetsForPage(Long wikiPageId)
    {
        // select() deliberately leaves out `content` - an attachment listing needs names/sizes/
        // uploaders, not megabytes of blob per row.
        return Ebean.find(WikiAsset.class)
                .select("fileName, contentType, fileSize, createdAt")
                .fetch("uploadedByUser").fetch("uploadedByAgent")
                .where().eq("wikiPage.id", wikiPageId)
                .order().asc("id")
                .findList();
    }
}
