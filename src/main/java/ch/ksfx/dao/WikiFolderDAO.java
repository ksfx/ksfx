package ch.ksfx.dao;

import ch.ksfx.model.wiki.WikiFolder;

import java.util.List;

public interface WikiFolderDAO
{
    public void saveOrUpdateWikiFolder(WikiFolder wikiFolder);

    /** Deleting a folder cascades to its subfolders and pages (see the DB FK definitions). */
    public void deleteWikiFolder(WikiFolder wikiFolder);

    public WikiFolder getWikiFolderForId(Long wikiFolderId);

    /** Every folder in one wiki, flat - the tree shown in the sidebar is built from this list. */
    public List<WikiFolder> getFoldersForWiki(Long wikiId);

    /** {@code parentFolderId} null means top-level - used to resolve/auto-create a folder path (see WikiService). */
    public WikiFolder getFolderByParentAndName(Long wikiId, Long parentFolderId, String name);
}
