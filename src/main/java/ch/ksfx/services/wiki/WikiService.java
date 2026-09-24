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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // ------------------------------------------------------------------------------------------
    // Wikilinks: [[Page Title]], [[folder/path/Page Title]], [[target|shown label]] - resolved at
    // RENDER time (the stored markdown keeps the [[...]] form), so links always show the page's
    // current title and re-resolve after renames. The no-label form and the "|" label separator
    // are the same syntax as GitHub/Obsidian wikis, which is what makes migrating existing GitHub
    // wikis in feasible without rewriting their links.
    //
    // "#" is also accepted as a label separator ([[target#shown label]]), kept equivalent to "|":
    // the WYSIWYG editor (Toast UI) treats a bare "|" as possible GFM table syntax and can mangle
    // the whole line when converting its rich-text model back to markdown on save, even if nothing
    // was actually edited - "#" isn't special to markdown mid-line, so it survives that round trip.
    // "|" still resolves (existing pages, and pasted-in GitHub/Obsidian wikilinks, keep working
    // unchanged) - "#" is just the recommended separator for anything typed directly in this wiki.
    // ------------------------------------------------------------------------------------------

    private static final Pattern WIKILINK_PATTERN = Pattern.compile("\\[\\[([^\\[\\]|#]+?)(?:[|#]([^\\[\\]]+?))?\\]\\]");

    /**
     * Replaces every [[...]] outside of code with a regular markdown link: to the matched page
     * (id-based URL, so the link target itself is rename-proof), or - if no page matches - to the
     * pre-filled new-page form ("red link" semantics; wiki.js styles these via the /page/new href).
     * Targets resolve by title or by slug (with or without .md), optionally folder-qualified
     * ("team/onboarding/Setup Guide"); unqualified targets match anywhere in the wiki (root first,
     * then alphabetically by folder path, for determinism when titles repeat across folders).
     *
     * Code is left alone on two levels: fenced blocks (``` / ~~~ toggle, tracked line by line) and
     * inline `code` spans (odd-indexed segments after splitting a line on backticks). Deliberately
     * simple - a fence inside a fence-info string or unbalanced backticks can fool it, which is an
     * accepted trade-off over pulling in a real markdown parser just for this.
     */
    public String renderWikilinks(Wiki wiki, String markdown)
    {
        if (markdown == null || !markdown.contains("[[")) {
            return markdown;
        }

        WikilinkResolver resolver = new WikilinkResolver(wiki);
        String[] lines = markdown.split("\n", -1);
        StringBuilder out = new StringBuilder();
        boolean inFence = false;

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();

            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                inFence = !inFence;
                out.append(lines[i]);
            } else if (inFence) {
                out.append(lines[i]);
            } else {
                String[] segments = lines[i].split("`", -1);

                for (int s = 0; s < segments.length; s += 2) {
                    segments[s] = replaceWikilinks(segments[s], resolver);
                }

                out.append(String.join("`", segments));
            }

            if (i < lines.length - 1) {
                out.append("\n");
            }
        }

        return out.toString();
    }

    private String replaceWikilinks(String text, WikilinkResolver resolver)
    {
        if (!text.contains("[[")) {
            return text;
        }

        Matcher matcher = WIKILINK_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String target = matcher.group(1).trim();
            String label = matcher.group(2) != null ? matcher.group(2).trim() : null;

            if (target.isEmpty()) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                continue;
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(resolver.toMarkdownLink(target, label)));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * One instance per render: loads the wiki's folders and pages once (two queries) and answers
     * every [[...]] lookup from in-memory maps - a page view resolves any number of wikilinks at
     * constant query cost.
     */
    private class WikilinkResolver
    {
        private final Wiki wiki;
        private final Map<String, WikiPage> byQualifiedTitle = new HashMap<>();
        private final Map<String, WikiPage> byQualifiedSlug = new HashMap<>();
        private final Map<String, WikiPage> byTitle = new HashMap<>();
        private final Map<String, WikiPage> bySlug = new HashMap<>();
        private final Map<String, Long> folderIdByPath = new HashMap<>();

        private WikilinkResolver(Wiki wiki)
        {
            this.wiki = wiki;

            Map<Long, WikiFolder> foldersById = new HashMap<>();

            for (WikiFolder folder : wikiFolderDAO.getFoldersForWiki(wiki.getId())) {
                foldersById.put(folder.getId(), folder);
            }

            Map<Long, String> pathByFolderId = new HashMap<>();

            for (WikiFolder folder : foldersById.values()) {
                String path = folderPath(folder, foldersById);
                pathByFolderId.put(folder.getId(), path);
                folderIdByPath.put(path.toLowerCase(), folder.getId());
            }

            List<WikiPage> pages = new ArrayList<>(wikiPageDAO.getAllWikiPages(wiki.getId()));

            // Root pages first, then alphabetically by folder path - makes the unqualified-title
            // winner deterministic when the same title exists in several folders.
            pages.sort((a, b) -> {
                String pathA = a.getFolder() != null ? pathByFolderId.getOrDefault(a.getFolder().getId(), "") : "";
                String pathB = b.getFolder() != null ? pathByFolderId.getOrDefault(b.getFolder().getId(), "") : "";
                return pathA.compareToIgnoreCase(pathB);
            });

            for (WikiPage page : pages) {
                String path = page.getFolder() != null ? pathByFolderId.getOrDefault(page.getFolder().getId(), "") : "";
                String pathPrefix = path.toLowerCase() + "\0";
                String titleKey = page.getTitle().toLowerCase();
                String slugKey = slugStem(page.getSlug());

                byQualifiedTitle.putIfAbsent(pathPrefix + titleKey, page);
                byQualifiedSlug.putIfAbsent(pathPrefix + slugKey, page);
                byTitle.putIfAbsent(titleKey, page);
                bySlug.putIfAbsent(slugKey, page);
            }
        }

        private String toMarkdownLink(String target, String label)
        {
            String folderPart = "";
            String namePart = target;
            int lastSlash = target.lastIndexOf('/');

            if (lastSlash >= 0) {
                folderPart = target.substring(0, lastSlash).trim();
                namePart = target.substring(lastSlash + 1).trim();
            }

            String nameKey = namePart.toLowerCase();
            String nameSlugKey = slugStem(nameKey);
            WikiPage page;

            if (!folderPart.isEmpty()) {
                String pathPrefix = normalizePathKey(folderPart) + "\0";
                page = byQualifiedTitle.get(pathPrefix + nameKey);

                if (page == null) {
                    page = byQualifiedSlug.get(pathPrefix + nameSlugKey);
                }
            } else {
                page = byTitle.get(nameKey);

                if (page == null) {
                    page = bySlug.get(nameSlugKey);
                }
            }

            if (page != null) {
                return "[" + escapeLabel(label != null ? label : page.getTitle()) + "](/wiki/" + wiki.getId() + "/page/" + page.getId() + ")";
            }

            // "Red link": points at the new-page form with the title (and, if the folder path
            // exists, the folder) pre-filled - creating the missing page is one click away.
            String url = "/wiki/" + wiki.getId() + "/page/new?title=" + urlEncode(namePart);
            Long folderId = folderPart.isEmpty() ? null : folderIdByPath.get(normalizePathKey(folderPart));

            if (folderId != null) {
                url += "&folderId=" + folderId;
            }

            return "[" + escapeLabel(label != null ? label : namePart) + "](" + url + ")";
        }

        private String folderPath(WikiFolder folder, Map<Long, WikiFolder> foldersById)
        {
            StringBuilder path = new StringBuilder(folder.getName());
            WikiFolder current = folder;

            while (current.getParent() != null) {
                current = foldersById.get(current.getParent().getId());

                if (current == null) {
                    break;
                }

                path.insert(0, current.getName() + "/");
            }

            return path.toString();
        }

        private String normalizePathKey(String folderPart)
        {
            StringBuilder key = new StringBuilder();

            for (String segment : folderPart.split("/")) {
                String cleaned = segment.trim();

                if (!cleaned.isEmpty()) {
                    if (key.length() > 0) {
                        key.append("/");
                    }

                    key.append(cleaned);
                }
            }

            return key.toString().toLowerCase();
        }

        private String slugStem(String value)
        {
            String stem = value.toLowerCase();
            return stem.endsWith(".md") ? stem.substring(0, stem.length() - 3) : stem;
        }

        private String escapeLabel(String label)
        {
            return label.replace("[", "\\[").replace("]", "\\]");
        }

        private String urlEncode(String value)
        {
            try {
                return URLEncoder.encode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
