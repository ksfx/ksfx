package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.AgentScheduleDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgentSchedule;
import ch.ksfx.services.scheduler.SchedulerService;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.Date;

/**
 * Overlay (AJAX-fragment) counterpart of {@link AgentScheduleController} - same underlying
 * DAO/SchedulerService, same Quartz job-lifecycle rules, but rendered as swappable fragments into
 * the chat page's own overlay instead of navigating away to a separate full page. Deliberately a
 * new controller under a distinct path (/agentic/schedules/**, plural) rather than adding fragment
 * variants onto the existing /agentic/schedule/** routes - keeps this new surface isolated, same
 * reasoning as AgenticFileBrowserController. The old full-page controller/templates are left
 * completely untouched and still work if linked to directly.
 */
@Controller
@RequestMapping("/agentic/schedules")
public class AgenticScheduleOverlayController
{
    private final AgentScheduleDAO agentScheduleDAO;
    private final AgentDAO agentDAO;
    private final SchedulerService schedulerService;

    public AgenticScheduleOverlayController(AgentScheduleDAO agentScheduleDAO, AgentDAO agentDAO, SchedulerService schedulerService)
    {
        this.agentScheduleDAO = agentScheduleDAO;
        this.agentDAO = agentDAO;
        this.schedulerService = schedulerService;
    }

    @GetMapping("/{agentId}/list")
    public String list(@PathVariable Long agentId, Model model)
    {
        return renderList(agentId, model);
    }

    @GetMapping("/{agentId}/edit")
    public String edit(@PathVariable Long agentId, @RequestParam(required = false) Long id, Model model)
    {
        AgentSchedule schedule = id != null ? agentScheduleDAO.getAgentScheduleForId(id) : new AgentSchedule();

        model.addAttribute("agent", agentDAO.getAgentForId(agentId));
        model.addAttribute("agentSchedule", schedule);

        return "agentic/schedule/agentic_schedule_overlay :: scheduleForm";
    }

    @PostMapping("/{agentId}/save")
    public String save(@PathVariable Long agentId,
                       @Valid @ModelAttribute("agentSchedule") AgentSchedule agentSchedule,
                       BindingResult bindingResult, Model model) throws SchedulerException
    {
        Agent agent = agentDAO.getAgentForId(agentId);
        agentSchedule.setAgent(agent);

        validateCron(agentSchedule, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("agent", agent);
            return "agentic/schedule/agentic_schedule_overlay :: scheduleForm";
        }

        if (agentSchedule.getId() == null) {
            agentSchedule.setCreatedAt(new Date());
        }

        agentScheduleDAO.saveOrUpdateAgentSchedule(agentSchedule);

        // Same delete-then-recreate rule as AgentScheduleController.submit(): job identity is
        // stable ("AgentSchedule"+id), so this one path covers enable, disable, and cron-expression
        // changes without needing to special-case any of them. deleteJob is a silent no-op if no
        // job with that key exists yet (new schedule, or was already disabled).
        schedulerService.deleteJob("AgentSchedule" + agentSchedule.getId(), "AgentSchedules");

        if (agentSchedule.getCronScheduleEnabled()) {
            schedulerService.scheduleAgentSchedule(agentSchedule);
        }

        return renderList(agentId, model);
    }

    @PostMapping("/{agentId}/delete/{id}")
    public String delete(@PathVariable Long agentId, @PathVariable Long id, Model model) throws SchedulerException
    {
        schedulerService.deleteJob("AgentSchedule" + id, "AgentSchedules");

        AgentSchedule schedule = agentScheduleDAO.getAgentScheduleForId(id);

        if (schedule != null) {
            agentScheduleDAO.deleteAgentSchedule(schedule);
        }

        return renderList(agentId, model);
    }

    private void validateCron(AgentSchedule agentSchedule, BindingResult bindingResult)
    {
        boolean blank = agentSchedule.getCronSchedule() == null || agentSchedule.getCronSchedule().trim().isEmpty();

        if (!blank) {
            try {
                new CronExpression(agentSchedule.getCronSchedule());
            } catch (Exception e) {
                bindingResult.rejectValue("cronSchedule", "agentSchedule.cronSchedule", "Cron Schedule not valid");
            }
        } else if (agentSchedule.getCronScheduleEnabled()) {
            bindingResult.rejectValue("cronSchedule", "agentSchedule.cronSchedule", "Cron Schedule required when enabled");
        }
    }

    private String renderList(Long agentId, Model model)
    {
        model.addAttribute("agent", agentDAO.getAgentForId(agentId));
        model.addAttribute("schedules", agentScheduleDAO.getSchedulesForAgent(agentId));
        model.addAttribute("schedulerService", schedulerService);

        return "agentic/schedule/agentic_schedule_overlay :: scheduleList";
    }
}
