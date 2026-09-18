package ch.ksfx.services.wiki;

import ch.ksfx.dao.WikiFolderDAO;
import ch.ksfx.dao.WikiPageDAO;
import ch.ksfx.dao.WikiPageVersionDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.user.User;
import ch.ksfx.model.wiki.Wiki;
import ch.ksfx.model.wiki.WikiFolder;
import ch.ksfx.model.wiki.WikiPage;
import ch.ksfx.model.wiki.WikiPageVersion;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Page create/update and folder-path resolution, shared by WikiController (web editor,
 * editedByUser set) and AgentWikiApiController (agent's own bearer token, editedByAgent set) -
 * exactly one of the two caller params is non-null per call, mirroring WikiPageVersion's own
 * "exactly one of editedByUser/editedByAgent" comment.
 */
@Service
public class WikiService
{
    private final WikiFolderDAO wikiFolderDAO;
    private final WikiPageDAO wikiPageDAO;
    private final WikiPageVersionDAO wikiPageVersionDAO;

    public WikiService(WikiFolderDAO wikiFolderDAO, WikiPageDAO wikiPageDAO, WikiPageVersionDAO wikiPageVersionDAO)
    {
        this.wikiFolderDAO = wikiFolderDAO;
        this.wikiPageDAO = wikiPageDAO;
        this.wikiPageVersionDAO = wikiPageVersionDAO;
    }

    /** New page - generates a fresh, folder-unique slug from the title (see {@link #generateUniqueSlug}). */
    public WikiPage createPage(Wiki wiki, WikiFolder folder, String title, String content, User editedByUser, Agent editedByAgent)
    {
        WikiPage page = new WikiPage();
        page.setWiki(wiki);
        page.setFolder(folder);
        page.setSlug(generateUniqueSlug(wiki, folder, title));
        page.setTitle(title);

        Date now = new Date();
        page.setCreatedAt(now);
        page.setUpdatedAt(now);
        wikiPageDAO.saveOrUpdateWikiPage(page);

        appendVersion(page, content, editedByUser, editedByAgent);
        return page;
    }

    /**
     * Existing page - title and folder (a "move") can both change, but {@link WikiPage#getSlug()}
     * never does, see that field's own comment.
     */
    public WikiPage updatePage(WikiPage page, WikiFolder folder, String title, String content, User editedByUser, Agent editedByAgent)
    {
        page.setFolder(folder);
        page.setTitle(title);
        page.setUpdatedAt(new Date());
        wikiPageDAO.saveOrUpdateWikiPage(page);

        appendVersion(page, content, editedByUser, editedByAgent);
        return page;
    }

    private void appendVersion(WikiPage page, String content, User editedByUser, Agent editedByAgent)
    {
        WikiPageVersion version = new WikiPageVersion();
        version.setWikiPage(page);
        version.setContent(content != null ? content : "");
        version.setCreatedAt(new Date());
        version.setEditedByUser(editedByUser);
        version.setEditedByAgent(editedByAgent);
        wikiPageVersionDAO.saveWikiPageVersion(version);
    }

    /**
     * Lowercases, replaces anything that isn't a-z/0-9 with "-", trims stray dashes, appends ".md"
     * (pages are markdown files, matching the "+ New .md" convention already used by the Agentic
     * file browser). "-2", "-3", ... is appended on a collision within the same folder - the DB
     * unique constraint on (wiki, folder, slug) is the actual backstop (see the migration), this
     * loop just avoids ever hitting it in the normal case.
     */
    public String generateUniqueSlug(Wiki wiki, WikiFolder folder, String title)
    {
        String stem = title.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");

        if (stem.isEmpty()) {
            stem = "page";
        }

        String candidate = stem + ".md";
        int counter = 2;

        while (wikiPageDAO.getPageForFolderAndSlug(wiki.getId(), folder != null ? folder.getId() : null, candidate) != null) {
            candidate = stem + "-" + counter + ".md";
            counter++;
        }

        return candidate;
    }

    /**
     * Splits a "/"-separated folder path (e.g. "team/onboarding") into real WikiFolder rows,
     * creating any that don't exist yet - used by the agent API so an agent can address a folder by
     * name without first having to look up or create its id. Blank/null means the wiki root (returns
     * null). The web UI never calls this - a human picks an existing folder from a dropdown instead.
     */
    public WikiFolder resolveOrCreateFolderPath(Wiki wiki, String folderPath)
    {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            return null;
        }

        WikiFolder parent = null;

        for (String segment : folderPath.split("/")) {
            String name = segment.trim();

            if (name.isEmpty()) {
                continue;
            }

            WikiFolder existing = wikiFolderDAO.getFolderByParentAndName(wiki.getId(), parent != null ? parent.getId() : null, name);

            if (existing == null) {
                existing = new WikiFolder();
                existing.setWiki(wiki);
                existing.setParent(parent);
                existing.setName(name);
                existing.setCreatedAt(new Date());
                wikiFolderDAO.saveOrUpdateWikiFolder(existing);
            }

            parent = existing;
        }

        return parent;
    }
}
