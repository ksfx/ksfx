package ch.ksfx.dao;

import ch.ksfx.model.wiki.WikiPageVersion;

import java.util.List;

public interface WikiPageVersionDAO
{
    public void saveWikiPageVersion(WikiPageVersion wikiPageVersion);
    public WikiPageVersion getWikiPageVersionForId(Long wikiPageVersionId);

    /** Most recent version first - element 0 is the page's current content. */
    public List<WikiPageVersion> getVersionsForPage(Long wikiPageId);

    /** Just the current content, without loading the whole history - the common case (viewing a page). */
    public WikiPageVersion getLatestVersionForPage(Long wikiPageId);
}
