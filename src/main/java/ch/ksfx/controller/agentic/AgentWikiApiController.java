package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.WikiAssetDAO;
import ch.ksfx.dao.WikiDAO;
import ch.ksfx.dao.WikiFolderDAO;
import ch.ksfx.dao.WikiPageDAO;
import ch.ksfx.dao.WikiPageVersionDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.wiki.Wiki;
import ch.ksfx.model.wiki.WikiAsset;
import ch.ksfx.model.wiki.WikiFolder;
import ch.ksfx.model.wiki.WikiPage;
import ch.ksfx.model.wiki.WikiPageVersion;
import ch.ksfx.services.wiki.WikiService;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lets an agent read/write the wiki using its own bearer token (see Agent.apiToken) instead of a
 * separate externally-issued ApiClient token - every write is automatically attributed to whichever
 * agent's token authenticated it (see WikiPageVersion.editedByAgent), so there is nothing to
 * misconfigure or spoof about "which agent wrote this". No Spring Security here, same
 * inline-bearer-token pattern as AgentMessageApiController/AgentScheduleApiController - see that
 * class's comment for why it's duplicated rather than shared.
 *
 * Pages live in a real folder tree now (see WikiController's class comment), but an agent shouldn't
 * have to look up folder ids first - {@code folderPath} here is a "/"-separated string of folder
 * *names* (e.g. "team/onboarding"), auto-created if missing (see
 * WikiService#resolveOrCreateFolderPath) and matched against for upsert-by-location writes, same
 * convenience the old flat-path API had. {@code pageId} is also accepted everywhere for precise
 * addressing once an agent already knows it (from {@link #list}).
 */
@RestController
@RequestMapping("/agentic/api/wiki")
public class AgentWikiApiController
{
    private final AgentDAO agentDAO;
    private final WikiDAO wikiDAO;
    private final WikiFolderDAO wikiFolderDAO;
    private final WikiPageDAO wikiPageDAO;
    private final WikiPageVersionDAO wikiPageVersionDAO;
    private final WikiAssetDAO wikiAssetDAO;
    private final WikiService wikiService;

    public AgentWikiApiController(AgentDAO agentDAO, WikiDAO wikiDAO, WikiFolderDAO wikiFolderDAO, WikiPageDAO wikiPageDAO,
                                   WikiPageVersionDAO wikiPageVersionDAO, WikiAssetDAO wikiAssetDAO, WikiService wikiService)
    {
        this.agentDAO = agentDAO;
        this.wikiDAO = wikiDAO;
        this.wikiFolderDAO = wikiFolderDAO;
        this.wikiPageDAO = wikiPageDAO;
        this.wikiPageVersionDAO = wikiPageVersionDAO;
        this.wikiAssetDAO = wikiAssetDAO;
        this.wikiService = wikiService;
    }

    @GetMapping("/wikis")
    public ResponseEntity<?> listWikis(HttpServletRequest request)
    {
        if (authenticate(request) == null) {
            return unauthorized();
        }

        List<WikiDto> wikis = new ArrayList<>();

        for (Wiki wiki : wikiDAO.getAllWikis()) {
            WikiDto dto = new WikiDto();
            dto.id = wiki.getId();
            dto.name = wiki.getName();
            wikis.add(dto);
        }

        return ResponseEntity.ok(wikis);
    }

    /** Lets an agent discover what already exists (including each page's folderPath/pageId) before deciding where to add/update a page. */
    @GetMapping("/{wikiId}/pages")
    public ResponseEntity<?> list(HttpServletRequest request, @PathVariable Long wikiId)
    {
        if (authenticate(request) == null) {
            return unauthorized();
        }

        if (wikiDAO.getWikiForId(wikiId) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("No wiki with that id."));
        }

        List<WikiPageDto> pages = new ArrayList<>();

        for (WikiPage page : wikiPageDAO.getAllWikiPages(wikiId)) {
            pages.add(toDto(page));
        }

        return ResponseEntity.ok(pages);
    }

    @GetMapping("/{wikiId}/page/{pageId}")
    public ResponseEntity<?> readById(HttpServletRequest request, @PathVariable Long wikiId, @PathVariable Long pageId)
    {
        if (authenticate(request) == null) {
            return unauthorized();
        }

        WikiPage page = wikiPageDAO.getWikiPageForId(pageId);

        if (page == null || !page.getWiki().getId().equals(wikiId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("No page with that id in this wiki."));
        }

        return ResponseEntity.ok(toDtoWithContent(page));
    }

    @GetMapping("/{wikiId}/page")
    public ResponseEntity<?> readByLocation(HttpServletRequest request, @PathVariable Long wikiId,
                                             @RequestParam(required = false) String folderPath, @RequestParam String title)
    {
        if (authenticate(request) == null) {
            return unauthorized();
        }

        Wiki wiki = wikiDAO.getWikiForId(wikiId);

        if (wiki == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("No wiki with that id."));
        }

        WikiFolder folder = findFolderPath(wiki, folderPath);
        WikiPage page = wikiPageDAO.getPageForFolderAndTitle(wikiId, folder != null ? folder.getId() : null, title);

        if (page == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("No page at that location."));
        }

        return ResponseEntity.ok(toDtoWithContent(page));
    }

    /**
     * Upsert: {@code pageId} set -> update that exact page (title/folder/content). Otherwise matched
     * by (folderPath, title) - updates if a page is already there, creates a new one (with a fresh
     * slug) if not. {@code folderPath} segments that don't exist yet are created automatically.
     *
     * {@code parentTitle} addresses a "subpage" parent by title, WITHIN THE SAME (resolved) folder -
     * a parent can never be in a different folder (see WikiService#validateParentPage), so unlike
     * folderPath this never needs its own folder qualifier. Same null-vs-blank convention as
     * assignee elsewhere in this codebase (e.g. AgentIssueApiController): omitted (null) on an
     * update leaves the current parent untouched; {@code ""} explicitly clears it (makes the page
     * top-level within its folder again); a non-blank value looks the title up in the resolved
     * folder and 400s if nothing matches there. On create, omitted/blank both simply mean "no
     * parent" - there's no existing value to preserve.
     */
    @PostMapping("/{wikiId}/page")
    public ResponseEntity<?> write(HttpServletRequest request, @PathVariable Long wikiId, @RequestBody WikiPageDto body)
    {
        Agent agent = authenticate(request);

        if (agent == null) {
            return unauthorized();
        }

        Wiki wiki = wikiDAO.getWikiForId(wikiId);

        if (wiki == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("No wiki with that id."));
        }

        if (isBlank(body.title)) {
            return ResponseEntity.badRequest().body(errorBody("title is required"));
        }

        WikiFolder folder = wikiService.resolveOrCreateFolderPath(wiki, body.folderPath);
        WikiPage existing = null;

        if (body.pageId != null) {
            existing = wikiPageDAO.getWikiPageForId(body.pageId);

            if (existing == null || !existing.getWiki().getId().equals(wikiId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("No page with that id in this wiki."));
            }
        } else {
            existing = wikiPageDAO.getPageForFolderAndTitle(wikiId, folder != null ? folder.getId() : null, body.title);
        }

        WikiPage parentPage;

        if (body.parentTitle == null) {
            // Not mentioned - leave whatever parent an existing page already has untouched; a new
            // page simply gets none.
            parentPage = existing != null ? existing.getParentPage() : null;
        } else if (isBlank(body.parentTitle)) {
            parentPage = null;
        } else {
            parentPage = wikiPageDAO.getPageForFolderAndTitle(wikiId, folder != null ? folder.getId() : null, body.parentTitle.trim());

            if (parentPage == null) {
                return ResponseEntity.badRequest().body(errorBody("No page titled '" + body.parentTitle + "' in that folder to use as parent."));
            }
        }

        String parentError = wikiService.validateParentPage(folder, existing, parentPage);

        if (parentError != null) {
            return ResponseEntity.badRequest().body(errorBody(parentError));
        }

        WikiPage page = existing != null
                ? wikiService.updatePage(existing, folder, parentPage, body.title, body.content, null, agent)
                : wikiService.createPage(wiki, folder, parentPage, body.title, body.content, null, agent);

        return ResponseEntity.ok(toDto(page));
    }

    /**
     * {@code pageId} absent: a standalone/inline asset (for embedding in markdown by URL).
     * {@code pageId} set: attached to that page's "Attachments" section, same as the web UI's
     * per-page upload.
     */
    @PostMapping("/asset")
    public ResponseEntity<?> uploadAsset(HttpServletRequest request, @RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) Long pageId) throws IOException
    {
        Agent agent = authenticate(request);

        if (agent == null) {
            return unauthorized();
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("file is required"));
        }

        WikiPage page = null;

        if (pageId != null) {
            page = wikiPageDAO.getWikiPageForId(pageId);

            if (page == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("No page with that id."));
            }
        }

        WikiAsset asset = new WikiAsset();
        asset.setWikiPage(page);
        asset.setFileName(file.getOriginalFilename());
        asset.setContentType(file.getContentType());
        asset.setContent(file.getBytes());
        asset.setFileSize(file.getSize());
        asset.setCreatedAt(new Date());
        asset.setUploadedByAgent(agent);
        wikiAssetDAO.saveWikiAsset(asset);

        Map<String, Object> response = new HashMap<>();
        response.put("assetId", asset.getId());
        response.put("url", "/wiki/assets/" + asset.getId());

        return ResponseEntity.ok(response);
    }

    /** Read-only counterpart of WikiService#resolveOrCreateFolderPath - a lookup miss just means "no page there", not an auto-create. */
    private WikiFolder findFolderPath(Wiki wiki, String folderPath)
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

            parent = wikiFolderDAO.getFolderByParentAndName(wiki.getId(), parent != null ? parent.getId() : null, name);

            if (parent == null) {
                return null;
            }
        }

        return parent;
    }

    private String folderPathOf(WikiFolder folder)
    {
        if (folder == null) {
            return null;
        }

        Deque<String> segments = new ArrayDeque<>();
        WikiFolder current = folder;

        while (current != null) {
            segments.addFirst(current.getName());
            current = current.getParent();
        }

        return String.join("/", segments);
    }

    private WikiPageDto toDto(WikiPage page)
    {
        WikiPageDto dto = new WikiPageDto();
        dto.pageId = page.getId();
        dto.folderPath = folderPathOf(page.getFolder());
        dto.title = page.getTitle();
        dto.parentTitle = page.getParentPage() != null ? page.getParentPage().getTitle() : null;
        return dto;
    }

    private WikiPageDto toDtoWithContent(WikiPage page)
    {
        WikiPageDto dto = toDto(page);
        WikiPageVersion latest = wikiPageVersionDAO.getLatestVersionForPage(page.getId());
        dto.content = latest != null ? latest.getContent() : "";
        return dto;
    }

    private Agent authenticate(HttpServletRequest request)
    {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }

        String token = header.substring("Bearer ".length()).trim();

        if (token.isEmpty()) {
            return null;
        }

        Agent agent = agentDAO.getAgentForApiToken(token);

        return agent != null && agent.getEnabled() ? agent : null;
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    private ResponseEntity<?> unauthorized()
    {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Unauthorized"));
    }

    private Map<String, String> errorBody(String message)
    {
        return Collections.singletonMap("error", message);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WikiDto
    {
        public Long id;
        public String name;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WikiPageDto
    {
        public Long pageId;
        public String folderPath;
        public String title;
        public String parentTitle;
        public String content;
    }
}
