package ch.ksfx.controller.api;

import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.ActivityCategory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * External /api/** entry point for creating Activities (the existing scheduled Cron/Groovy job
 * model under {@link Activity}, otherwise managed via /activity in the UI). Authenticated by
 * ch.ksfx.services.security.ApiTokenAuthenticationFilter / ApiClientAuthenticationProvider - unlike
 * /agentic/api/**, the caller is resolved through Spring Security proper, not an inline check.
 */
@RestController
@RequestMapping("/api/activities")
public class ActivityApiController
{
    private final ActivityDAO activityDAO;

    public ActivityApiController(ActivityDAO activityDAO)
    {
        this.activityDAO = activityDAO;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ActivityCreateRequest body)
    {
        if (body.name == null || body.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("name is required"));
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

        activityDAO.saveOrUpdateActivity(activity);

        return ResponseEntity.status(HttpStatus.CREATED).body(ActivityApiDto.from(activity));
    }

    private Map<String, String> errorBody(String message)
    {
        return Collections.singletonMap("error", message);
    }

    private static class ActivityCreateRequest
    {
        public String name;
        public String cronSchedule;
        public boolean cronScheduleEnabled;
        public String groovyCode;
        public Long activityCategoryId;
    }

    /**
     * Deliberately not the raw Activity entity - avoids serializing its lazy Ebean relations
     * (requiredActivities/triggerActivities), which aren't relevant to a create response.
     */
    private static class ActivityApiDto
    {
        public Long id;
        public String name;
        public String cronSchedule;
        public boolean cronScheduleEnabled;

        static ActivityApiDto from(Activity activity)
        {
            ActivityApiDto dto = new ActivityApiDto();
            dto.id = activity.getId();
            dto.name = activity.getName();
            dto.cronSchedule = activity.getCronSchedule();
            dto.cronScheduleEnabled = activity.getCronScheduleEnabled();
            return dto;
        }
    }
}
