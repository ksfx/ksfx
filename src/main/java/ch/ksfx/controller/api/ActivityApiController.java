package ch.ksfx.controller.api;

import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.ActivityApprovalStrategy;
import ch.ksfx.model.activity.ActivityCategory;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.services.activity.ActivityInstanceRunner;
import ch.ksfx.services.git.ActivityGitRepositoryService;
import ch.ksfx.services.scheduler.SchedulerService;
import ch.ksfx.services.systemlogger.SystemLogger;
import com.fasterxml.jackson.annotation.JsonInclude;
import groovy.lang.GroovyClassLoader;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * External /api/** CRUDL surface for Activities (the existing scheduled Cron/Groovy job model
 * under {@link Activity}, otherwise managed via /activity in the UI), plus a run-trigger endpoint.
 * Authenticated by ch.ksfx.services.security.ApiTokenAuthenticationFilter /
 * ApiClientAuthenticationProvider - unlike /agentic/api/**, the caller is resolved through Spring
 * Security proper, not an inline check.
 *
 * Unlike the MVC /activity/** controller, update and delete here keep the live Quartz job in sync
 * with the Activity row (delete removes the job before the row; update deletes then conditionally
 * re-schedules) - the MVC controller has never done either consistently (schedule on/off is a
 * separate pair of actions there, and delete doesn't touch Quartz at all), but there's no reason
 * for a new API to repeat that gap.
 *
 * When Git sync is active, this also keeps the Git-backed source in lockstep with the DB row
 * exactly like the MVC controller does (same slug/write/rename/delete calls against
 * {@link ActivityGitRepositoryService}) - API and GUI must produce the same end state for the
 * same edit, otherwise callers using one and humans using the other silently diverge.
 */
@RestController
@RequestMapping("/api/activities")
public class ActivityApiController
{
    private final ActivityDAO activityDAO;
    private final ActivityInstanceDAO activityInstanceDAO;
    private final ActivityInstanceRunner activityInstanceRunner;
    private final SchedulerService schedulerService;
    private final ServiceProvider serviceProvider;
    private final ActivityGitRepositoryService activityGitRepositoryService;
    private final SystemLogger systemLogger;

