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
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;

@Controller
@RequestMapping("/agentic/schedule")
public class AgentScheduleController
{
    private final AgentScheduleDAO agentScheduleDAO;
    private final AgentDAO agentDAO;
    private final SchedulerService schedulerService;

    public AgentScheduleController(AgentScheduleDAO agentScheduleDAO, AgentDAO agentDAO, SchedulerService schedulerService)
    {
        this.agentScheduleDAO = agentScheduleDAO;
        this.agentDAO = agentDAO;
        this.schedulerService = schedulerService;
    }

    @GetMapping("/{agentId}/")
    public String index(@PathVariable(value = "agentId") Long agentId, Model model)
    {
        model.addAttribute("agent", agentDAO.getAgentForId(agentId));
        model.addAttribute("schedules", agentScheduleDAO.getSchedulesForAgent(agentId));
        model.addAttribute("schedulerService", schedulerService);

        return "agentic/schedule/agent_schedule";
    }

    @GetMapping({"/{agentId}/edit", "/{agentId}/edit/{id}"})
    public String edit(@PathVariable(value = "agentId") Long agentId,
                        @PathVariable(value = "id", required = false) Long id, Model model)
    {
        AgentSchedule schedule = id != null ? agentScheduleDAO.getAgentScheduleForId(id) : new AgentSchedule();

        model.addAttribute("agent", agentDAO.getAgentForId(agentId));
        model.addAttribute("agentSchedule", schedule);

        return "agentic/schedule/agent_schedule_edit";
    }

    @PostMapping({"/{agentId}/edit", "/{agentId}/edit/{id}"})
    public String submit(@PathVariable(value = "agentId") Long agentId,
                          @PathVariable(value = "id", required = false) Long id,
                          @Valid @ModelAttribute("agentSchedule") AgentSchedule agentSchedule,
                          BindingResult bindingResult, Model model) throws SchedulerException
    {
        Agent agent = agentDAO.getAgentForId(agentId);
        agentSchedule.setAgent(agent);

        validateCron(agentSchedule, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("agent", agent);
            return "agentic/schedule/agent_schedule_edit";
        }

        boolean isNew = agentSchedule.getId() == null;

        if (isNew) {
            agentSchedule.setCreatedAt(new Date());
        }

        agentScheduleDAO.saveOrUpdateAgentSchedule(agentSchedule);

        // Job identity is stable ("AgentSchedule"+id): delete-then-recreate unconditionally covers
        // enable, disable, and cron-expression changes in one path. deleteJob is a silent no-op if
        // no job with that key exists yet (new schedule, or was already disabled).
        schedulerService.deleteJob("AgentSchedule" + agentSchedule.getId(), "AgentSchedules");

        if (agentSchedule.getCronScheduleEnabled()) {
            schedulerService.scheduleAgentSchedule(agentSchedule);
        }

        return "redirect:/agentic/schedule/" + agentId + "/";
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

    @GetMapping("/{agentId}/delete/{id}")
    public String delete(@PathVariable(value = "agentId") Long agentId,
                          @PathVariable(value = "id") Long id) throws SchedulerException
    {
        schedulerService.deleteJob("AgentSchedule" + id, "AgentSchedules");
        agentScheduleDAO.deleteAgentSchedule(agentScheduleDAO.getAgentScheduleForId(id));

        return "redirect:/agentic/schedule/" + agentId + "/";
    }
}
