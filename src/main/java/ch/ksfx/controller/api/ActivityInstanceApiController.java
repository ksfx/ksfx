package ch.ksfx.controller.api;

import ch.ksfx.services.activity.ActivityInstanceQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * Cross-activity run inventory - the one question {@link ActivityApiController}'s per-activity
 * endpoints couldn't answer without N calls: "what ran in the last 24 hours, across everything, and
 * is anything still running / stuck?" Same auth as the rest of {@code /api/**} (ApiClient bearer via
 * ApiTokenAuthenticationFilter). Own top-level path rather than {@code /api/activities/instances}
 * so it can't be mistaken for (or shadow) the {@code /api/activities/{id}/...} family.
 *
 * Read-only and summary-only: no {@code console} here (a run's log can be arbitrarily large) -
 * follow {@code activityId} + {@code instanceId} to {@code GET /api/activities/{id}/instances/{instanceId}}
 * for that. The query itself lives in {@link ActivityInstanceQueryService}.
 */
@RestController
@RequestMapping("/api/activity-instances")
public class ActivityInstanceApiController
{
    private final ActivityInstanceQueryService queryService;

    public ActivityInstanceApiController(ActivityInstanceQueryService queryService)
    {
        this.queryService = queryService;
    }

    /**
     * All filters optional: {@code status} (RUNNING | FINISHED | PENDING_APPROVAL),
     * {@code activityId}, {@code startedAfter} / {@code startedBefore} (ISO date or date-time),
     * {@code lastHours} (shorthand that overrides startedAfter), {@code page} / {@code size}
     * (size capped at {@link ActivityInstanceQueryService#MAX_PAGE_SIZE}). Newest first.
     */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) Long activityId,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String startedAfter,
                                  @RequestParam(required = false) String startedBefore,
                                  @RequestParam(required = false) Integer lastHours,
                                  @RequestParam(defaultValue = "0") Integer page,
                                  @RequestParam(defaultValue = "50") Integer size)
    {
        ActivityInstanceQueryService.Filter filter = new ActivityInstanceQueryService.Filter();
        filter.activityId = activityId;
        filter.status = status;
        filter.startedAfter = startedAfter;
        filter.startedBefore = startedBefore;
        filter.lastHours = lastHours;
        filter.page = page;
        filter.size = size;

        ActivityInstanceQueryService.Result result;

        try {
            result = queryService.query(filter);
        } catch (ActivityInstanceQueryService.InvalidFilterException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        }

        return ResponseEntity.ok(queryService.toBody(result));
    }
}
