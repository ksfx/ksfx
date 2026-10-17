package ch.ksfx.dao.ebean;

import ch.ksfx.dao.WikiFolderDAO;
import ch.ksfx.model.wiki.WikiFolder;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanWikiFolderDAO implements WikiFolderDAO
{
    @Override
    public void saveOrUpdateWikiFolder(WikiFolder wikiFolder)
    {
        if (wikiFolder.getId() != null) {
            Ebean.update(wikiFolder);
        } else {
            Ebean.save(wikiFolder);
        }
    }

    @Override
    public void deleteWikiFolder(WikiFolder wikiFolder)
    {
        Ebean.delete(wikiFolder);
    }

    @Override
    public WikiFolder getWikiFolderForId(Long wikiFolderId)
    {
        return Ebean.find(WikiFolder.class, wikiFolderId);
    }

    @Override
    public List<WikiFolder> getFoldersForWiki(Long wikiId)
    {
        return Ebean.find(WikiFolder.class).where().eq("wiki.id", wikiId).order().asc("name").findList();
    }

    @Override
    public WikiFolder getFolderByParentAndName(Long wikiId, Long parentFolderId, String name)
    {
        ExpressionList<WikiFolder> query = Ebean.find(WikiFolder.class).where().eq("wiki.id", wikiId).eq("name", name);

        if (parentFolderId != null) {
            query = query.eq("parent.id", parentFolderId);
        } else {
            query = query.isNull("parent");
        }

        return query.findUnique();
    }
}
