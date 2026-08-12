package ch.ksfx.controller.agentic;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.AgentScheduleDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgentSchedule;
import ch.ksfx.services.scheduler.SchedulerService;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Self-service scheduling API for agents themselves - called via curl from inside the headless
 * Claude CLI's Bash tool (see ClaudeCliSessionService.buildAppendedSystemPrompt), not from a
 * browser. No Spring Security session/CSRF involved (path is permitAll(), see WebSecurityConfig) -
 * auth is a plain per-agent bearer token checked inline in {@link #authenticate}.
 *
 * Every route derives "which agent" exclusively from the authenticated token, never from a
 * client-supplied id, and every read/update/delete re-checks that the target AgentSchedule
 * actually belongs to that agent before touching it - a leaked/guessed token for one agent must
 * never be usable to read or modify another agent's schedules.
 */
@RestController
@RequestMapping("/agentic/api/schedule")
public class AgentScheduleApiController
{
    private final AgentScheduleDAO agentScheduleDAO;
    private final AgentDAO agentDAO;
    private final SchedulerService schedulerService;

    public AgentScheduleApiController(AgentScheduleDAO agentScheduleDAO, AgentDAO agentDAO, SchedulerService schedulerService)
    {
        this.agentScheduleDAO = agentScheduleDAO;
        this.agentDAO = agentDAO;
        this.schedulerService = schedulerService;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request)
    {
        Agent agent = authenticate(request);

        if (agent == null) {
            return unauthorized();
        }

        List<AgentScheduleApiDto> dtos = agentScheduleDAO.getSchedulesForAgent(agent.getId()).stream()
                .map(AgentScheduleApiDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<?> create(HttpServletRequest request, @RequestBody AgentScheduleApiDto body) throws SchedulerException
    {
        Agent agent = authenticate(request);

        if (agent == null) {
            return unauthorized();
        }

        String cronError = validateCron(body);
        if (cronError != null) {
            return ResponseEntity.badRequest().body(errorBody(cronError));
        }

        AgentSchedule schedule = new AgentSchedule();
        schedule.setAgent(agent); // never from the request body - only the authenticated token decides ownership
        schedule.setName(body.name);
        schedule.setTaskPrompt(body.taskPrompt);
        schedule.setCronSchedule(body.cronSchedule);
        schedule.setCronScheduleEnabled(body.cronScheduleEnabled);
        schedule.setCreatedAt(new Date());

        agentScheduleDAO.saveOrUpdateAgentSchedule(schedule);

        schedulerService.deleteJob("AgentSchedule" + schedule.getId(), "AgentSchedules");
        if (schedule.getCronScheduleEnabled()) {
            schedulerService.scheduleAgentSchedule(schedule);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(AgentScheduleApiDto.from(schedule));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(HttpServletRequest request, @PathVariable Long id, @RequestBody AgentScheduleApiDto body) throws SchedulerException
    {
        Agent agent = authenticate(request);

        if (agent == null) {
            return unauthorized();
        }

        AgentSchedule schedule = agentScheduleDAO.getAgentScheduleForId(id);

        if (!owns(schedule, agent)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("Not found"));
        }

        String cronError = validateCron(body);
        if (cronError != null) {
            return ResponseEntity.badRequest().body(errorBody(cronError));
        }

        schedule.setName(body.name);
        schedule.setTaskPrompt(body.taskPrompt);
        schedule.setCronSchedule(body.cronSchedule);
        schedule.setCronScheduleEnabled(body.cronScheduleEnabled);

        agentScheduleDAO.saveOrUpdateAgentSchedule(schedule);

        schedulerService.deleteJob("AgentSchedule" + schedule.getId(), "AgentSchedules");
        if (schedule.getCronScheduleEnabled()) {
            schedulerService.scheduleAgentSchedule(schedule);
        }

        return ResponseEntity.ok(AgentScheduleApiDto.from(schedule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(HttpServletRequest request, @PathVariable Long id) throws SchedulerException
    {
        Agent agent = authenticate(request);

        if (agent == null) {
            return unauthorized();
        }

        AgentSchedule schedule = agentScheduleDAO.getAgentScheduleForId(id);

        if (!owns(schedule, agent)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("Not found"));
        }

        schedulerService.deleteJob("AgentSchedule" + id, "AgentSchedules");
        agentScheduleDAO.deleteAgentSchedule(schedule);

        return ResponseEntity.noContent().build();
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

    private ResponseEntity<?> unauthorized()
    {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Unauthorized"));
    }

    private boolean owns(AgentSchedule schedule, Agent agent)
    {
        return schedule != null && schedule.getAgent() != null && schedule.getAgent().getId().equals(agent.getId());
    }

    private String validateCron(AgentScheduleApiDto body)
    {
        boolean blank = body.cronSchedule == null || body.cronSchedule.trim().isEmpty();

        if (blank) {
            return body.cronScheduleEnabled ? "cronSchedule required when cronScheduleEnabled=true" : null;
        }

        try {
            new CronExpression(body.cronSchedule);
            return null;
        } catch (Exception e) {
            return "Invalid Quartz cron expression: " + e.getMessage();
        }
    }

    private Map<String, String> errorBody(String message)
    {
        return Collections.singletonMap("error", message);
    }

    /**
     * Request/response shape for this API - deliberately not the raw AgentSchedule entity, whose
     * @ManyToOne agent back-reference would otherwise serialize the full Agent (including its own
     * apiToken) into every response.
     */
    private static class AgentScheduleApiDto
    {
        public Long id;
        public String name;
        public String taskPrompt;
        public String cronSchedule;
        public boolean cronScheduleEnabled;
        public Date lastRunAt;
        public String lastRunStatus;
        public String lastRunError;
        public Date createdAt;

        static AgentScheduleApiDto from(AgentSchedule schedule)
        {
            AgentScheduleApiDto dto = new AgentScheduleApiDto();
            dto.id = schedule.getId();
            dto.name = schedule.getName();
            dto.taskPrompt = schedule.getTaskPrompt();
            dto.cronSchedule = schedule.getCronSchedule();
            dto.cronScheduleEnabled = schedule.getCronScheduleEnabled();
            dto.lastRunAt = schedule.getLastRunAt();
            dto.lastRunStatus = schedule.getLastRunStatus();
            dto.lastRunError = schedule.getLastRunError();
            dto.createdAt = schedule.getCreatedAt();
            return dto;
        }
    }
}
