package ch.ksfx.controller.wiki;

import ch.ksfx.dao.WikiAssetDAO;
import ch.ksfx.dao.WikiDAO;
import ch.ksfx.dao.WikiFolderDAO;
import ch.ksfx.dao.WikiPageDAO;
import ch.ksfx.dao.WikiPageVersionDAO;
import ch.ksfx.model.user.User;
import ch.ksfx.model.wiki.Wiki;
import ch.ksfx.model.wiki.WikiAsset;
import ch.ksfx.model.wiki.WikiFolder;
import ch.ksfx.model.wiki.WikiPage;
import ch.ksfx.model.wiki.WikiPageVersion;
import ch.ksfx.services.wiki.WikiService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;

/**
 * The in-house wiki (see the design discussion this replaces GitHub-per-team wikis with): Markdown
 * pages organized in a real, manageable {@link WikiFolder} tree (create/rename/delete a folder,
 * move a page between folders - all from the sidebar/editor, see the folder-action endpoints below
 * and wiki_tree.html), edited with the same Toast UI Editor already vendored for the Agentic file
 * browser, with images stored as {@link WikiAsset} DB rows rather than on disk (one DB backup covers
 * pages, versions, and images alike). Every save inserts a new {@link WikiPageVersion} rather than
 * overwriting content in place, so the edit history is just "every version for this page" - see that
 * class's own comment.
 *
 * Pages can additionally nest under one another ({@link WikiPage#getParentPage()}, "subpages") -
 * deliberately NOT a second hierarchy alongside folders: a page's parent must always live in the
 * same folder as the page (see {@link ch.ksfx.services.wiki.WikiService#validateParentPage}), so
 * folders stay the one real containment structure and the parent-page chain is just a
 * finer-grained ordering within a single folder's pages (see {@link #buildTree}/{@link
 * #pagesInFolder}).
 *
 * A KSFX instance can host several independent {@link Wiki}s (e.g. one per project) - every route
 * except asset up/download is scoped under {@code /wiki/{wikiId}/...}, and every page's model gets
 * {@code allWikis}/{@code currentWiki} (see {@link #baseModel}) so the sidebar can render the
 * switcher dropdown. Session-authenticated only (no permitAll here) - the parallel agent-facing
 * write path is AgentWikiApiController, using an agent's own bearer token instead of a browser
 * session.
 */
@Controller
@RequestMapping("/wiki")
public class WikiController
{
    private final WikiDAO wikiDAO;
    private final WikiFolderDAO wikiFolderDAO;
    private final WikiPageDAO wikiPageDAO;
    private final WikiPageVersionDAO wikiPageVersionDAO;
    private final WikiAssetDAO wikiAssetDAO;
    private final WikiService wikiService;

    public WikiController(WikiDAO wikiDAO, WikiFolderDAO wikiFolderDAO, WikiPageDAO wikiPageDAO,
                           WikiPageVersionDAO wikiPageVersionDAO, WikiAssetDAO wikiAssetDAO, WikiService wikiService)
    {
        this.wikiDAO = wikiDAO;
        this.wikiFolderDAO = wikiFolderDAO;
        this.wikiPageDAO = wikiPageDAO;
        this.wikiPageVersionDAO = wikiPageVersionDAO;
        this.wikiAssetDAO = wikiAssetDAO;
        this.wikiService = wikiService;
    }

    /** No wiki selected yet - go to the first one, or prompt to create one if there aren't any. */
    @GetMapping({"/", ""})
    public String root()
    {
        List<Wiki> wikis = wikiDAO.getAllWikis();
        return wikis.isEmpty() ? "redirect:/wiki/none" : "redirect:/wiki/" + wikis.get(0).getId() + "/";
    }

    @GetMapping("/none")
    public String none(Model model)
    {
        model.addAttribute("allWikis", Collections.emptyList());
        return "wiki/wiki_none";
    }

