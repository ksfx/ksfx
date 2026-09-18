package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentMessageDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgentMessage;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-agent chat search, opened from the sidebar (not a per-agent toolbar button like Files/
 * Schedules - a search naturally isn't scoped to whichever agent you happen to have open). Same
 * fragment-swap overlay pattern as those two, and deliberately a new, isolated controller for the
 * same reason: this is new surface, nothing existing changes behavior.
 *
 * The context view (10 messages before/after a match) reuses agent_chat.html's own messagesList
 * fragment directly, rather than a new template - that fragment was already written to have no
 * fixed parameter list specifically so a second/third caller could reuse it unchanged (see its own
 * comment; the chat's own "load older messages" pagination is the second caller, this is the
 * third), so a search result renders as an identical message bubble to the real chat, for free.
 */
@Controller
@RequestMapping("/agentic/search")
public class AgenticSearchController
{
    private static final int RESULT_LIMIT = 30;
    private static final int CONTEXT_WINDOW = 10;
    private static final int SNIPPET_RADIUS = 60;

    private final AgentMessageDAO agentMessageDAO;

    public AgenticSearchController(AgentMessageDAO agentMessageDAO)
    {
        this.agentMessageDAO = agentMessageDAO;
    }

    @GetMapping("/query")
    public String query(@RequestParam(defaultValue = "") String q, Model model)
    {
        String term = q.trim();
        List<AgentMessage> matches = term.isEmpty() ? new ArrayList<>() : agentMessageDAO.searchMessages(term, RESULT_LIMIT);

        List<SearchResult> results = new ArrayList<>();

        for (AgentMessage message : matches) {
            results.add(toSearchResult(message, term));
        }

        model.addAttribute("query", term);
        model.addAttribute("results", results);
        model.addAttribute("resultLimit", RESULT_LIMIT);

        return "agentic/search/agentic_search_overlay :: searchResults";
    }

    /**
     * Renders the 10-before / match / 10-after window for one search result via the real chat
     * page's own messagesList fragment - see the class comment. The agent that owns the matched
     * message is resolved from the message row itself, not a path/query parameter, which is what
     * lets this stay agent-agnostic despite the search itself being cross-agent.
     */
    @GetMapping("/context")
    public String context(@RequestParam Long messageId, Model model)
    {
        AgentMessage matched = agentMessageDAO.getAgentMessageForId(messageId);

        if (matched == null || matched.getAgent() == null) {
            throw new IllegalArgumentException("Message not found");
        }

        Agent agent = matched.getAgent();
        Long agentId = agent.getId();

        List<AgentMessage> before = agentMessageDAO.getMessagesForAgentBefore(agentId, messageId, CONTEXT_WINDOW);
        List<AgentMessage> after = agentMessageDAO.getMessagesForAgentAfter(agentId, messageId, CONTEXT_WINDOW);

        List<AgentMessage> window = new ArrayList<>(before);
        window.add(matched);
        window.addAll(after);

        model.addAttribute("agent", agent);
        model.addAttribute("messages", window);

        return "agentic/agent/agent_chat :: messagesList";
    }

    private SearchResult toSearchResult(AgentMessage message, String term)
    {
        SearchResult result = new SearchResult();
        result.messageId = message.getId();
        result.agentId = message.getAgent() != null ? message.getAgent().getId() : null;
        result.agentName = message.getAgent() != null ? message.getAgent().getName() : "?";
        result.role = message.getRole() != null ? message.getRole().name() : "";
        result.createdAt = message.getCreatedAt();
        result.snippetHtml = buildSnippetHtml(message.getContent(), term);

        return result;
    }

    /**
     * A short, HTML-safe excerpt centered on the first match of {@code term} in {@code content},
     * with the match itself wrapped in &lt;mark&gt;. Everything except the &lt;mark&gt; tags
     * themselves is HtmlUtils-escaped, so this is safe to render with th:utext despite content
     * being arbitrary user/agent text.
     */
    private String buildSnippetHtml(String content, String term)
    {
        if (content == null) {
            return "";
        }

        int matchIndex = term.isEmpty() ? -1 : content.toLowerCase().indexOf(term.toLowerCase());

        if (matchIndex < 0) {
            String plain = content.length() > 160 ? content.substring(0, 160) + "…" : content;
            return HtmlUtils.htmlEscape(plain);
        }

        int start = Math.max(0, matchIndex - SNIPPET_RADIUS);
        int end = Math.min(content.length(), matchIndex + term.length() + SNIPPET_RADIUS);

        String before = HtmlUtils.htmlEscape(content.substring(start, matchIndex));
        String match = HtmlUtils.htmlEscape(content.substring(matchIndex, matchIndex + term.length()));
        String after = HtmlUtils.htmlEscape(content.substring(matchIndex + term.length(), end));

        return (start > 0 ? "…" : "") + before + "<mark>" + match + "</mark>" + after + (end < content.length() ? "…" : "");
    }

    public static class SearchResult
    {
        public Long messageId;
        public Long agentId;
        public String agentName;
        public String role;
        public java.util.Date createdAt;
        public String snippetHtml;

        public Long getMessageId() { return messageId; }
        public Long getAgentId() { return agentId; }
        public String getAgentName() { return agentName; }
        public String getRole() { return role; }
        public java.util.Date getCreatedAt() { return createdAt; }
        public String getSnippetHtml() { return snippetHtml; }
    }
}
