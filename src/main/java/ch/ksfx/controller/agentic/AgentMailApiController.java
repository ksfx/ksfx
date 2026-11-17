package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.AgenticConfigDAO;
import ch.ksfx.dao.MailDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.model.mail.MailOutboxEntry;
import ch.ksfx.services.agentic.AgentWorkspaceService;
import ch.ksfx.services.mail.MailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mail for agents (concept "Mail service", KSFX wiki) - called via curl from the agent's Bash tool
 * with its bearer token, like the schedule, wiki and issue APIs. The mail goes into the platform's
 * outbox (MailService) and is sent in the background; the answer is immediate.
 *
 * Recipients are plain addresses or "user:{id}" (the server fills in that user's address - the agent
 * never needs to know it). Attachments are paths inside the agent's own workspace only. No recipient
 * rules or per-agent quotas for now - the platform-wide daily limit is the emergency brake.
 */
@RestController
@RequestMapping("/agentic/api/mail")
public class AgentMailApiController
{
    private final AgentDAO agentDAO;
    private final MailService mailService;
    private final MailDAO mailDAO;
    private final AgenticConfigDAO agenticConfigDAO;
    private final AgentWorkspaceService agentWorkspaceService;

    public AgentMailApiController(AgentDAO agentDAO, MailService mailService, MailDAO mailDAO, AgenticConfigDAO agenticConfigDAO,
                                  AgentWorkspaceService agentWorkspaceService)
    {
        this.agentDAO = agentDAO;
        this.mailService = mailService;
        this.mailDAO = mailDAO;
        this.agenticConfigDAO = agenticConfigDAO;
        this.agentWorkspaceService = agentWorkspaceService;
    }

    @PostMapping
    public ResponseEntity<?> send(HttpServletRequest request, @RequestBody MailBody body)
    {
        Agent agent = authenticate(request);

        if (agent == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Unauthorized"));
        }

        MailService.MailRequest mail = new MailService.MailRequest();

        try {
            mail.to = resolveRecipients(body.to);
            mail.cc = resolveRecipients(body.cc);
            mail.attachments = readAttachments(agent, body.attachments);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorBody(e.getMessage()));
        }

        mail.subject = body.subject;
        mail.markdownBody = body.body;
        mail.sourceType = MailOutboxEntry.SourceType.AGENT;
        mail.sourceLabel = agent.getName();
        mail.agent = agent;
        mail.project = agent.getProject();

        try {
            MailOutboxEntry entry = mailService.submit(mail);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(toDto(entry));
        } catch (MailService.MailRejectedException e) {
            HttpStatus status = e.getReason() == MailService.MailRejectedException.Reason.INVALID ? HttpStatus.BAD_REQUEST
                    : e.getReason() == MailService.MailRejectedException.Reason.LIMIT ? HttpStatus.TOO_MANY_REQUESTS
                    : HttpStatus.SERVICE_UNAVAILABLE;

            return ResponseEntity.status(status).body(errorBody(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> status(HttpServletRequest request, @PathVariable Long id)
    {
        Agent agent = authenticate(request);

        if (agent == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Unauthorized"));
        }

        MailOutboxEntry entry = mailDAO.getOutboxEntryForId(id);

        // Only the agent's own mails - a token must never reveal what other agents sent.
        if (entry == null || entry.getAgent() == null || !entry.getAgent().getId().equals(agent.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("No mail with id " + id + " sent by you."));
        }

        return ResponseEntity.ok(toDto(entry));
    }

    /** Addresses stay as they are; "user:{id}" becomes that user's address. */
    private List<String> resolveRecipients(List<String> recipients)
    {
        List<String> result = new ArrayList<>();

        if (recipients == null) {
            return result;
        }

        for (String recipient : recipients) {
            if (recipient != null && recipient.trim().startsWith("user:")) {
                String address;

                try {
                    address = mailService.addressOfUser(Long.valueOf(recipient.trim().substring("user:".length())));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid recipient: " + recipient);
                }

                if (address == null) {
                    throw new IllegalArgumentException("No mail address known for " + recipient.trim() + ".");
                }

                result.add(address);
            } else {
                result.add(recipient);
            }
        }

        return result;
    }

    /** Files from the agent's own workspace only - a path that leaves it is rejected. */
    private List<MailService.Attachment> readAttachments(Agent agent, List<String> paths)
    {
        List<MailService.Attachment> attachments = new ArrayList<>();

        if (paths == null || paths.isEmpty()) {
            return attachments;
        }

        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        if (config == null) {
            throw new IllegalArgumentException("Attachments are not available - Agentic has no workspace root configured.");
        }

        Path workspace;

        try {
            workspace = agentWorkspaceService.resolveWorkspace(agent, config).toRealPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("Your workspace could not be found for attachments.");
        }

        long total = 0;

        for (String raw : paths) {
            String relative = raw != null ? raw.trim() : "";

            // The agent may hand over an absolute path it sees inside its container or on the host -
            // only the part relative to its workspace counts.
            String containerPrefix = "/workspace/agent-" + agent.getId() + "/";
            if (relative.startsWith(containerPrefix)) {
                relative = relative.substring(containerPrefix.length());
            }

            Path file;

            try {
                // toRealPath resolves symlinks: a link inside the workspace pointing outside it (on a
                // Docker agent: to a host file it could never read itself) must not get through.
                file = (relative.startsWith("/") ? java.nio.file.Paths.get(relative) : workspace.resolve(relative)).toRealPath();
            } catch (IOException e) {
                throw new IllegalArgumentException("Attachment not found in your workspace: " + raw);
            }

            if (!file.startsWith(workspace) || !Files.isRegularFile(file)) {
                throw new IllegalArgumentException("Attachment not found in your workspace: " + raw);
            }

            try {
                byte[] data = Files.readAllBytes(file);
                total += data.length;

                if (total > MailService.MAX_ATTACHMENT_BYTES) {
                    throw new IllegalArgumentException("Attachments too large - at most " + (MailService.MAX_ATTACHMENT_BYTES / 1024 / 1024) + " MB in total.");
                }

                String fileName = file.getFileName().toString();
                String contentType = URLConnection.guessContentTypeFromName(fileName);
                attachments.add(new MailService.Attachment(fileName, contentType, data));
            } catch (IOException e) {
                throw new IllegalArgumentException("Attachment could not be read: " + raw);
            }
        }

        return attachments;
    }

    private Map<String, Object> toDto(MailOutboxEntry entry)
    {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", entry.getId());
        dto.put("status", entry.getStatus().name());
        dto.put("to", entry.getToAddresses());
        dto.put("cc", entry.getCcAddresses());
        dto.put("subject", entry.getSubject());
        dto.put("attempts", entry.getAttempts());
        dto.put("lastError", entry.getLastError());
        dto.put("createdAt", entry.getCreatedAt());
        dto.put("sentAt", entry.getSentAt());

        return dto;
    }

    private Agent authenticate(HttpServletRequest request)
    {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }

        String token = header.substring("Bearer ".length()).trim();
        Agent agent = token.isEmpty() ? null : agentDAO.getAgentForApiToken(token);

        return agent != null && agent.getEnabled() ? agent : null;
    }

    private Map<String, String> errorBody(String message)
    {
        return Collections.singletonMap("error", message);
    }

    public static class MailBody
    {
        public List<String> to;
        public List<String> cc;
        public String subject;
        public String body;
        public List<String> attachments;
    }
}
