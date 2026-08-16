package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.services.agentic.ClaudeCliSessionService;
import ch.ksfx.services.agentic.ClaudeCliSessionService.AgentTriggeredTurnResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

/**
 * Agent-to-agent messaging API - lets an agent (the caller) synchronously trigger a turn on
 * another agent (the target - any agent, no same-AgenticProject restriction) via curl from its own
 * Bash tool, getting the target's reply back as the HTTP response body. Mirrors
 * AgentScheduleApiController closely: no Spring Security session/CSRF involved (path is
 * permitAll(), see WebSecurityConfig) - auth is a plain per-agent bearer token checked inline in
 * {@link #authenticate}, same pattern, duplicated rather than shared since there is still no
 * reusable "resolve bearer token to Agent" helper bean in this codebase.
 */
@RestController
@RequestMapping("/agentic/api/message")
public class AgentMessageApiController
{
    private final AgentDAO agentDAO;
    private final ClaudeCliSessionService claudeCliSessionService;

    public AgentMessageApiController(AgentDAO agentDAO, ClaudeCliSessionService claudeCliSessionService)
    {
        this.agentDAO = agentDAO;
        this.claudeCliSessionService = claudeCliSessionService;
    }

    @PostMapping
    public ResponseEntity<?> send(HttpServletRequest request, @RequestBody AgentMessageApiDto body)
    {
        Agent caller = authenticate(request);

        if (caller == null) {
            return unauthorized();
        }

        if (body.targetAgentId == null || isBlank(body.message)) {
            return ResponseEntity.badRequest().body(errorBody("targetAgentId and message are required"));
        }

        if (body.targetAgentId.equals(caller.getId())) {
            return ResponseEntity.badRequest().body(errorBody("Cannot message yourself."));
        }

        Agent target = agentDAO.getAgentForId(body.targetAgentId);

        if (target == null || !target.getEnabled()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("Target agent not found or disabled."));
        }

        AgentTriggeredTurnResult result = claudeCliSessionService.runAgentTriggeredTurn(body.targetAgentId, caller, body.message);

        if (result.isSkipped()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody("Target agent is already busy with another turn."));
        }

        if (result.getErrorMessage() != null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(result.getErrorMessage()));
        }

        AgentMessageApiDto response = new AgentMessageApiDto();
        response.targetAgentId = target.getId();
        response.targetAgentName = target.getName();
        response.reply = result.getReply();

        return ResponseEntity.ok(response);
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

    private ResponseEntity<?> unauthorized()
    {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Unauthorized"));
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    private Map<String, String> errorBody(String message)
    {
        return Collections.singletonMap("error", message);
    }

    /**
     * Request/response shape for this API - not the raw AgentMessage/Agent entities, for the same
     * reason as AgentScheduleApiController's DTO: avoids ever serializing an Agent's apiToken back
     * into a response. Asymmetric field use: targetAgentId/message are populated by the client;
     * targetAgentId/targetAgentName/reply are populated in the response (targetAgentId in both) -
     * @JsonInclude keeps the client-only `message` field out of the response body instead of
     * serializing it as a stray null.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class AgentMessageApiDto
    {
        public Long targetAgentId;
        public String message;
        public String targetAgentName;
        public String reply;
    }
}
