package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentMessageDAO;
import ch.ksfx.dao.AgenticConfigDAO;
import ch.ksfx.model.AgentMessage;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.services.SystemEnvironment;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/agentic/config")
public class AgenticConfigController
{
    private final AgenticConfigDAO agenticConfigDAO;
    private final AgentMessageDAO agentMessageDAO;
    private final SystemEnvironment systemEnvironment;

    public AgenticConfigController(AgenticConfigDAO agenticConfigDAO, AgentMessageDAO agentMessageDAO, SystemEnvironment systemEnvironment)
    {
        this.agenticConfigDAO = agenticConfigDAO;
        this.agentMessageDAO = agentMessageDAO;
        this.systemEnvironment = systemEnvironment;
    }

    @GetMapping("/")
    public String index(@RequestParam(value = "statsDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statsDate, Model model)
    {
        AgenticConfig agenticConfig = agenticConfigDAO.getAgenticConfig();

        if (agenticConfig == null) {
            agenticConfig = new AgenticConfig();
        }

        if (agenticConfig.getWorkspaceRoot() == null || agenticConfig.getWorkspaceRoot().trim().isEmpty()) {
            agenticConfig.setWorkspaceRoot(systemEnvironment.getApplicationHomeDirectoryPath() + SystemEnvironment.FILE_SEPARATOR + "agentic-workspaces");
        }

        model.addAttribute("agenticConfig", agenticConfig);

        addUsageStats(model, statsDate != null ? statsDate : LocalDate.now());

        return "agentic/config/agentic_config";
    }

    @PostMapping("/")
    public String save(@Valid @ModelAttribute("agenticConfig") AgenticConfig agenticConfig, BindingResult bindingResult)
    {
        if (bindingResult.hasErrors()) {
            return "agentic/config/agentic_config";
        }

        // agenticConfig is a fresh object built only from submitted form fields - the
        // claudeRateLimit* snapshot fields are read-only/system-captured, never form inputs, so
        // without this they'd be silently wiped to null on every settings save.
        AgenticConfig previous = agenticConfigDAO.getAgenticConfig();

        if (previous != null) {
            agenticConfig.setClaudeRateLimitStatus(previous.getClaudeRateLimitStatus());
            agenticConfig.setClaudeRateLimitType(previous.getClaudeRateLimitType());
            agenticConfig.setClaudeRateLimitResetsAt(previous.getClaudeRateLimitResetsAt());
            agenticConfig.setClaudeRateLimitOverageStatus(previous.getClaudeRateLimitOverageStatus());
            agenticConfig.setClaudeRateLimitOverageResetsAt(previous.getClaudeRateLimitOverageResetsAt());
            agenticConfig.setClaudeRateLimitUsingOverage(previous.getClaudeRateLimitUsingOverage());
            agenticConfig.setClaudeRateLimitUpdatedAt(previous.getClaudeRateLimitUpdatedAt());
        }

        agenticConfigDAO.saveOrUpdateAgenticConfig(agenticConfig);

        return "redirect:/agentic/config/";
    }

    /**
     * Aggregates per-turn usage (see ClaudeCliSessionService/AgentMessage) into per-agent and
     * grand totals for display, twice: once all-time, once scoped to statsDate (defaults to today,
     * see index()) so you can see "what did today/this day actually cost" separately from the
     * running total. Computed in Java rather than a DB-level aggregate query - the dataset is small
     * for a personal tool, and this avoids Ebean aggregate-query syntax for a one-off report; both
     * aggregations reuse the same single DB fetch rather than querying twice.
     */
    private void addUsageStats(Model model, LocalDate statsDate)
    {
        List<AgentMessage> messages = agentMessageDAO.getAssistantMessagesWithUsage();

        UsageAggregate allTime = aggregate(messages);

        model.addAttribute("usageByAgent", allTime.usageByAgent.values());
        model.addAttribute("totalInputTokens", allTime.totalInputTokens);
        model.addAttribute("totalOutputTokens", allTime.totalOutputTokens);
        model.addAttribute("totalCacheCreationTokens", allTime.totalCacheCreationTokens);
        model.addAttribute("totalCacheReadTokens", allTime.totalCacheReadTokens);
        model.addAttribute("totalTurns", allTime.totalTurns);

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

            summary.turnCount++;
            summary.inputTokens += inputTokens;
            summary.outputTokens += outputTokens;
            summary.cacheCreationTokens += cacheCreationTokens;
            summary.cacheReadTokens += cacheReadTokens;

            result.totalInputTokens += inputTokens;
            result.totalOutputTokens += outputTokens;
            result.totalCacheCreationTokens += cacheCreationTokens;
            result.totalCacheReadTokens += cacheReadTokens;
            result.totalTurns++;
        }

        return result;
    }

    private static class UsageAggregate
    {
        Map<Long, UsageSummary> usageByAgent = new LinkedHashMap<>();
        long totalInputTokens;
        long totalOutputTokens;
        long totalCacheCreationTokens;
        long totalCacheReadTokens;
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
    }
}
