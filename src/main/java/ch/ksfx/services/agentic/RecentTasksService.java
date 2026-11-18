package ch.ksfx.services.agentic;

import ch.ksfx.dao.AgentMessageDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgentMessage;
import ch.ksfx.model.AgentMessageRole;
import ch.ksfx.model.Project;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The dashboard's task feed: the latest things the project's agents were asked - by a human, a
 * schedule or another agent - each with what came of it (answer, error, still running). Read-only
 * and rendered with the page; one small query per task for its outcome, fine for a handful of rows.
 *
 * Who asked: AGENT rows name the sending agent; USER rows the human who sent them, or "Schedule"
 * when no human is recorded - which is also what chat messages from before sender tracking
 * (2026-11-01) show, an acceptable blur for history that old. Queued messages are not tasks yet
 * (they live in agent_message_queue until their turn starts) and do not appear.
 */
@Service("recentTasksService")
public class RecentTasksService
{
    public static final int TASK_LIMIT = 8;
    private static final int TEXT_LIMIT = 280;
    private static final int ANSWER_LIMIT = 160;

    /** Badge colors per agent (by id) - CI navy first, then muted tones that sit well next to it. */
    private static final String[] AGENT_COLORS = { "#0b2142", "#1f6f8b", "#6a4c93", "#2a7f62", "#a4503a", "#8a6d1e", "#3d5a80" };

    private final AgentMessageDAO agentMessageDAO;
    private final ClaudeCliSessionService claudeCliSessionService;

    public RecentTasksService(AgentMessageDAO agentMessageDAO, ClaudeCliSessionService claudeCliSessionService)
    {
        this.agentMessageDAO = agentMessageDAO;
        this.claudeCliSessionService = claudeCliSessionService;
    }

    public List<RecentTask> recentTasks(Project project)
    {
        List<RecentTask> tasks = new ArrayList<>();

        if (project == null || project.getId() == null) {
            return tasks;
        }

        Date now = new Date();

        for (AgentMessage message : agentMessageDAO.getRecentTasksForProject(project.getId(), TASK_LIMIT)) {
            Agent agent = message.getAgent();
            RecentTask task = new RecentTask();
            task.agentId = agent.getId();
            task.agentName = agent.getName();
            task.agentInitial = agent.getName() != null && !agent.getName().trim().isEmpty() ? agent.getName().trim().substring(0, 1).toUpperCase() : "?";
            task.agentColor = AGENT_COLORS[(int) (Math.abs(agent.getId()) % AGENT_COLORS.length)];
            task.text = shorten(message.getContent(), TEXT_LIMIT);
            task.when = relative(message.getCreatedAt(), now);
            task.whenExact = message.getCreatedAt() != null ? new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH).format(message.getCreatedAt()) : "";

            if (message.getRole() == AgentMessageRole.AGENT) {
                task.senderIcon = "fa-sitemap";
                task.senderLabel = message.getFromAgent() != null ? message.getFromAgent().getName() : "Agent";
            } else if (message.getFromUser() != null) {
                task.senderIcon = "fa-user";
                task.senderLabel = message.getUserLabel();
            } else {
                task.senderIcon = "fa-clock-o";
                task.senderLabel = "Schedule";
            }

            AgentMessage outcome = agentMessageDAO.getOutcomeAfter(agent.getId(), message.getId());

            if (outcome != null && outcome.getRole() == AgentMessageRole.ASSISTANT) {
                task.status = "done";
                task.statusLabel = outcome.getDurationMs() != null ? duration(outcome.getDurationMs()) : "done";
                task.answer = shorten(outcome.getContent(), ANSWER_LIMIT);
            } else if (outcome != null) {
                task.status = "error";
                task.statusLabel = "no answer";
                task.answer = shorten(outcome.getContent(), ANSWER_LIMIT);
            } else if (claudeCliSessionService.isRunning(agent.getId())) {
                task.status = "running";
                task.statusLabel = "running";
            } else {
                task.status = "open";
                task.statusLabel = "no answer";
            }

            tasks.add(task);
        }

        return tasks;
    }

    /**
     * One line of plain text: the loudest Markdown marks (code fences, backticks, bold/italic stars,
     * heading hashes, table pipes) dropped, whitespace collapsed, cut at a word boundary.
     */
    static String shorten(String content, int limit)
    {
        if (content == null) {
            return "";
        }

        String text = content.replaceAll("```[\\w-]*", " ").replaceAll("[`*|]", "").replaceAll("(?m)^\\s*#+\\s+", "")
                .replaceAll("\\s+", " ").trim();

        if (text.length() <= limit) {
            return text;
        }

        int cut = text.lastIndexOf(' ', limit);

        return text.substring(0, cut > limit / 2 ? cut : limit) + "…";
    }

    static String relative(Date date, Date now)
    {
        if (date == null) {
            return "";
        }

        long minutes = (now.getTime() - date.getTime()) / 60000;

        if (minutes < 1) {
            return "just now";
        }

        if (minutes < 60) {
            return minutes + " min ago";
        }

        if (minutes < 12 * 60) {
            return (minutes / 60) + " h ago";
        }

        Calendar then = Calendar.getInstance();
        then.setTime(date);
        Calendar today = Calendar.getInstance();
        today.setTime(now);
        String time = new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(date);

        if (then.get(Calendar.YEAR) == today.get(Calendar.YEAR) && then.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            return "today " + time;
        }

        today.add(Calendar.DAY_OF_YEAR, -1);

        if (then.get(Calendar.YEAR) == today.get(Calendar.YEAR) && then.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            return "yesterday " + time;
        }

        return new SimpleDateFormat("d MMM HH:mm", Locale.ENGLISH).format(date);
    }

    static String duration(int ms)
    {
        long seconds = Math.max(1, Math.round(ms / 1000.0));

        return seconds < 60 ? seconds + " s" : (seconds / 60) + " min " + (seconds % 60) + " s";
    }

    public static class RecentTask
    {
        private Long agentId;
        private String agentName;
        private String agentInitial;
        private String agentColor;
        private String senderIcon;
        private String senderLabel;
        private String text;
        private String when;
        private String whenExact;
        private String status;
        private String statusLabel;
        private String answer;

        public Long getAgentId() { return agentId; }
        public String getAgentName() { return agentName; }
        public String getAgentInitial() { return agentInitial; }
        public String getAgentColor() { return agentColor; }
        public String getSenderIcon() { return senderIcon; }
        public String getSenderLabel() { return senderLabel; }
        public String getText() { return text; }
        public String getWhen() { return when; }
        public String getWhenExact() { return whenExact; }
        public String getStatus() { return status; }
        public String getStatusLabel() { return statusLabel; }
        public String getAnswer() { return answer; }
    }
}