    public ActivityApiController(ActivityDAO activityDAO,
                                  ActivityInstanceDAO activityInstanceDAO,
                                  ActivityInstanceRunner activityInstanceRunner,
                                  SchedulerService schedulerService,
                                  ServiceProvider serviceProvider,
                                  ActivityGitRepositoryService activityGitRepositoryService,
                                  SystemLogger systemLogger)
    {
        this.activityDAO = activityDAO;
        this.activityInstanceDAO = activityInstanceDAO;
        this.activityInstanceRunner = activityInstanceRunner;
        this.schedulerService = schedulerService;
        this.serviceProvider = serviceProvider;
        this.activityGitRepositoryService = activityGitRepositoryService;
        this.systemLogger = systemLogger;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) Long activityCategoryId)
    {
        List<Activity> activities;

        if (activityCategoryId != null) {
            ActivityCategory category = activityDAO.getActivityCategoryForId(activityCategoryId);

            if (category == null) {
                return ResponseEntity.badRequest().body(errorBody("No activity category with id " + activityCategoryId));
            }

            activities = activityDAO.getAllActivitiesForActivityCategory(category);
        } else {
            activities = activityDAO.getAllActivities();
        }

        return ResponseEntity.ok(activities.stream().map(ActivityApiDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id)
    {
        Activity activity = activityDAO.getActivityForId(id);

        if (activity == null) {
            return notFound();
        }

        return ResponseEntity.ok(ActivityApiDto.fromDetailed(activity, resolveGroovyCode(activity.getGitPath(), activity.getGroovyCode())));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ActivityApiRequest body)
    {
        if (body.name == null || body.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("name is required"));
        }

        String validationError = validate(body.groovyCode, body.cronSchedule);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(errorBody(validationError));
        }

        Activity activity = new Activity();
        activity.setName(body.name);
        activity.setCronSchedule(body.cronSchedule);
        activity.setCronScheduleEnabled(body.cronScheduleEnabled);
        activity.setGroovyCode(body.groovyCode);

        if (body.activityCategoryId != null) {
            ActivityCategory category = activityDAO.getActivityCategoryForId(body.activityCategoryId);

            if (category == null) {
                return ResponseEntity.badRequest().body(errorBody("No activity category with id " + body.activityCategoryId));
            }

            activity.setActivityCategory(category);
        }

        if (body.activityApprovalStrategyId != null) {
            ActivityApprovalStrategy strategy = activityDAO.getActivityApprovalStrategyForId(body.activityApprovalStrategyId);

            if (strategy == null) {
                return ResponseEntity.badRequest().body(errorBody("No activity approval strategy with id " + body.activityApprovalStrategyId));
            }

            activity.setActivityApprovalStrategy(strategy);
        }

        if (activityGitRepositoryService.isActive() && activity.getGroovyCode() != null) {
            try {
                String slug = activityGitRepositoryService.uniqueSlug(
                        activityGitRepositoryService.slugify(activity.getName()),
                        ActivityGitRepositoryService.ACTIVITIES_DIRECTORY);
                activity.setGitPath(ActivityGitRepositoryService.ACTIVITIES_DIRECTORY + "/" + slug + ".groovy");
                activityGitRepositoryService.writeActivitySource(activity.getGitPath(), activity.getGroovyCode(), "Create activity: " + activity.getName());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody("Could not write to Git repository: " + e.getMessage()));
            }
        }

        activityDAO.saveOrUpdateActivity(activity);

        if (activity.getCronScheduleEnabled()) {
            schedulerService.scheduleActivity(activity);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ActivityApiDto.from(activity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ActivityApiRequest body) throws SchedulerException
    {
        Activity activity = activityDAO.getActivityForId(id);

        if (activity == null) {
            return notFound();
        }

        if (body.name == null || body.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("name is required"));
        }

        // groovyCode is only validated/overwritten if actually present in the request body - an
        // update that only e.g. toggles cronScheduleEnabled shouldn't have to resend the script,
        // and shouldn't have it wiped either.
        if (body.groovyCode != null) {
            String validationError = validate(body.groovyCode, body.cronSchedule);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(errorBody(validationError));
            }

            activity.setGroovyCode(body.groovyCode);
        } else {
            String validationError = validate(null, body.cronSchedule);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(errorBody(validationError));
            }
        }

        if (body.activityCategoryId != null) {
            ActivityCategory category = activityDAO.getActivityCategoryForId(body.activityCategoryId);

            if (category == null) {
                return ResponseEntity.badRequest().body(errorBody("No activity category with id " + body.activityCategoryId));
            }

            activity.setActivityCategory(category);
        }

        if (body.activityApprovalStrategyId != null) {
            ActivityApprovalStrategy strategy = activityDAO.getActivityApprovalStrategyForId(body.activityApprovalStrategyId);

            if (strategy == null) {
                return ResponseEntity.badRequest().body(errorBody("No activity approval strategy with id " + body.activityApprovalStrategyId));
            }

            activity.setActivityApprovalStrategy(strategy);
        }

        // requiredActivities/triggerActivities are deliberately never touched here - activity was
        // loaded from the DB above, not built fresh, so anything not explicitly reassigned above or
        // below just keeps its current persisted value. gitPath itself is managed below, mirroring
        // the MVC controller instead of being left untouched.
        activity.setName(body.name);
        activity.setCronSchedule(body.cronSchedule);
        activity.setCronScheduleEnabled(body.cronScheduleEnabled);

        if (activityGitRepositoryService.isActive() && activity.getGroovyCode() != null) {
            try {
                if (activity.getGitPath() == null) {
                    String slug = activityGitRepositoryService.uniqueSlug(
                            activityGitRepositoryService.slugify(activity.getName()),
                            ActivityGitRepositoryService.ACTIVITIES_DIRECTORY);
                    activity.setGitPath(ActivityGitRepositoryService.ACTIVITIES_DIRECTORY + "/" + slug + ".groovy");
                    activityGitRepositoryService.writeActivitySource(activity.getGitPath(), activity.getGroovyCode(), "Create activity: " + activity.getName());
                } else {
                    Set<String> siblingPaths = new HashSet<>();
                    for (Activity other : activityDAO.getAllActivities()) {
                        if (!other.getId().equals(activity.getId()) && other.getGitPath() != null) {
                            siblingPaths.add(other.getGitPath());
                        }
                    }

                    String desiredPath = activityGitRepositoryService.desiredPath(ActivityGitRepositoryService.ACTIVITIES_DIRECTORY, activity.getName(), activity.getGitPath(), siblingPaths);

                    if (!desiredPath.equals(activity.getGitPath())) {
                        activityGitRepositoryService.renameAndWriteActivitySource(activity.getGitPath(), desiredPath, activity.getGroovyCode(), "Rename activity: " + activity.getName());
                        activity.setGitPath(desiredPath);
                    } else {
                        activityGitRepositoryService.writeActivitySource(activity.getGitPath(), activity.getGroovyCode(), "Update activity: " + activity.getName());
                    }
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody("Could not write to Git repository: " + e.getMessage()));
            }
        }

        activityDAO.saveOrUpdateActivity(activity);

        schedulerService.deleteJob("Activity" + activity.getId(), "Activities");
        if (activity.getCronScheduleEnabled()) {
            schedulerService.scheduleActivity(activity);
        }

        return ResponseEntity.ok(ActivityApiDto.from(activity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) throws SchedulerException
    {
        Activity activity = activityDAO.getActivityForId(id);

        if (activity == null) {
            return notFound();
        }

        // Mirrors ActivityController.delete: a Git failure is logged but never blocks deleting the
        // DB row - an orphaned file in Git is recoverable, an Activity the API refuses to delete
        // because Git is unreachable is a worse failure mode for the caller.
        if (activity.getGitPath() != null && activityGitRepositoryService.isActive()) {
            try {
                activityGitRepositoryService.deleteAndPush(activity.getGitPath(), "Delete activity: " + activity.getName());
            } catch (Exception e) {
                systemLogger.logMessage("WARN", "Could not delete Activity '" + activity.getName() + "' from Git", e);
            }
        }

        schedulerService.deleteJob("Activity" + id, "Activities");
        activityDAO.deleteActivity(activity);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/run")
    public ResponseEntity<?> run(@PathVariable Long id)
    {
        Activity activity = activityDAO.getActivityForId(id);

        if (activity == null) {
            return notFound();
        }

        ActivityInstance activityInstance = new ActivityInstance();
        activityInstance.setActivity(activity);

        activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);
        activityInstanceRunner.runActivity(activityInstance);

        // Mirrors ActivityInstanceRunner.runActivity's own gate exactly, purely to report it back -
        // runActivity silently no-ops rather than throwing/returning a result when approval is
        // required and missing, so without this the caller would get a 202 with no way to tell
        // whether the job actually started or is just sitting there waiting for manual approval.
        ActivityApprovalStrategy strategy = activity.getActivityApprovalStrategy();
        boolean started = activityInstance.getApproved() || strategy == null || "none".equalsIgnoreCase(strategy.getName());

        return ResponseEntity.accepted().body(ActivityRunResponseDto.from(activityInstance, started));
    }

    /**
     * Same two checks as the MVC form's ActivityController.activityValidate, ported from
     * BindingResult field errors to a plain error-message-or-null return - duplicated rather than
     * shared, consistent with this codebase's existing convention of duplicating small per-endpoint
     * logic (e.g. AgentScheduleApiController/AgentMessageApiController's identical authenticate()).
     * groovyCode may be null (update endpoint, script not being changed) - only cron is checked then.
     */
    private String validate(String groovyCode, String cronSchedule)
    {
        if (groovyCode != null) {
            try {
                Class<?> clazz = new GroovyClassLoader().parseClass(groovyCode);
                clazz.getDeclaredConstructor(ServiceProvider.class).newInstance(serviceProvider);
            } catch (Exception e) {
                return "groovyCode does not compile: " + e.getMessage();
            }
        }

        if (cronSchedule != null && !cronSchedule.isEmpty()) {
            try {
                new CronExpression(cronSchedule);
            } catch (Exception e) {
                return "cronSchedule is not a valid Quartz cron expression: " + e.getMessage();
            }
        }

        return null;
    }

    /**
     * Mirrors ActivityController.activityEdit's own git-freshness behaviour: if the Activity is
     * Git-backed and sync is active, re-fetch and return the live Git content instead of the
     * possibly-stale DB cache, falling back to the cache on any Git error rather than failing the
     * whole request - a caller reading via the API should see the same "what's actually in Git
     * right now" answer a human editing the same Activity in the GUI would.
     */
    private String resolveGroovyCode(String gitPath, String cachedGroovyCode)
    {
        if (gitPath == null || !activityGitRepositoryService.isActive()) {
            return cachedGroovyCode;
        }

        try {
            activityGitRepositoryService.sync();
            return activityGitRepositoryService.readActivitySource(gitPath);
        } catch (Exception e) {
            systemLogger.logMessage("WARN", "Could not read Git source for gitPath '" + gitPath + "', falling back to cached groovyCode", e);
            return cachedGroovyCode;
        }
    }

    private ResponseEntity<?> notFound()
    {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("Not found"));
    }

    private Map<String, String> errorBody(String message)
    {
        return Collections.singletonMap("error", message);
    }

    private static class ActivityApiRequest
    {
        public String name;
        public String cronSchedule;
        public boolean cronScheduleEnabled;
        public String groovyCode;
        public Long activityCategoryId;
        public Long activityApprovalStrategyId;
    }

    /**
     * Deliberately not the raw Activity entity - avoids serializing its lazy Ebean relations
     * (requiredActivities/triggerActivities). groovyCode is omitted from list/create/update
     * responses (via {@link #from}) since it's not relevant there and can be large; {@link #get}
     * uses {@link #fromDetailed} instead, the only place it's populated (`@JsonInclude(NON_NULL)`
     * keeps it out of the JSON entirely everywhere else, rather than serializing `"groovyCode":null`).
     */
    private static class ActivityApiDto
    {
        public Long id;
        public String name;
        public String cronSchedule;
        public boolean cronScheduleEnabled;
        public Long activityCategoryId;
        public String activityCategoryName;
        public Long activityApprovalStrategyId;
        public String activityApprovalStrategyName;
        public String gitPath;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String groovyCode;

        static ActivityApiDto from(Activity activity)
        {
            ActivityApiDto dto = new ActivityApiDto();
            dto.id = activity.getId();
            dto.name = activity.getName();
            dto.cronSchedule = activity.getCronSchedule();
            dto.cronScheduleEnabled = activity.getCronScheduleEnabled();
            dto.gitPath = activity.getGitPath();

            if (activity.getActivityCategory() != null) {
                dto.activityCategoryId = activity.getActivityCategory().getId();
                dto.activityCategoryName = activity.getActivityCategory().getName();
            }

            if (activity.getActivityApprovalStrategy() != null) {
                dto.activityApprovalStrategyId = activity.getActivityApprovalStrategy().getId();
                dto.activityApprovalStrategyName = activity.getActivityApprovalStrategy().getName();
            }

            return dto;
        }

        static ActivityApiDto fromDetailed(Activity activity, String resolvedGroovyCode)
        {
            ActivityApiDto dto = from(activity);
            dto.groovyCode = resolvedGroovyCode;

            return dto;
        }
    }

    private static class ActivityRunResponseDto
    {
        public Long instanceId;
        public Long activityId;

        /**
         * False means the instance was created but is sitting unapproved - see the comment at the
         * call site in {@link #run}. Not a run-time completion status either way (this endpoint
         * never waits for the job to finish); just whether it was actually handed to the runner.
         */
        public boolean started;

        static ActivityRunResponseDto from(ActivityInstance instance, boolean started)
        {
            ActivityRunResponseDto dto = new ActivityRunResponseDto();
            dto.instanceId = instance.getId();
            dto.activityId = instance.getActivity().getId();
            dto.started = started;
            return dto;
        }
    }
}
