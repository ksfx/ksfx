package ch.ksfx.dao.ebean;

import ch.ksfx.dao.WikiDAO;
import ch.ksfx.model.wiki.Wiki;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanWikiDAO implements WikiDAO
{
    @Override
    public void saveOrUpdateWiki(Wiki wiki)
    {
        if (wiki.getId() != null) {
            Ebean.update(wiki);
        } else {
            Ebean.save(wiki);
        }
    }

    @Override
    public void deleteWiki(Wiki wiki)
    {
        Ebean.delete(wiki);
    }

    @Override
    public Wiki getWikiForId(Long wikiId)
    {
        return Ebean.find(Wiki.class, wikiId);
    }

    @Override
    public List<Wiki> getAllWikis()
    {
        return Ebean.find(Wiki.class).order().asc("name").findList();
    }
}
