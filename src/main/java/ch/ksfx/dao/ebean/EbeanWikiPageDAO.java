package ch.ksfx.dao.ebean;

import ch.ksfx.dao.WikiPageDAO;
import ch.ksfx.model.wiki.WikiPage;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanWikiPageDAO implements WikiPageDAO
{
    @Override
    public void saveOrUpdateWikiPage(WikiPage wikiPage)
    {
        if (wikiPage.getId() != null) {
            Ebean.update(wikiPage);
        } else {
            Ebean.save(wikiPage);
        }
    }

    @Override
    public void deleteWikiPage(WikiPage wikiPage)
    {
        Ebean.delete(wikiPage);
    }

    @Override
    public WikiPage getWikiPageForId(Long wikiPageId)
    {
        return Ebean.find(WikiPage.class, wikiPageId);
    }

    @Override
    public List<WikiPage> getAllWikiPages(Long wikiId)
    {
        return Ebean.find(WikiPage.class).where().eq("wiki.id", wikiId).order().asc("title").findList();
    }

    @Override
    public WikiPage getPageForFolderAndSlug(Long wikiId, Long folderId, String slug)
    {
        ExpressionList<WikiPage> query = Ebean.find(WikiPage.class).where().eq("wiki.id", wikiId).eq("slug", slug);
        return scopeToFolder(query, folderId).findUnique();
    }

    @Override
    public WikiPage getPageForFolderAndTitle(Long wikiId, Long folderId, String title)
    {
        ExpressionList<WikiPage> query = Ebean.find(WikiPage.class).where().eq("wiki.id", wikiId).eq("title", title);
        return scopeToFolder(query, folderId).findUnique();
    }

    @Override
    public List<WikiPage> getChildPages(Long parentPageId)
    {
        return Ebean.find(WikiPage.class).where().eq("parentPage.id", parentPageId).order().asc("title").findList();
    }

    private ExpressionList<WikiPage> scopeToFolder(ExpressionList<WikiPage> query, Long folderId)
    {
        return folderId != null ? query.eq("folder.id", folderId) : query.isNull("folder");
    }
}
