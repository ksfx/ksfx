package ch.ksfx.services.agentic;

import ch.ksfx.dao.AgentScheduleDAO;
import ch.ksfx.model.AgentSchedule;
import ch.ksfx.services.systemlogger.SystemLogger;
import ch.ksfx.util.StacktraceUtil;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;

/**
 * Fires a scheduled {@link AgentSchedule}: sends its task prompt to the agent exactly like a
 * manual chat message (see {@link ClaudeCliSessionService#runScheduledTurn}), then records the
 * outcome back onto the schedule row for the "Aufgaben" list. Plain no-arg-constructor Job -
 * KSFX has no custom Quartz JobFactory anywhere, so dependencies arrive via JobDataMap (populated
 * by SchedulerService.scheduleAgentSchedule), matching ActivityInstanceJob's exact convention.
 */
public class AgentScheduleJob implements Job
{
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();

        Long agentScheduleId = (Long) jobDataMap.get("agentScheduleId");
        AgentScheduleDAO agentScheduleDAO = (AgentScheduleDAO) jobDataMap.get("agentScheduleDao");
        ClaudeCliSessionService claudeCliSessionService = (ClaudeCliSessionService) jobDataMap.get("claudeCliSessionService");
        SystemLogger systemLogger = (SystemLogger) jobDataMap.get("systemLogger");

        AgentSchedule agentSchedule = agentScheduleDAO.getAgentScheduleForId(agentScheduleId);

        if (agentSchedule == null || !agentSchedule.getCronScheduleEnabled()) {
            return;
        }

        try {
            String result = claudeCliSessionService.runScheduledTurn(agentSchedule.getAgent().getId(), agentSchedule.getTaskPrompt());

            agentSchedule.setLastRunAt(new Date());

            if (result == null) {
                agentSchedule.setLastRunStatus("SUCCESS");
                agentSchedule.setLastRunError(null);
            } else if (ClaudeCliSessionService.SKIPPED_RESULT.equals(result)) {
                agentSchedule.setLastRunStatus("SKIPPED");
                agentSchedule.setLastRunError(null);
            } else {
                agentSchedule.setLastRunStatus("FAILED");
                agentSchedule.setLastRunError(result);
            }
        } catch (Exception e) {
            systemLogger.logMessage("AGENTIC", "Scheduled agent turn failed for schedule " + agentScheduleId, e);

            agentSchedule.setLastRunAt(new Date());
            agentSchedule.setLastRunStatus("FAILED");
            agentSchedule.setLastRunError(e.getMessage() + StacktraceUtil.getStackTrace(e));
        }

        agentScheduleDAO.saveOrUpdateAgentSchedule(agentSchedule);
    }
}
