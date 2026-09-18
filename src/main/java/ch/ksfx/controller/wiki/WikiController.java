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

    @GetMapping("/{wikiId}/")
    public String index(@PathVariable Long wikiId, Model model)
    {
        Wiki wiki = requireWiki(wikiId);
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

        baseModel(model, wiki);
        model.addAttribute("currentPageId", pageId);
        model.addAttribute("page", page);
        model.addAttribute("version", wikiPageVersionDAO.getLatestVersionForPage(page.getId()));
        model.addAttribute("attachments", wikiAssetDAO.getAssetsForPage(pageId));

        return "wiki/wiki_page";
    }

    /** Title-only creation form, optionally pre-scoped to a folder (see the sidebar's "New page here"). */
    @GetMapping("/{wikiId}/page/new")
    public String newPage(@PathVariable Long wikiId, @RequestParam(required = false) Long folderId, Model model)
    {
        Wiki wiki = requireWiki(wikiId);
        WikiFolder folder = folderId != null ? requireFolder(wikiId, folderId) : null;

        baseModel(model, wiki);
        model.addAttribute("isNew", true);
        model.addAttribute("pageId", null);
        model.addAttribute("title", "");
        model.addAttribute("content", "");
        model.addAttribute("folderId", folderId);
        model.addAttribute("folderOptions", folderOptions(wikiId));

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

        return "wiki/wiki_edit";
    }

    @PostMapping("/{wikiId}/page/save")
    public String savePage(@PathVariable Long wikiId, @RequestParam(required = false) Long pageId,
                            @RequestParam(required = false) String folderId, @RequestParam String title,
                            @RequestParam(defaultValue = "") String content, RedirectAttributes redirectAttributes)
    {
        Wiki wiki = requireWiki(wikiId);
        WikiFolder folder = parseFolder(wikiId, folderId);

        if (title == null || title.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "A page needs a title.");
            return pageId != null ? "redirect:/wiki/" + wikiId + "/page/" + pageId + "/edit" : "redirect:/wiki/" + wikiId + "/page/new";
        }

        WikiPage page;

        if (pageId != null) {
            page = wikiService.updatePage(requirePage(wikiId, pageId), folder, title.trim(), content, currentUser(), null);
        } else {
            page = wikiService.createPage(wiki, folder, title.trim(), content, currentUser(), null);
        }

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

        for (WikiPage page : pages) {
            WikiTreeNode parentNode = page.getFolder() != null ? nodesByFolderId.get(page.getFolder().getId()) : root;
            parentNode.addPage(page);
        }

        return root;
    }

    public static class WikiTreeNode
    {
        private final Long folderId;
        private final String name;
        private final Map<String, WikiTreeNode> folders = new TreeMap<>();
        private final Map<String, WikiPage> pages = new TreeMap<>();

        public WikiTreeNode(Long folderId, String name)
        {
            this.folderId = folderId;
            this.name = name;
        }

        private void addFolder(WikiTreeNode child)
        {
            folders.put(child.name + "#" + child.folderId, child);
        }

        private void addPage(WikiPage page)
        {
            pages.put(page.getTitle() + "#" + page.getId(), page);
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

        public List<WikiPage> getPages()
        {
            return new ArrayList<>(pages.values());
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
