package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentMessageDAO;
import ch.ksfx.dao.AgenticConfigDAO;
import ch.ksfx.model.AgentMessage;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.services.SystemEnvironment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public String index(Model model)
    {
        AgenticConfig agenticConfig = agenticConfigDAO.getAgenticConfig();

        if (agenticConfig == null) {
            agenticConfig = new AgenticConfig();
        }

        if (agenticConfig.getWorkspaceRoot() == null || agenticConfig.getWorkspaceRoot().trim().isEmpty()) {
            agenticConfig.setWorkspaceRoot(systemEnvironment.getApplicationHomeDirectoryPath() + SystemEnvironment.FILE_SEPARATOR + "agentic-workspaces");
        }

        model.addAttribute("agenticConfig", agenticConfig);

        addUsageStats(model);

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
     * grand totals for display. Computed in Java rather than a DB-level aggregate query - the
     * dataset is small for a personal tool, and this avoids Ebean aggregate-query syntax for a
     * one-off report.
     */
    private void addUsageStats(Model model)
    {
        List<AgentMessage> messages = agentMessageDAO.getAssistantMessagesWithUsage();

        Map<Long, UsageSummary> usageByAgent = new LinkedHashMap<>();
        long totalInputTokens = 0;
        long totalOutputTokens = 0;

        for (AgentMessage message : messages) {
            Long agentId = message.getAgent().getId();
            UsageSummary summary = usageByAgent.get(agentId);

            if (summary == null) {
                summary = new UsageSummary();
                summary.agentName = message.getAgent().getName();
                usageByAgent.put(agentId, summary);
            }

            summary.turnCount++;
            summary.inputTokens += message.getInputTokens();
            summary.outputTokens += message.getOutputTokens();

            totalInputTokens += message.getInputTokens();
            totalOutputTokens += message.getOutputTokens();
        }

        model.addAttribute("usageByAgent", usageByAgent.values());
        model.addAttribute("totalInputTokens", totalInputTokens);
        model.addAttribute("totalOutputTokens", totalOutputTokens);
        model.addAttribute("totalTurns", messages.size());
    }

    public static class UsageSummary
    {
        public String agentName;
        public int turnCount;
        public long inputTokens;
        public long outputTokens;
    }
}
