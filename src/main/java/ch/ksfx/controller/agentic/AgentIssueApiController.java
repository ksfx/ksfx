package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.IssueAssetDAO;
import ch.ksfx.dao.IssueCommentDAO;
import ch.ksfx.dao.IssueDAO;
import ch.ksfx.dao.IssueTrackerDAO;
import ch.ksfx.dao.user.UserDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.issues.*;
import ch.ksfx.model.user.User;
import ch.ksfx.services.issues.IssueService;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lets an agent do everything the Issues web UI can - list/read/create/update issues, comment,
 * upload assets - using its own bearer token (see Agent.apiToken): same inline-bearer pattern as
 * AgentWikiApiController and friends, and the same decisive property: the authenticated token alone
 * decides attribution (createdByAgent on issues/comments/uploads), never the request body.
 *
 * Conveniences mirroring the wiki API: labels are addressed by NAME (auto-created with a neutral
 * color if unknown - see IssueService), and assignees by "user:{id}"/"agent:{id}" strings with a
 * discovery endpoint ({@link #assignees}) listing both kinds, so an agent can assign work to a
 * human or a fellow agent without knowing KSFX-internal ids in advance.
 */
@RestController
@RequestMapping("/agentic/api/issues")
public class AgentIssueApiController
{
    private final AgentDAO agentDAO;
    private final IssueTrackerDAO issueTrackerDAO;
    private final IssueDAO issueDAO;
    private final IssueCommentDAO issueCommentDAO;
    private final IssueAssetDAO issueAssetDAO;
    private final IssueService issueService;
    private final UserDAO userDAO;

    public AgentIssueApiController(AgentDAO agentDAO, IssueTrackerDAO issueTrackerDAO, IssueDAO issueDAO,
                                    IssueCommentDAO issueCommentDAO, IssueAssetDAO issueAssetDAO,
                                    IssueService issueService, UserDAO userDAO)
    {
        this.agentDAO = agentDAO;
        this.issueTrackerDAO = issueTrackerDAO;
        this.issueDAO = issueDAO;
        this.issueCommentDAO = issueCommentDAO;
        this.issueAssetDAO = issueAssetDAO;
        this.issueService = issueService;
        this.userDAO = userDAO;
    }

    @GetMapping("/trackers")
    public ResponseEntity<?> trackers(HttpServletRequest request)
    {
        if (authenticate(request) == null) {
            return unauthorized();
        }

        List<TrackerDto> trackers = new ArrayList<>();

        for (IssueTracker tracker : issueTrackerDAO.getAllIssueTrackers()) {
            TrackerDto dto = new TrackerDto();
            dto.id = tracker.getId();
            dto.name = tracker.getName();
            trackers.add(dto);
        }

        return ResponseEntity.ok(trackers);
    }

    /** Who can be assigned - both humans and agents, in the same "user:{id}"/"agent:{id}" form the write endpoints accept. */
    @GetMapping("/assignees")
    public ResponseEntity<?> assignees(HttpServletRequest request)
    {
        if (authenticate(request) == null) {
            return unauthorized();
        }

        List<AssigneeDto> assignees = new ArrayList<>();

        for (User user : userDAO.getAllUsers()) {
            AssigneeDto dto = new AssigneeDto();
            dto.assignee = "user:" + user.getId();
            dto.name = user.getUsername();
            dto.type = "user";
            assignees.add(dto);
        }

        for (Agent agent : agentDAO.getAllAgents()) {
            if (!agent.getEnabled()) {
                continue;
            }

            AssigneeDto dto = new AssigneeDto();
            dto.assignee = "agent:" + agent.getId();
            dto.name = agent.getName();
            dto.type = "agent";
            assignees.add(dto);
        }

        return ResponseEntity.ok(assignees);
    }

    /** {@code status} filter: open (default, = OPEN + IN_PROGRESS), closed, all. */
    @GetMapping("/{trackerId}/issues")
    public ResponseEntity<?> list(HttpServletRequest request, @PathVariable Long trackerId,
                                   @RequestParam(defaultValue = "open") String status)
    {
        if (authenticate(request) == null) {
            return unauthorized();
        }

        if (issueTrackerDAO.getIssueTrackerForId(trackerId) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("No tracker with that id."));
        }

        List<IssueStatus> statuses;

        switch (status) {
            case "closed": statuses = Collections.singletonList(IssueStatus.CLOSED); break;
            case "all": statuses = null; break;
            default: statuses = Arrays.asList(IssueStatus.OPEN, IssueStatus.IN_PROGRESS);
        }

        List<IssueDto> issues = new ArrayList<>();

        for (Issue issue : issueDAO.getIssuesForTracker(trackerId, statuses)) {
            issues.add(toDto(issue, false));
        }

        return ResponseEntity.ok(issues);
    }

    @GetMapping("/{trackerId}/issue/{issueId}")
    public ResponseEntity<?> read(HttpServletRequest request, @PathVariable Long trackerId, @PathVariable Long issueId)
    {
        if (authenticate(request) == null) {
            return unauthorized();
        }

        Issue issue = findIssue(trackerId, issueId);

        if (issue == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("No issue with that id in this tracker."));
        }

        IssueDto dto = toDto(issue, true);
        dto.comments = new ArrayList<>();

        for (IssueComment comment : issueCommentDAO.getCommentsForIssue(issueId)) {
            CommentDto commentDto = new CommentDto();
            commentDto.commentId = comment.getId();
            commentDto.author = authorOf(comment.getCreatedByUser(), comment.getCreatedByAgent());
            commentDto.content = comment.getContent();
            commentDto.createdAt = comment.getCreatedAt();
            dto.comments.add(commentDto);
        }

        dto.attachments = new ArrayList<>();

        for (IssueAsset asset : issueAssetDAO.getAssetsForIssue(issueId)) {
            AttachmentDto attachmentDto = new AttachmentDto();
            attachmentDto.assetId = asset.getId();
            attachmentDto.fileName = asset.getFileName();
            attachmentDto.url = "/issues/assets/" + asset.getId();
            dto.attachments.add(attachmentDto);
        }

        return ResponseEntity.ok(dto);
    }

    /** Create. Only {@code title} is required - everything else falls back to defaults (OPEN/MEDIUM, unassigned, no labels). */
    @PostMapping("/{trackerId}/issue")
    public ResponseEntity<?> create(HttpServletRequest request, @PathVariable Long trackerId, @RequestBody IssueDto body)
    {
        Agent agent = authenticate(request);

        if (agent == null) {
            return unauthorized();
        }

        IssueTracker tracker = issueTrackerDAO.getIssueTrackerForId(trackerId);

        if (tracker == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("No tracker with that id."));
        }

        if (isBlank(body.title)) {
            return ResponseEntity.badRequest().body(errorBody("title is required"));
        }

        Issue issue = new Issue();
        issue.setIssueTracker(tracker);
        issue.setCreatedAt(new Date());
        issue.setCreatedByAgent(agent);

        String error = applyFields(issue, tracker, body, true);

        if (error != null) {
            return ResponseEntity.badRequest().body(errorBody(error));
        }

        issueService.touchAndSave(issue);

        return ResponseEntity.ok(toDto(issue, false));
    }

    /**
     * Partial update - only fields present in the body change ({@code null} = leave as is), so an
     * agent can flip just the status without re-sending everything. Exception: {@code assignee}
     * uses "" (empty string) for "unassign", since null already means "don't touch".
     */
    @PostMapping("/{trackerId}/issue/{issueId}")
    public ResponseEntity<?> update(HttpServletRequest request, @PathVariable Long trackerId, @PathVariable Long issueId,
                                     @RequestBody IssueDto body)
    {
        Agent agent = authenticate(request);

        if (agent == null) {
            return unauthorized();
        }

        Issue issue = findIssue(trackerId, issueId);

        if (issue == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("No issue with that id in this tracker."));
        }

        String error = applyFields(issue, issue.getIssueTracker(), body, false);

        if (error != null) {
            return ResponseEntity.badRequest().body(errorBody(error));
        }

        issueService.touchAndSave(issue);

        return ResponseEntity.ok(toDto(issue, false));
    }

    @PostMapping("/{trackerId}/issue/{issueId}/comment")
    public ResponseEntity<?> comment(HttpServletRequest request, @PathVariable Long trackerId, @PathVariable Long issueId,
                                      @RequestBody CommentDto body)
    {
        Agent agent = authenticate(request);

        if (agent == null) {
            return unauthorized();
        }

        Issue issue = findIssue(trackerId, issueId);

        if (issue == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("No issue with that id in this tracker."));
        }

        if (isBlank(body.content)) {
            return ResponseEntity.badRequest().body(errorBody("content is required"));
        }

        IssueComment comment = new IssueComment();
        comment.setIssue(issue);
        comment.setContent(body.content);
        comment.setCreatedByAgent(agent);
        comment.setCreatedAt(new Date());
        issueCommentDAO.saveIssueComment(comment);

        issueService.touchAndSave(issue);

        CommentDto response = new CommentDto();
        response.commentId = comment.getId();
        response.author = agent.getName();
        response.content = comment.getContent();
        response.createdAt = comment.getCreatedAt();

        return ResponseEntity.ok(response);
    }

    /** Same standalone-vs-attached split as the wiki asset endpoint: no {@code issueId} = inline/embed URL, with = attachment. */
    @PostMapping("/asset")
    public ResponseEntity<?> uploadAsset(HttpServletRequest request, @RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) Long issueId) throws IOException
    {
        Agent agent = authenticate(request);

        if (agent == null) {
            return unauthorized();
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("file is required"));
        }

        Issue issue = null;

        if (issueId != null) {
            issue = issueDAO.getIssueForId(issueId);

            if (issue == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("No issue with that id."));
            }
        }

        IssueAsset asset = new IssueAsset();
        asset.setIssue(issue);
        asset.setFileName(file.getOriginalFilename());
        asset.setContentType(file.getContentType());
        asset.setContent(file.getBytes());
        asset.setFileSize(file.getSize());
        asset.setCreatedAt(new Date());
        asset.setUploadedByAgent(agent);
        issueAssetDAO.saveIssueAsset(asset);

        Map<String, Object> response = new HashMap<>();
        response.put("assetId", asset.getId());
        response.put("url", "/issues/assets/" + asset.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * Applies the DTO's non-null fields onto the issue. {@code isCreate} controls whether absent
     * enum fields fall back to defaults (create) or stay untouched (update). Returns an error
     * message, or null on success.
     */
    private String applyFields(Issue issue, IssueTracker tracker, IssueDto body, boolean isCreate)
    {
        if (body.title != null) {
            if (body.title.trim().isEmpty()) {
                return "title must not be blank";
            }

            issue.setTitle(body.title.trim());
        }

        if (body.description != null) {
            issue.setDescription(body.description);
        }

        try {
            if (body.status != null) {
                issue.setStatus(IssueStatus.valueOf(body.status.toUpperCase()));
            } else if (isCreate) {
                issue.setStatus(IssueStatus.OPEN);
            }

            if (body.priority != null) {
                issue.setPriority(IssuePriority.valueOf(body.priority.toUpperCase()));
            } else if (isCreate) {
                issue.setPriority(IssuePriority.MEDIUM);
            }
        } catch (IllegalArgumentException e) {
            return "Invalid status or priority - status: OPEN/IN_PROGRESS/CLOSED, priority: LOW/MEDIUM/HIGH/CRITICAL";
        }

        if (body.labels != null) {
            issue.setLabels(issueService.resolveOrCreateLabelsByNames(tracker, body.labels));
        }

        if (body.assignee != null) {
            if (body.assignee.isEmpty()) {
                issue.setAssigneeUser(null);
                issue.setAssigneeAgent(null);
            } else if (body.assignee.startsWith("user:")) {
                User user = userDAO.getUser(Long.valueOf(body.assignee.substring("user:".length())));

                if (user == null) {
                    return "Unknown user in assignee";
                }

                issue.setAssigneeUser(user);
                issue.setAssigneeAgent(null);
            } else if (body.assignee.startsWith("agent:")) {
                Agent assigneeAgent = agentDAO.getAgentForId(Long.valueOf(body.assignee.substring("agent:".length())));

                if (assigneeAgent == null) {
                    return "Unknown agent in assignee";
                }

                issue.setAssigneeAgent(assigneeAgent);
                issue.setAssigneeUser(null);
            } else {
                return "assignee must be \"user:{id}\", \"agent:{id}\" or \"\" - see GET /agentic/api/issues/assignees";
            }
        }

        return null;
    }

    private Issue findIssue(Long trackerId, Long issueId)
    {
        Issue issue = issueDAO.getIssueForId(issueId);
        return issue != null && issue.getIssueTracker().getId().equals(trackerId) ? issue : null;
    }

    private IssueDto toDto(Issue issue, boolean withDescription)
    {
        IssueDto dto = new IssueDto();
        dto.issueId = issue.getId();
        dto.title = issue.getTitle();
        dto.status = issue.getStatus().name();
        dto.priority = issue.getPriority().name();
        dto.createdAt = issue.getCreatedAt();
        dto.updatedAt = issue.getUpdatedAt();
        dto.createdBy = authorOf(issue.getCreatedByUser(), issue.getCreatedByAgent());

        if (withDescription) {
            dto.description = issue.getDescription();
        }

        dto.labels = new ArrayList<>();

        if (issue.getLabels() != null) {
            for (IssueLabel label : issue.getLabels()) {
                dto.labels.add(label.getName());
            }
        }

        if (issue.getAssigneeUser() != null) {
            dto.assignee = "user:" + issue.getAssigneeUser().getId();
            dto.assigneeName = issue.getAssigneeUser().getUsername();
        } else if (issue.getAssigneeAgent() != null) {
            dto.assignee = "agent:" + issue.getAssigneeAgent().getId();
            dto.assigneeName = issue.getAssigneeAgent().getName();
        }

        return dto;
    }

    private String authorOf(User user, Agent agent)
    {
        if (agent != null) {
            return "agent:" + agent.getName();
        }

        return user != null ? user.getUsername() : null;
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
    public static class TrackerDto
    {
        public Long id;
        public String name;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AssigneeDto
    {
        public String assignee;
        public String name;
        public String type;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IssueDto
    {
        public Long issueId;
        public String title;
        public String description;
        public String status;
        public String priority;
        public List<String> labels;
        public String assignee;
        public String assigneeName;
        public String createdBy;
        public Date createdAt;
        public Date updatedAt;
        public List<CommentDto> comments;
        public List<AttachmentDto> attachments;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommentDto
    {
        public Long commentId;
        public String author;
        public String content;
        public Date createdAt;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AttachmentDto
    {
        public Long assetId;
        public String fileName;
        public String url;
    }
}
