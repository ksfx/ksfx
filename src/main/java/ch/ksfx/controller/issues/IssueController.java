package ch.ksfx.controller.issues;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.IssueAssetDAO;
import ch.ksfx.dao.IssueCommentDAO;
import ch.ksfx.dao.IssueDAO;
import ch.ksfx.dao.IssueLabelDAO;
import ch.ksfx.dao.IssueTrackerDAO;
import ch.ksfx.dao.user.UserDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.issues.*;
import ch.ksfx.model.user.User;
import ch.ksfx.services.issues.IssueService;
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
 * The built-in issue tracker (GitHub-analog, sibling of {@link ch.ksfx.controller.wiki.WikiController}
 * and deliberately built on the same patterns): multiple independent {@link IssueTracker}s with a
 * sidebar switcher, markdown everywhere (same vendored Toast UI Editor), images/attachments as DB
 * blobs ({@link IssueAsset}), human-or-agent attribution on issues and comments, per-tracker
 * {@link IssueLabel}s, priorities and a three-state status - but NO versioning (explicit design
 * decision; descriptions are edited in place). Session-authenticated; the parallel agent-facing
 * path is AgentIssueApiController.
 */
@Controller
@RequestMapping("/issues")
public class IssueController
{
    private final IssueTrackerDAO issueTrackerDAO;
    private final IssueDAO issueDAO;
    private final IssueLabelDAO issueLabelDAO;
    private final IssueCommentDAO issueCommentDAO;
    private final IssueAssetDAO issueAssetDAO;
    private final IssueService issueService;
    private final UserDAO userDAO;
    private final AgentDAO agentDAO;

    public IssueController(IssueTrackerDAO issueTrackerDAO, IssueDAO issueDAO, IssueLabelDAO issueLabelDAO,
                            IssueCommentDAO issueCommentDAO, IssueAssetDAO issueAssetDAO, IssueService issueService,
                            UserDAO userDAO, AgentDAO agentDAO)
    {
        this.issueTrackerDAO = issueTrackerDAO;
        this.issueDAO = issueDAO;
        this.issueLabelDAO = issueLabelDAO;
        this.issueCommentDAO = issueCommentDAO;
        this.issueAssetDAO = issueAssetDAO;
        this.issueService = issueService;
        this.userDAO = userDAO;
        this.agentDAO = agentDAO;
    }

    @GetMapping({"/", ""})
    public String root()
    {
        List<IssueTracker> trackers = issueTrackerDAO.getAllIssueTrackers();
        return trackers.isEmpty() ? "redirect:/issues/none" : "redirect:/issues/" + trackers.get(0).getId() + "/";
    }

    @GetMapping("/none")
    public String none(Model model)
    {
        model.addAttribute("allTrackers", Collections.emptyList());
        return "issues/issues_none";
    }

