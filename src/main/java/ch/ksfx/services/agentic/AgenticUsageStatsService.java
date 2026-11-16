package ch.ksfx.services.agentic;

import ch.ksfx.dao.AgentMessageDAO;
import ch.ksfx.model.AgentMessage;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates per-turn usage (see ClaudeCliSessionService/AgentMessage) into per-agent and grand
 * totals for display, twice: once all-time, once scoped to a day. Extracted from
 * AgenticConfigController when the per-project settings page (concept "Agentic settings per
 * project") gained the same view scoped to one project - same aggregation, same model attribute
 * names (so both templates share the markup), just an optional project filter on the fetch.
 * Computed in Java rather than a DB-level aggregate query - the dataset is small for a personal
 * tool, and both aggregations reuse the same single DB fetch rather than querying twice.
 */
@Service
public class AgenticUsageStatsService
{
    private final AgentMessageDAO agentMessageDAO;

    public AgenticUsageStatsService(AgentMessageDAO agentMessageDAO)
    {
        this.agentMessageDAO = agentMessageDAO;
    }

    /** projectId null = instance-wide (the Admin -> Agentic view), otherwise that project's agents only. */
    public void addUsageStats(Model model, LocalDate statsDate, Long projectId)
    {
        List<AgentMessage> messages = projectId == null
                ? agentMessageDAO.getAssistantMessagesWithUsage()
                : agentMessageDAO.getAssistantMessagesWithUsageForProject(projectId);

        UsageAggregate allTime = aggregate(messages);

        model.addAttribute("usageByAgent", allTime.usageByAgent.values());
        model.addAttribute("totalInputTokens", allTime.totalInputTokens);
        model.addAttribute("totalOutputTokens", allTime.totalOutputTokens);
        model.addAttribute("totalCacheCreationTokens", allTime.totalCacheCreationTokens);
        model.addAttribute("totalCacheReadTokens", allTime.totalCacheReadTokens);
        model.addAttribute("totalTurns", allTime.totalTurns);
        model.addAttribute("totalVoiceCleanupInputTokens", allTime.totalVoiceCleanupInputTokens);
        model.addAttribute("totalVoiceCleanupOutputTokens", allTime.totalVoiceCleanupOutputTokens);

        List<AgentMessage> dayMessages = messages.stream()
                .filter(m -> toLocalDate(m.getCreatedAt()).equals(statsDate))
                .collect(Collectors.toList());

        UsageAggregate day = aggregate(dayMessages);

        model.addAttribute("statsDate", statsDate);
        model.addAttribute("usageByAgentForDay", day.usageByAgent.values());
        model.addAttribute("totalInputTokensForDay", day.totalInputTokens);
        model.addAttribute("totalOutputTokensForDay", day.totalOutputTokens);
        model.addAttribute("totalCacheCreationTokensForDay", day.totalCacheCreationTokens);
        model.addAttribute("totalCacheReadTokensForDay", day.totalCacheReadTokens);
        model.addAttribute("totalTurnsForDay", day.totalTurns);
        model.addAttribute("totalVoiceCleanupInputTokensForDay", day.totalVoiceCleanupInputTokens);
        model.addAttribute("totalVoiceCleanupOutputTokensForDay", day.totalVoiceCleanupOutputTokens);
    }

    /**
     * Token usage per access (account) of a project - all-time and today - for the access cards on
     * the project's Agentic Settings page. Keyed by access id; answers whose access was deleted
     * (or that ran before accesses were recorded) are not counted here, only in the totals.
     */
    public java.util.Map<Long, AccessUsage> usageByAccess(Long projectId)
    {
        java.util.Map<Long, AccessUsage> result = new java.util.HashMap<>();
        LocalDate today = LocalDate.now();

        for (AgentMessage message : agentMessageDAO.getAssistantMessagesWithUsageForProject(projectId)) {
            if (message.getAgenticAccess() == null) {
                continue;
            }

            AccessUsage usage = result.computeIfAbsent(message.getAgenticAccess().getId(), id -> new AccessUsage());
            long tokens = (message.getInputTokens() != null ? message.getInputTokens() : 0) + (message.getOutputTokens() != null ? message.getOutputTokens() : 0);
            boolean isToday = toLocalDate(message.getCreatedAt()).equals(today);

            usage.tokensAllTime += tokens;
            usage.tokensToday += isToday ? tokens : 0;

            if (!message.getInternal()) {
                usage.turnsAllTime++;
                usage.turnsToday += isToday ? 1 : 0;
            }
        }

        return result;
    }

    public static class AccessUsage
    {
        public long tokensAllTime;
        public long tokensToday;
        public int turnsAllTime;
        public int turnsToday;

        public long getTokensAllTime() { return tokensAllTime; }
        public long getTokensToday() { return tokensToday; }
        public int getTurnsAllTime() { return turnsAllTime; }
        public int getTurnsToday() { return turnsToday; }
    }

    private LocalDate toLocalDate(java.util.Date date)
    {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private UsageAggregate aggregate(List<AgentMessage> messages)
    {
        UsageAggregate result = new UsageAggregate();

        for (AgentMessage message : messages) {
            Long agentId = message.getAgent().getId();
            UsageSummary summary = result.usageByAgent.get(agentId);

            if (summary == null) {
                summary = new UsageSummary();
                summary.agentName = message.getAgent().getName();
                result.usageByAgent.put(agentId, summary);
            }

            // Integer fields can in principle be null (result event omitted a usage sub-field) even
            // though inputTokens - the field getAssistantMessagesWithUsage() filters on - isn't.
            // Both cache figures are sub-categories of the *input* side of the request (prompt
            // caching), never the output - cacheCreationTokens is prompt content newly written to
            // the cache this turn, cacheReadTokens is prompt content served back out of an existing
            // cache entry instead of being reprocessed. Naming them "in"/"out" would misleadingly
            // pair them with the Input/Output columns, as if output could be cached - it can't.
            int inputTokens = message.getInputTokens() != null ? message.getInputTokens() : 0;
            int outputTokens = message.getOutputTokens() != null ? message.getOutputTokens() : 0;
            int cacheCreationTokens = message.getCacheCreationInputTokens() != null ? message.getCacheCreationInputTokens() : 0;
            int cacheReadTokens = message.getCacheReadInputTokens() != null ? message.getCacheReadInputTokens() : 0;

            // Voice-cleanup calls (see ClaudeCliSessionService.persistVoiceCleanupUsage) are
            // persisted as internal=true ASSISTANT messages purely to get their tokens counted here
            // - folded into the same totals as regular conversation usage (so the grand total is
            // accurate) but also broken out separately (so it's visible, not just silently merged
            // in) rather than counted as a "turn" of its own.
            boolean isVoiceCleanup = message.getInternal();

            if (!isVoiceCleanup) {
                summary.turnCount++;
                result.totalTurns++;
            } else {
                summary.voiceCleanupInputTokens += inputTokens;
                summary.voiceCleanupOutputTokens += outputTokens;
                result.totalVoiceCleanupInputTokens += inputTokens;
                result.totalVoiceCleanupOutputTokens += outputTokens;

                // completionSource is only ever "cli"/"api" (see VoiceCompletionResult.getSource())
                // but a message from before that field existed would have it null - counted as
                // neither rather than guessed, so historical rows don't silently skew this.
                if ("api".equals(message.getCompletionSource())) {
                    summary.voiceCleanupApiCount++;
                } else if ("cli".equals(message.getCompletionSource())) {
                    summary.voiceCleanupCliCount++;
                }
            }

            summary.inputTokens += inputTokens;
            summary.outputTokens += outputTokens;
            summary.cacheCreationTokens += cacheCreationTokens;
            summary.cacheReadTokens += cacheReadTokens;

            result.totalInputTokens += inputTokens;
            result.totalOutputTokens += outputTokens;
            result.totalCacheCreationTokens += cacheCreationTokens;
            result.totalCacheReadTokens += cacheReadTokens;
        }

        return result;
    }

    static class UsageAggregate
    {
        Map<Long, UsageSummary> usageByAgent = new LinkedHashMap<>();
        long totalInputTokens;
        long totalOutputTokens;
        long totalCacheCreationTokens;
        long totalCacheReadTokens;
        long totalVoiceCleanupInputTokens;
        long totalVoiceCleanupOutputTokens;
        int totalTurns;
    }

    public static class UsageSummary
    {
        public String agentName;
        public int turnCount;
        public long inputTokens;
        public long outputTokens;
        public long cacheCreationTokens;
        public long cacheReadTokens;
        public long voiceCleanupInputTokens;
        public long voiceCleanupOutputTokens;
        public int voiceCleanupCliCount;
        public int voiceCleanupApiCount;
    }
}