    @PostMapping("/create")
    public String create(@RequestParam String name, RedirectAttributes redirectAttributes)
    {
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "A wiki needs a name.");
            return "redirect:/wiki/none";
        }

        Wiki wiki = new Wiki();
        wiki.setName(name.trim());
        wiki.setCreatedAt(new Date());
        wikiDAO.saveOrUpdateWiki(wiki);

        return "redirect:/wiki/" + wiki.getId() + "/";
    }

    /**
     * Cascades through folders/pages/versions/attachments via the DB FKs - wiki.js guards this
     * behind a type-the-name confirmation. No flash message: /wiki/ immediately redirects again (to
     * the next wiki or /wiki/none), and flash attributes don't survive a redirect they weren't
     * re-added for (see AgentController.delete's comment on the same effect) - the wiki vanishing
     * from the switcher is feedback enough.
     */
    @PostMapping("/{wikiId}/delete")
    public String deleteWiki(@PathVariable Long wikiId)
    {
        wikiDAO.deleteWiki(requireWiki(wikiId));

        return "redirect:/wiki/";
    }

    /**
     * A top-level page with slug "home.md" is the wiki's home page by convention (same convention
     * as GitHub wikis - no schema/config needed, and the empty-wiki "Create the first page" flow
     * nudges toward it by pre-filling the title "Home") - landing on the wiki goes straight there.
     * Without one, the pick-a-page index renders as before.
     */
    @GetMapping("/{wikiId}/")
    public String index(@PathVariable Long wikiId, Model model)
    {
        Wiki wiki = requireWiki(wikiId);
        WikiPage homePage = wikiPageDAO.getPageForFolderAndSlug(wikiId, null, "home.md");

        if (homePage != null) {
            return "redirect:/wiki/" + wikiId + "/page/" + homePage.getId();
        }

        List<WikiPage> allPages = wikiPageDAO.getAllWikiPages(wikiId);

        baseModel(model, wiki);
        model.addAttribute("pageCount", allPages.size());

        return "wiki/wiki_index";
    }

    @GetMapping("/{wikiId}/page/{pageId}")
    public String viewPage(@PathVariable Long wikiId, @PathVariable Long pageId, Model model)
    {
        Wiki wiki = requireWiki(wikiId);
        WikiPage page = requirePage(wikiId, pageId);

        WikiPageVersion version = wikiPageVersionDAO.getLatestVersionForPage(page.getId());

        baseModel(model, wiki);
        model.addAttribute("currentPageId", pageId);
        model.addAttribute("page", page);
        model.addAttribute("version", version);
        // What the viewer actually renders: [[wikilinks]] resolved to real links (see
        // WikiService.renderWikilinks) - the stored markdown itself keeps the [[...]] form, which
        // is why the edit view keeps using the raw version content.
        model.addAttribute("renderedContent", version != null ? wikiService.renderWikilinks(wiki, version.getContent()) : "");
        model.addAttribute("attachments", wikiAssetDAO.getAssetsForPage(pageId));
        // Root-first ancestor chain for the breadcrumb - always same-folder by construction (see
        // WikiService#validateParentPage), so unlike the folder path this never needs to be
        // reconciled against anything else.
        model.addAttribute("ancestors", ancestorChain(page));
        model.addAttribute("childPages", wikiPageDAO.getChildPages(pageId));

        Set<Long> descendantIds = new HashSet<>();
        collectDescendantIds(pageId, descendantIds);
        model.addAttribute("descendantCount", descendantIds.size());

        return "wiki/wiki_page";
    }

    /**
     * Title-only creation form, optionally pre-scoped to a folder (see the sidebar's "New page
     * here") or to a parent page (see the tree's per-page "New subpage" action) - the latter
     * derives its folder from the parent instead of taking one independently, since a subpage must
     * live in the same folder as its parent anyway (see WikiService#validateParentPage). {@code
     * title} pre-fills the title field - currently only used by the empty-wiki "Create the first
     * page" button to suggest "Home", nudging toward the home-page convention (see {@link #index}).
     */
    @GetMapping("/{wikiId}/page/new")
    public String newPage(@PathVariable Long wikiId, @RequestParam(required = false) Long folderId,
                           @RequestParam(required = false) Long parentPageId,
                           @RequestParam(defaultValue = "") String title, Model model)
    {
        Wiki wiki = requireWiki(wikiId);
        WikiPage parentPage = parentPageId != null ? requirePage(wikiId, parentPageId) : null;
        WikiFolder folder = parentPage != null ? parentPage.getFolder() : (folderId != null ? requireFolder(wikiId, folderId) : null);

        baseModel(model, wiki);
        model.addAttribute("isNew", true);
        model.addAttribute("pageId", null);
        model.addAttribute("title", title);
        model.addAttribute("content", "");
        model.addAttribute("folderId", folder != null ? folder.getId() : null);
        model.addAttribute("folderOptions", folderOptions(wikiId));
        model.addAttribute("parentPageId", parentPage != null ? parentPage.getId() : null);
        model.addAttribute("parentPageOptions", pagesInFolder(wikiId, folder, null));

        return "wiki/wiki_edit";
    }

    @GetMapping("/{wikiId}/page/{pageId}/edit")
    public String editPage(@PathVariable Long wikiId, @PathVariable Long pageId, Model model)
    {
        Wiki wiki = requireWiki(wikiId);
        WikiPage page = requirePage(wikiId, pageId);
        WikiPageVersion latest = wikiPageVersionDAO.getLatestVersionForPage(page.getId());

        baseModel(model, wiki);
        model.addAttribute("currentPageId", pageId);
        model.addAttribute("isNew", false);
        model.addAttribute("pageId", pageId);
        model.addAttribute("title", page.getTitle());
        model.addAttribute("content", latest != null ? latest.getContent() : "");
        model.addAttribute("folderId", page.getFolder() != null ? page.getFolder().getId() : null);
        model.addAttribute("folderOptions", folderOptions(wikiId));
        model.addAttribute("parentPageId", page.getParentPage() != null ? page.getParentPage().getId() : null);
        // Options are for the page's CURRENT folder - if "Move to another folder" is then actually
        // used, any previously-selected parent gets caught by validateParentPage's same-folder
        // check on save (a plain error redirect, same as every other validation failure here)
        // rather than needing a second, JS-repopulated select for the rare move+reparent case.
        model.addAttribute("parentPageOptions", pagesInFolder(wikiId, page.getFolder(), page));

        return "wiki/wiki_edit";
    }

    @PostMapping("/{wikiId}/page/save")
    public String savePage(@PathVariable Long wikiId, @RequestParam(required = false) Long pageId,
                            @RequestParam(required = false) String folderId, @RequestParam(required = false) String parentPageId,
                            @RequestParam String title, @RequestParam(defaultValue = "") String content,
                            RedirectAttributes redirectAttributes)
    {
        Wiki wiki = requireWiki(wikiId);
        WikiFolder folder = parseFolder(wikiId, folderId);
        WikiPage existing = pageId != null ? requirePage(wikiId, pageId) : null;

        if (title == null || title.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "A page needs a title.");
            return pageId != null ? "redirect:/wiki/" + wikiId + "/page/" + pageId + "/edit" : "redirect:/wiki/" + wikiId + "/page/new";
        }

        WikiPage parentPage = (parentPageId == null || parentPageId.trim().isEmpty()) ? null : requirePage(wikiId, Long.valueOf(parentPageId.trim()));
        String parentError = wikiService.validateParentPage(folder, existing, parentPage);

        if (parentError != null) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", parentError);
            return pageId != null ? "redirect:/wiki/" + wikiId + "/page/" + pageId + "/edit" : "redirect:/wiki/" + wikiId + "/page/new";
        }

        WikiPage page = existing != null
                ? wikiService.updatePage(existing, folder, parentPage, title.trim(), content, currentUser(), null)
                : wikiService.createPage(wiki, folder, parentPage, title.trim(), content, currentUser(), null);

        return "redirect:/wiki/" + wikiId + "/page/" + page.getId();
    }

    @GetMapping("/{wikiId}/page/{pageId}/history")
    public String history(@PathVariable Long wikiId, @PathVariable Long pageId, Model model)
    {
        Wiki wiki = requireWiki(wikiId);
        WikiPage page = requirePage(wikiId, pageId);

        baseModel(model, wiki);
        model.addAttribute("currentPageId", pageId);
        model.addAttribute("page", page);
        model.addAttribute("versions", wikiPageVersionDAO.getVersionsForPage(page.getId()));

        return "wiki/wiki_history";
    }

    /**
     * Copies an old version's content into a brand new version row rather than reverting the page
     * in place - so "restore" shows up as its own entry in the history instead of erasing what it's
     * undoing, same append-only reasoning as WikiPageVersion's own comment.
     */
    @PostMapping("/{wikiId}/page/{pageId}/restore")
    public String restore(@PathVariable Long wikiId, @PathVariable Long pageId, @RequestParam Long versionId, RedirectAttributes redirectAttributes)
    {
        WikiPage page = requirePage(wikiId, pageId);
        WikiPageVersion old = wikiPageVersionDAO.getWikiPageVersionForId(versionId);

        if (old == null || !old.getWikiPage().getId().equals(pageId)) {
            return "redirect:/wiki/" + wikiId + "/";
        }

        Date now = new Date();
        WikiPageVersion restored = new WikiPageVersion();
        restored.setWikiPage(page);
        restored.setContent(old.getContent());
        restored.setCreatedAt(now);
        restored.setEditedByUser(currentUser());
        wikiPageVersionDAO.saveWikiPageVersion(restored);

        page.setUpdatedAt(now);
        wikiPageDAO.saveOrUpdateWikiPage(page);

        redirectAttributes.addFlashAttribute("resultMessage", "Restored an earlier version.");
        return "redirect:/wiki/" + wikiId + "/page/" + pageId;
    }

    @PostMapping("/{wikiId}/page/{pageId}/delete")
    public String delete(@PathVariable Long wikiId, @PathVariable Long pageId, RedirectAttributes redirectAttributes)
    {
        wikiPageDAO.deleteWikiPage(requirePage(wikiId, pageId));

        redirectAttributes.addFlashAttribute("resultMessage", "Page deleted.");
        return "redirect:/wiki/" + wikiId + "/";
    }

    @PostMapping("/{wikiId}/folder/create")
    public String createFolder(@PathVariable Long wikiId, @RequestParam(required = false) String parentFolderId,
                                @RequestParam String name, RedirectAttributes redirectAttributes)
    {
        Wiki wiki = requireWiki(wikiId);

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "A folder needs a name.");
            return "redirect:/wiki/" + wikiId + "/";
        }

        WikiFolder folder = new WikiFolder();
        folder.setWiki(wiki);
        folder.setParent(parseFolder(wikiId, parentFolderId));
        folder.setName(name.trim());
        folder.setCreatedAt(new Date());
        wikiFolderDAO.saveOrUpdateWikiFolder(folder);

        return "redirect:/wiki/" + wikiId + "/";
    }

    @PostMapping("/{wikiId}/folder/{folderId}/rename")
    public String renameFolder(@PathVariable Long wikiId, @PathVariable Long folderId, @RequestParam String name,
                                RedirectAttributes redirectAttributes)
    {
        WikiFolder folder = requireFolder(wikiId, folderId);

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "A folder needs a name.");
            return "redirect:/wiki/" + wikiId + "/";
        }

        folder.setName(name.trim());
        wikiFolderDAO.saveOrUpdateWikiFolder(folder);

        return "redirect:/wiki/" + wikiId + "/";
    }

    /** Cascades to every subfolder and page underneath (see the DB FK definitions) - the client-side confirm() warns about this. */
    @PostMapping("/{wikiId}/folder/{folderId}/delete")
    public String deleteFolder(@PathVariable Long wikiId, @PathVariable Long folderId, RedirectAttributes redirectAttributes)
    {
        wikiFolderDAO.deleteWikiFolder(requireFolder(wikiId, folderId));

        redirectAttributes.addFlashAttribute("resultMessage", "Folder deleted.");
        return "redirect:/wiki/" + wikiId + "/";
    }

    /**
     * Called by the Toast UI Editor's image-upload hook (see wiki.js) while editing a page - not
     * tied to any WikiPage or Wiki at upload time (the page itself might still be unsaved), so the
     * asset is a standalone row referenced only by the URL embedded into the markdown, same as
     * pasting an external image URL would be.
     */
    @PostMapping("/asset/upload")
    @ResponseBody
    public Map<String, Object> uploadAsset(@RequestParam("file") MultipartFile file) throws IOException
    {
        WikiAsset asset = buildAsset(file, null);
        wikiAssetDAO.saveWikiAsset(asset);

        Map<String, Object> response = new HashMap<>();
        response.put("url", "/wiki/assets/" + asset.getId());
        return response;
    }

    /** The page's "Attachments" section (bottom of the page view) - unlike inline images, these ARE tied to the page. */
    @PostMapping("/{wikiId}/page/{pageId}/attachment/upload")
    public String uploadAttachment(@PathVariable Long wikiId, @PathVariable Long pageId,
                                    @RequestParam("files") MultipartFile[] files, RedirectAttributes redirectAttributes) throws IOException
    {
        WikiPage page = requirePage(wikiId, pageId);

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            wikiAssetDAO.saveWikiAsset(buildAsset(file, page));
        }

        return "redirect:/wiki/" + wikiId + "/page/" + pageId;
    }

    @PostMapping("/{wikiId}/page/{pageId}/attachment/{assetId}/delete")
    public String deleteAttachment(@PathVariable Long wikiId, @PathVariable Long pageId, @PathVariable Long assetId,
                                    RedirectAttributes redirectAttributes)
    {
        requirePage(wikiId, pageId);
        WikiAsset asset = wikiAssetDAO.getWikiAssetForId(assetId);

        // The page check above plus this ownership check keep a forged URL from deleting another
        // page's (or an inline) asset.
        if (asset != null && asset.getWikiPage() != null && asset.getWikiPage().getId().equals(pageId)) {
            wikiAssetDAO.deleteWikiAsset(asset);
            redirectAttributes.addFlashAttribute("resultMessage", "Attachment deleted.");
        }

        return "redirect:/wiki/" + wikiId + "/page/" + pageId;
    }

    private WikiAsset buildAsset(MultipartFile file, WikiPage page) throws IOException
    {
        WikiAsset asset = new WikiAsset();
        asset.setWikiPage(page);
        asset.setFileName(file.getOriginalFilename());
        asset.setContentType(file.getContentType());
        asset.setContent(file.getBytes());
        asset.setFileSize(file.getSize());
        asset.setCreatedAt(new Date());
        asset.setUploadedByUser(currentUser());
        return asset;
    }

    @GetMapping("/assets/{id}")
    public ResponseEntity<Resource> asset(@PathVariable Long id)
    {
        WikiAsset asset = wikiAssetDAO.getWikiAssetForId(id);

        if (asset == null || asset.getContent() == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = asset.getContentType() != null
                ? MediaType.parseMediaType(asset.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + asset.getFileName() + "\"")
                .body(new ByteArrayResource(asset.getContent()));
    }

    /** Every wiki-scoped page needs the switcher dropdown's data and the folder/page tree - set once here instead of in every handler. */
    private void baseModel(Model model, Wiki wiki)
    {
        model.addAttribute("allWikis", wikiDAO.getAllWikis());
        model.addAttribute("currentWiki", wiki);
        model.addAttribute("tree", buildTree(wikiFolderDAO.getFoldersForWiki(wiki.getId()), wikiPageDAO.getAllWikiPages(wiki.getId())));
    }

    /** Flattened, indented (via {@link FolderOption#getDepth()}) list for the "move to folder" dropdown in the editor. */
    private List<FolderOption> folderOptions(Long wikiId)
    {
        WikiTreeNode root = buildTree(wikiFolderDAO.getFoldersForWiki(wikiId), Collections.emptyList());
        List<FolderOption> options = new ArrayList<>();
        collectFolderOptions(root, 0, options);
        return options;
    }

    private void collectFolderOptions(WikiTreeNode node, int depth, List<FolderOption> out)
    {
        for (WikiTreeNode child : node.getFolders()) {
            out.add(new FolderOption(child.getFolderId(), child.getName(), depth));
            collectFolderOptions(child, depth + 1, out);
        }
    }

    private Wiki requireWiki(Long wikiId)
    {
        Wiki wiki = wikiDAO.getWikiForId(wikiId);

        if (wiki == null) {
            throw new IllegalArgumentException("Wiki not found");
        }

        return wiki;
    }

    /**
     * "" (an empty &lt;select&gt; value, e.g. the "(top level)" option, or an omitted form field) means
     * no folder - handled here rather than via {@code @RequestParam Long}, whose default String-to-Long
     * conversion rejects an empty string instead of treating it as null.
     */
    private WikiFolder parseFolder(Long wikiId, String folderIdParam)
    {
        if (folderIdParam == null || folderIdParam.trim().isEmpty()) {
            return null;
        }

        return requireFolder(wikiId, Long.valueOf(folderIdParam.trim()));
    }

    private WikiFolder requireFolder(Long wikiId, Long folderId)
    {
        WikiFolder folder = wikiFolderDAO.getWikiFolderForId(folderId);

        if (folder == null || !folder.getWiki().getId().equals(wikiId)) {
            throw new IllegalArgumentException("Folder not found");
        }

        return folder;
    }

    private WikiPage requirePage(Long wikiId, Long pageId)
    {
        WikiPage page = wikiPageDAO.getWikiPageForId(pageId);

        if (page == null || !page.getWiki().getId().equals(wikiId)) {
            throw new IllegalArgumentException("Page not found");
        }

        return page;
    }

    /** Root-first: wiki root's direct children first, {@code page} itself last - what the breadcrumb renders in order. */
    private List<WikiPage> ancestorChain(WikiPage page)
    {
        LinkedList<WikiPage> chain = new LinkedList<>();
        WikiPage cursor = page.getParentPage();

        while (cursor != null) {
            chain.addFirst(cursor);
            cursor = cursor.getParentPage();
        }

        return chain;
    }

    /**
     * Candidate parent-page options for the editor's "Parent page" select, scoped to one folder
     * (the only folder a valid parent could ever be in - see WikiService#validateParentPage).
     * {@code excludePage} is null when creating a brand new page (nothing to exclude yet) and the
     * page being edited otherwise - excludes it and every one of its own descendants, so the select
     * never even offers a choice {@code validateParentPage} would reject as a cycle.
     */
    private List<WikiPage> pagesInFolder(Long wikiId, WikiFolder folder, WikiPage excludePage)
    {
        Set<Long> excludedIds = new HashSet<>();

        if (excludePage != null) {
            excludedIds.add(excludePage.getId());
            collectDescendantIds(excludePage.getId(), excludedIds);
        }

        Long folderId = folder != null ? folder.getId() : null;
        List<WikiPage> result = new ArrayList<>();

        for (WikiPage page : wikiPageDAO.getAllWikiPages(wikiId)) {
            Long pageFolderId = page.getFolder() != null ? page.getFolder().getId() : null;

            if (Objects.equals(pageFolderId, folderId) && !excludedIds.contains(page.getId())) {
                result.add(page);
            }
        }

        return result;
    }

    private void collectDescendantIds(Long pageId, Set<Long> out)
    {
        for (WikiPage child : wikiPageDAO.getChildPages(pageId)) {
            if (out.add(child.getId())) {
                collectDescendantIds(child.getId(), out);
            }
        }
    }

    private User currentUser()
    {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;

        return principal instanceof User ? (User) principal : null;
    }

    /**
     * Groups the flat folder + page lists into a real tree - unlike the first cut of this feature,
     * folders are real rows (see WikiFolder), not derived from path strings, so an empty folder
     * still renders. Children are kept in TreeMaps so folders/pages at each level render
     * alphabetically without any sorting in the template.
     *
     * Pages nest under {@link WikiPage#getParentPage()} within a folder rather than being a flat
     * list - a page with no parent attaches directly to its folder's node (as before this feature);
     * a page WITH a parent attaches under that parent's own {@link WikiPageNode} instead, which is
     * only reachable because a parent is always in the same folder as its children (see
     * WikiService#validateParentPage) - the folder's page map therefore only ever needs to hold the
     * folder's OWN top-level pages, every deeper level lives inside a WikiPageNode's own children.
     * A parent pointing outside this page's own folder set (shouldn't happen, guarded against at
     * write time) falls back to top-level rather than being dropped, so a stray page never just
     * disappears from the tree.
     */
    private WikiTreeNode buildTree(List<WikiFolder> folders, List<WikiPage> pages)
    {
        WikiTreeNode root = new WikiTreeNode(null, "");
        Map<Long, WikiTreeNode> nodesByFolderId = new HashMap<>();

        for (WikiFolder folder : folders) {
            nodesByFolderId.put(folder.getId(), new WikiTreeNode(folder.getId(), folder.getName()));
        }

        for (WikiFolder folder : folders) {
            WikiTreeNode node = nodesByFolderId.get(folder.getId());
            WikiTreeNode parentNode = folder.getParent() != null ? nodesByFolderId.get(folder.getParent().getId()) : root;
            parentNode.addFolder(node);
        }

        Map<Long, WikiPageNode> nodesByPageId = new HashMap<>();

        for (WikiPage page : pages) {
            nodesByPageId.put(page.getId(), new WikiPageNode(page));
        }

        for (WikiPage page : pages) {
            WikiPageNode node = nodesByPageId.get(page.getId());
            WikiPageNode parentPageNode = page.getParentPage() != null ? nodesByPageId.get(page.getParentPage().getId()) : null;

            if (parentPageNode != null) {
                parentPageNode.addChild(node);
            } else {
                WikiTreeNode folderNode = page.getFolder() != null ? nodesByFolderId.get(page.getFolder().getId()) : root;
                folderNode.addPage(node);
            }
        }

        return root;
    }

    public static class WikiTreeNode
    {
        private final Long folderId;
        private final String name;
        private final Map<String, WikiTreeNode> folders = new TreeMap<>();
        private final Map<String, WikiPageNode> pages = new TreeMap<>();

        public WikiTreeNode(Long folderId, String name)
        {
            this.folderId = folderId;
            this.name = name;
        }

        private void addFolder(WikiTreeNode child)
        {
            folders.put(child.name + "#" + child.folderId, child);
        }

        private void addPage(WikiPageNode page)
        {
            pages.put(page.getPage().getTitle() + "#" + page.getPage().getId(), page);
        }

        public Long getFolderId()
        {
            return folderId;
        }

        public String getName()
        {
            return name;
        }

        public List<WikiTreeNode> getFolders()
        {
            return new ArrayList<>(folders.values());
        }

        public List<WikiPageNode> getPages()
        {
            return new ArrayList<>(pages.values());
        }
    }

    /** One page plus its own subpages (recursive) - see {@link #buildTree}'s comment on how nesting is decided. */
    public static class WikiPageNode
    {
        private final WikiPage page;
        private final Map<String, WikiPageNode> children = new TreeMap<>();

        public WikiPageNode(WikiPage page)
        {
            this.page = page;
        }

        private void addChild(WikiPageNode child)
        {
            children.put(child.page.getTitle() + "#" + child.page.getId(), child);
        }

        public WikiPage getPage()
        {
            return page;
        }

        public List<WikiPageNode> getChildren()
        {
            return new ArrayList<>(children.values());
        }
    }

    public static class FolderOption
    {
        private final Long id;
        private final String name;
        private final int depth;

        public FolderOption(Long id, String name, int depth)
        {
            this.id = id;
            this.name = name;
            this.depth = depth;
        }

        public Long getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }

        public int getDepth()
        {
            return depth;
        }
    }
}