    @PostMapping("/create")
    public String create(@RequestParam String name, RedirectAttributes redirectAttributes)
    {
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "A tracker needs a name.");
            return "redirect:/issues/none";
        }

        IssueTracker tracker = new IssueTracker();
        tracker.setName(name.trim());
        tracker.setCreatedAt(new Date());
        issueTrackerDAO.saveOrUpdateIssueTracker(tracker);

        return "redirect:/issues/" + tracker.getId() + "/";
    }

    /** Same no-flash-message reasoning as WikiController.deleteWiki - the redirect chain would eat it. */
    @PostMapping("/{trackerId}/delete")
    public String deleteTracker(@PathVariable Long trackerId)
    {
        issueTrackerDAO.deleteIssueTracker(requireTracker(trackerId));

        return "redirect:/issues/";
    }

    /** The issue list - `filter` open (default, = OPEN + IN_PROGRESS, GitHub-style), closed, or all. */
    @GetMapping("/{trackerId}/")
    public String list(@PathVariable Long trackerId, @RequestParam(defaultValue = "open") String filter, Model model)
    {
        IssueTracker tracker = requireTracker(trackerId);

        List<Issue> issues = issueDAO.getIssuesForTracker(trackerId, statusesForFilter(filter));
        Map<Long, Integer> commentCounts = new HashMap<>();

        for (Issue issue : issues) {
            commentCounts.put(issue.getId(), issueCommentDAO.countCommentsForIssue(issue.getId()));
        }

        baseModel(model, tracker);
        model.addAttribute("issues", issues);
        model.addAttribute("commentCounts", commentCounts);
        model.addAttribute("filter", filter);

        return "issues/issues_list";
    }

    @GetMapping("/{trackerId}/issue/{issueId}")
    public String view(@PathVariable Long trackerId, @PathVariable Long issueId, Model model)
    {
        IssueTracker tracker = requireTracker(trackerId);
        Issue issue = requireIssue(trackerId, issueId);

        baseModel(model, tracker);
        model.addAttribute("issue", issue);
        model.addAttribute("comments", issueCommentDAO.getCommentsForIssue(issueId));
        model.addAttribute("attachments", issueAssetDAO.getAssetsForIssue(issueId));

        return "issues/issues_view";
    }

    @GetMapping("/{trackerId}/issue/new")
    public String newIssue(@PathVariable Long trackerId, Model model)
    {
        IssueTracker tracker = requireTracker(trackerId);

        baseModel(model, tracker);
        formModel(model, tracker, null);

        return "issues/issues_edit";
    }

    @GetMapping("/{trackerId}/issue/{issueId}/edit")
    public String editIssue(@PathVariable Long trackerId, @PathVariable Long issueId, Model model)
    {
        IssueTracker tracker = requireTracker(trackerId);
        Issue issue = requireIssue(trackerId, issueId);

        baseModel(model, tracker);
        formModel(model, tracker, issue);

        return "issues/issues_edit";
    }

    @PostMapping("/{trackerId}/issue/save")
    public String saveIssue(@PathVariable Long trackerId,
                             @RequestParam(required = false) Long issueId,
                             @RequestParam String title,
                             @RequestParam(defaultValue = "") String description,
                             @RequestParam(defaultValue = "MEDIUM") IssuePriority priority,
                             @RequestParam(defaultValue = "OPEN") IssueStatus status,
                             @RequestParam(defaultValue = "") String assignee,
                             @RequestParam(required = false, name = "labelIds") List<Long> labelIds,
                             RedirectAttributes redirectAttributes)
    {
        IssueTracker tracker = requireTracker(trackerId);

        if (title == null || title.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "An issue needs a title.");
            return issueId != null
                    ? "redirect:/issues/" + trackerId + "/issue/" + issueId + "/edit"
                    : "redirect:/issues/" + trackerId + "/issue/new";
        }

        Issue issue;

        if (issueId != null) {
            issue = requireIssue(trackerId, issueId);
        } else {
            issue = new Issue();
            issue.setIssueTracker(tracker);
            issue.setCreatedAt(new Date());
            issue.setCreatedByUser(currentUser());
        }

        issue.setTitle(title.trim());
        issue.setDescription(description);
        issue.setPriority(priority);
        issue.setStatus(status);
        issue.setLabels(issueService.resolveLabelsByIds(trackerId, labelIds));
        applyAssignee(issue, assignee);
        issueService.touchAndSave(issue);

        return "redirect:/issues/" + trackerId + "/issue/" + issue.getId();
    }

    /** Quick status transitions from the detail view (Close / Reopen / Start progress) without the full edit form. */
    @PostMapping("/{trackerId}/issue/{issueId}/status")
    public String changeStatus(@PathVariable Long trackerId, @PathVariable Long issueId, @RequestParam IssueStatus status)
    {
        Issue issue = requireIssue(trackerId, issueId);
        issue.setStatus(status);
        issueService.touchAndSave(issue);

        return "redirect:/issues/" + trackerId + "/issue/" + issueId;
    }

    @PostMapping("/{trackerId}/issue/{issueId}/delete")
    public String deleteIssue(@PathVariable Long trackerId, @PathVariable Long issueId, RedirectAttributes redirectAttributes)
    {
        issueDAO.deleteIssue(requireIssue(trackerId, issueId));

        redirectAttributes.addFlashAttribute("resultMessage", "Issue deleted.");
        return "redirect:/issues/" + trackerId + "/";
    }

    @PostMapping("/{trackerId}/issue/{issueId}/comment")
    public String addComment(@PathVariable Long trackerId, @PathVariable Long issueId,
                              @RequestParam(defaultValue = "") String content, RedirectAttributes redirectAttributes)
    {
        Issue issue = requireIssue(trackerId, issueId);

        if (!content.trim().isEmpty()) {
            IssueComment comment = new IssueComment();
            comment.setIssue(issue);
            comment.setContent(content);
            comment.setCreatedByUser(currentUser());
            comment.setCreatedAt(new Date());
            issueCommentDAO.saveIssueComment(comment);

            issueService.touchAndSave(issue);
        }

        return "redirect:/issues/" + trackerId + "/issue/" + issueId;
    }

    @PostMapping("/{trackerId}/issue/{issueId}/comment/{commentId}/delete")
    public String deleteComment(@PathVariable Long trackerId, @PathVariable Long issueId, @PathVariable Long commentId)
    {
        requireIssue(trackerId, issueId);
        IssueComment comment = issueCommentDAO.getIssueCommentForId(commentId);

        if (comment != null && comment.getIssue().getId().equals(issueId)) {
            issueCommentDAO.deleteIssueComment(comment);
        }

        return "redirect:/issues/" + trackerId + "/issue/" + issueId;
    }

    @GetMapping("/{trackerId}/labels")
    public String labels(@PathVariable Long trackerId, Model model)
    {
        IssueTracker tracker = requireTracker(trackerId);

        baseModel(model, tracker);

        return "issues/issues_labels";
    }

    @PostMapping("/{trackerId}/labels/create")
    public String createLabel(@PathVariable Long trackerId, @RequestParam String name,
                               @RequestParam(defaultValue = IssueService.DEFAULT_LABEL_COLOR) String color,
                               RedirectAttributes redirectAttributes)
    {
        IssueTracker tracker = requireTracker(trackerId);

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "A label needs a name.");
        } else if (issueLabelDAO.getLabelForTrackerAndName(trackerId, name.trim()) != null) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "A label with that name already exists.");
        } else {
            IssueLabel label = new IssueLabel();
            label.setIssueTracker(tracker);
            label.setName(name.trim());
            label.setColor(color);
            label.setCreatedAt(new Date());
            issueLabelDAO.saveOrUpdateIssueLabel(label);
        }

        return "redirect:/issues/" + trackerId + "/labels";
    }

    @PostMapping("/{trackerId}/labels/{labelId}/update")
    public String updateLabel(@PathVariable Long trackerId, @PathVariable Long labelId,
                               @RequestParam String name, @RequestParam String color, RedirectAttributes redirectAttributes)
    {
        IssueLabel label = requireLabel(trackerId, labelId);

        if (name != null && !name.trim().isEmpty()) {
            label.setName(name.trim());
            label.setColor(color);
            issueLabelDAO.saveOrUpdateIssueLabel(label);
        }

        return "redirect:/issues/" + trackerId + "/labels";
    }

    @PostMapping("/{trackerId}/labels/{labelId}/delete")
    public String deleteLabel(@PathVariable Long trackerId, @PathVariable Long labelId)
    {
        issueLabelDAO.deleteIssueLabel(requireLabel(trackerId, labelId));

        return "redirect:/issues/" + trackerId + "/labels";
    }

    /** Inline editor images - same standalone-asset semantics as WikiController.uploadAsset. */
    @PostMapping("/asset/upload")
    @ResponseBody
    public Map<String, Object> uploadAsset(@RequestParam("file") MultipartFile file) throws IOException
    {
        IssueAsset asset = buildAsset(file, null);
        issueAssetDAO.saveIssueAsset(asset);

        Map<String, Object> response = new HashMap<>();
        response.put("url", "/issues/assets/" + asset.getId());
        return response;
    }

    @PostMapping("/{trackerId}/issue/{issueId}/attachment/upload")
    public String uploadAttachment(@PathVariable Long trackerId, @PathVariable Long issueId,
                                    @RequestParam("files") MultipartFile[] files) throws IOException
    {
        Issue issue = requireIssue(trackerId, issueId);

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            issueAssetDAO.saveIssueAsset(buildAsset(file, issue));
        }

        return "redirect:/issues/" + trackerId + "/issue/" + issueId;
    }

    @PostMapping("/{trackerId}/issue/{issueId}/attachment/{assetId}/delete")
    public String deleteAttachment(@PathVariable Long trackerId, @PathVariable Long issueId, @PathVariable Long assetId)
    {
        requireIssue(trackerId, issueId);
        IssueAsset asset = issueAssetDAO.getIssueAssetForId(assetId);

        if (asset != null && asset.getIssue() != null && asset.getIssue().getId().equals(issueId)) {
            issueAssetDAO.deleteIssueAsset(asset);
        }

        return "redirect:/issues/" + trackerId + "/issue/" + issueId;
    }

    @GetMapping("/assets/{id}")
    public ResponseEntity<Resource> asset(@PathVariable Long id)
    {
        IssueAsset asset = issueAssetDAO.getIssueAssetForId(id);

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

    /**
     * "Open" deliberately includes IN_PROGRESS (GitHub semantics: not-closed) - IN_PROGRESS is a
     * refinement of open, not a third bucket, so the list tabs stay the familiar Open/Closed/All.
     */
    private List<IssueStatus> statusesForFilter(String filter)
    {
        switch (filter) {
            case "closed": return Collections.singletonList(IssueStatus.CLOSED);
            case "all": return null;
            default: return Arrays.asList(IssueStatus.OPEN, IssueStatus.IN_PROGRESS);
        }
    }

    /**
     * The assignee select submits "user:{id}", "agent:{id}" or "" (unassigned) - one dropdown for
     * both kinds, parsed here into the either/or pair of nullable FKs.
     */
    private void applyAssignee(Issue issue, String assignee)
    {
        issue.setAssigneeUser(null);
        issue.setAssigneeAgent(null);

        if (assignee.startsWith("user:")) {
            issue.setAssigneeUser(userDAO.getUser(Long.valueOf(assignee.substring("user:".length()))));
        } else if (assignee.startsWith("agent:")) {
            issue.setAssigneeAgent(agentDAO.getAgentForId(Long.valueOf(assignee.substring("agent:".length()))));
        }
    }

    private void baseModel(Model model, IssueTracker tracker)
    {
        model.addAttribute("allTrackers", issueTrackerDAO.getAllIssueTrackers());
        model.addAttribute("currentTracker", tracker);
        model.addAttribute("trackerLabels", issueLabelDAO.getLabelsForTracker(tracker.getId()));
        model.addAttribute("openCount", issueDAO.countIssuesForTracker(tracker.getId(), Arrays.asList(IssueStatus.OPEN, IssueStatus.IN_PROGRESS)));
        model.addAttribute("closedCount", issueDAO.countIssuesForTracker(tracker.getId(), Collections.singletonList(IssueStatus.CLOSED)));
    }

    private void formModel(Model model, IssueTracker tracker, Issue issue)
    {
        Set<Long> selectedLabelIds = new HashSet<>();

        if (issue != null && issue.getLabels() != null) {
            for (IssueLabel label : issue.getLabels()) {
                selectedLabelIds.add(label.getId());
            }
        }

        model.addAttribute("isNew", issue == null);
        model.addAttribute("issue", issue);
        model.addAttribute("selectedLabelIds", selectedLabelIds);
        model.addAttribute("allUsers", userDAO.getAllUsers());
        model.addAttribute("allAgents", agentDAO.getAllAgents());
        model.addAttribute("allStatuses", IssueStatus.values());
        model.addAttribute("allPriorities", IssuePriority.values());
    }

    private IssueTracker requireTracker(Long trackerId)
    {
        IssueTracker tracker = issueTrackerDAO.getIssueTrackerForId(trackerId);

        if (tracker == null) {
            throw new IllegalArgumentException("Tracker not found");
        }

        return tracker;
    }

    private Issue requireIssue(Long trackerId, Long issueId)
    {
        Issue issue = issueDAO.getIssueForId(issueId);

        if (issue == null || !issue.getIssueTracker().getId().equals(trackerId)) {
            throw new IllegalArgumentException("Issue not found");
        }

        return issue;
    }

    private IssueLabel requireLabel(Long trackerId, Long labelId)
    {
        IssueLabel label = issueLabelDAO.getIssueLabelForId(labelId);

        if (label == null || !label.getIssueTracker().getId().equals(trackerId)) {
            throw new IllegalArgumentException("Label not found");
        }

        return label;
    }

    private IssueAsset buildAsset(MultipartFile file, Issue issue) throws IOException
    {
        IssueAsset asset = new IssueAsset();
        asset.setIssue(issue);
        asset.setFileName(file.getOriginalFilename());
        asset.setContentType(file.getContentType());
        asset.setContent(file.getBytes());
        asset.setFileSize(file.getSize());
        asset.setCreatedAt(new Date());
        asset.setUploadedByUser(currentUser());
        return asset;
    }

    private User currentUser()
    {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;

        return principal instanceof User ? (User) principal : null;
    }
}
