package ch.ksfx.controller.wiki;

import ch.ksfx.dao.WikiPageDAO;
import ch.ksfx.model.user.User;
import ch.ksfx.model.wiki.WikiPage;
import ch.ksfx.services.project.UserScope;
import ch.ksfx.services.wiki.WikiCollabService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The live editing session of a wiki page (see WikiCollabService) - JSON endpoints for wiki-collab.js.
 * Only for people who may write the page's project; agents never come here (they use their own wiki
 * API, which WikiCollabService hooks into).
 */
@RestController
@RequestMapping("/wiki/{wikiId}/page/{pageId}/collab")
public class WikiCollabController
{
    private final WikiPageDAO wikiPageDAO;
    private final WikiCollabService wikiCollabService;
    private final UserScope userScope;

    public WikiCollabController(WikiPageDAO wikiPageDAO, WikiCollabService wikiCollabService, UserScope userScope)
    {
        this.wikiPageDAO = wikiPageDAO;
        this.wikiCollabService = wikiCollabService;
        this.userScope = userScope;
    }

    @PostMapping("/join")
    public Map<String, Object> join(@PathVariable Long wikiId, @PathVariable Long pageId, @RequestBody ClientBody body)
    {
        return wikiCollabService.join(requirePage(wikiId, pageId), body.clientId, currentUser());
    }

    @PostMapping("/push")
    public ResponseEntity<?> push(@PathVariable Long wikiId, @PathVariable Long pageId, @RequestBody PushBody body)
    {
        requirePage(wikiId, pageId);
        Map<String, Object> answer = wikiCollabService.push(pageId, body.clientId, currentUser(), body.seen, body.updates, body.awareness, body.markdown);

        return answer != null ? ResponseEntity.ok(answer) : ResponseEntity.ok(Collections.singletonMap("ended", true));
    }

    @GetMapping("/poll")
    public DeferredResult<Map<String, Object>> poll(@PathVariable Long wikiId, @PathVariable Long pageId, @RequestParam long clientId,
                                                    @RequestParam(defaultValue = "0") int since, @RequestParam(defaultValue = "0") long awarenessSince,
                                                    @RequestParam(defaultValue = "0") long externalSince)
    {
        requirePage(wikiId, pageId);

        return wikiCollabService.poll(pageId, clientId, since, awarenessSince, externalSince);
    }

    /** Called with navigator.sendBeacon on unload - form-encoded, CSRF token as a field. */
    @PostMapping("/leave")
    public ResponseEntity<?> leave(@PathVariable Long wikiId, @PathVariable Long pageId, @RequestParam long clientId)
    {
        requirePage(wikiId, pageId);
        wikiCollabService.leave(pageId, clientId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/external-applied")
    public Map<String, Object> externalApplied(@PathVariable Long wikiId, @PathVariable Long pageId, @RequestBody ExternalBody body)
    {
        requirePage(wikiId, pageId);

        return Collections.singletonMap("ok", wikiCollabService.externalApplied(pageId, body.clientId, body.externalId, body.markdown));
    }

    private WikiPage requirePage(Long wikiId, Long pageId)
    {
        WikiPage page = wikiPageDAO.getWikiPageForId(pageId);

        if (page == null || !page.getWiki().getId().equals(wikiId)) {
            throw new IllegalArgumentException("Page not found");
        }

        userScope.requireWrite(page.getWiki().getProject()); // editing - also for the GET poll

        return page;
    }

    private User currentUser()
    {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;

        return principal instanceof User ? (User) principal : null;
    }

    public static class ClientBody
    {
        public long clientId;
    }

    public static class PushBody
    {
        public long clientId;
        /** How many session updates the client had applied when it sent this. */
        public int seen;
        public List<String> updates;
        public String awareness;
        public String markdown;
    }

    public static class ExternalBody
    {
        public long clientId;
        public long externalId;
        public String markdown;
    }
}
