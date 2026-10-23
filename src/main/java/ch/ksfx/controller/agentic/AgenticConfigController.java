package ch.ksfx.controller.agentic;

import ch.ksfx.services.agentic.AgenticUsageStatsService;
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
    private final AgenticUsageStatsService usageStatsService;
    private final SystemEnvironment systemEnvironment;

    public AgenticConfigController(AgenticConfigDAO agenticConfigDAO, AgenticUsageStatsService usageStatsService, SystemEnvironment systemEnvironment)
    {
        this.agenticConfigDAO = agenticConfigDAO;
        this.usageStatsService = usageStatsService;
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

        usageStatsService.addUsageStats(model, statsDate != null ? statsDate : LocalDate.now(), null);

        return "agentic/config/agentic_config";
    }

    @PostMapping("/")
    public String save(@Valid @ModelAttribute("agenticConfig") AgenticConfig agenticConfig, BindingResult bindingResult)
    {
        if (bindingResult.hasErrors()) {
            return "agentic/config/agentic_config";
        }

        agenticConfigDAO.saveOrUpdateAgenticConfig(agenticConfig);

        return "redirect:/agentic/config/";
    }

}
