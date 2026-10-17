package ch.ksfx.dao.ebean;

import ch.ksfx.dao.WikiPageVersionDAO;
import ch.ksfx.model.wiki.WikiPageVersion;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanWikiPageVersionDAO implements WikiPageVersionDAO
{
    @Override
    public void saveWikiPageVersion(WikiPageVersion wikiPageVersion)
    {
        Ebean.save(wikiPageVersion);
    }

    @Override
    public WikiPageVersion getWikiPageVersionForId(Long wikiPageVersionId)
    {
        return Ebean.find(WikiPageVersion.class, wikiPageVersionId);
    }

    @Override
    public List<WikiPageVersion> getVersionsForPage(Long wikiPageId)
    {
        return Ebean.find(WikiPageVersion.class).fetch("editedByUser").fetch("editedByAgent")
                .where().eq("wikiPage.id", wikiPageId)
                .order().desc("id")
                .findList();
    }

    @Override
    public WikiPageVersion getLatestVersionForPage(Long wikiPageId)
    {
        return Ebean.find(WikiPageVersion.class).fetch("editedByUser").fetch("editedByAgent")
                .where().eq("wikiPage.id", wikiPageId)
                .order().desc("id")
                .setMaxRows(1)
                .findOneOrEmpty()
                .orElse(null);
    }
}
