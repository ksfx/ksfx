package ch.ksfx.dao;

import ch.ksfx.model.wiki.WikiPage;

import java.util.List;

public interface WikiPageDAO
{
    public void saveOrUpdateWikiPage(WikiPage wikiPage);
    public void deleteWikiPage(WikiPage wikiPage);
    public WikiPage getWikiPageForId(Long wikiPageId);

    /** Every page in one wiki, ordered by title - the tree shown in the sidebar is built from this list. */
    public List<WikiPage> getAllWikiPages(Long wikiId);

    /** {@code folderId} null means top-level - used for slug-collision checks (see WikiService). */
    public WikiPage getPageForFolderAndSlug(Long wikiId, Long folderId, String slug);

    /** Same folder scoping, matched by title instead of slug - backs the agent API's upsert-by-location. */
    public WikiPage getPageForFolderAndTitle(Long wikiId, Long folderId, String title);

    /** Direct children only (one level), ordered by title - backs the "Subpages" list on the page view. */
    public List<WikiPage> getChildPages(Long parentPageId);
}
