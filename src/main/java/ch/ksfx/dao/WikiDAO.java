package ch.ksfx.dao;

import ch.ksfx.model.wiki.Wiki;

import java.util.List;

public interface WikiDAO
{
    public void saveOrUpdateWiki(Wiki wiki);
    public void deleteWiki(Wiki wiki);
    public Wiki getWikiForId(Long wikiId);

    /** Every wiki, ordered by name - backs the sidebar switcher dropdown. */
    public List<Wiki> getAllWikis();
}
