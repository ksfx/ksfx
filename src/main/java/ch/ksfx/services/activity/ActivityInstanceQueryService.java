package ch.ksfx.services.activity;

import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.activity.ActivityInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The cross-activity "what ran recently, and is anything still running?" query behind
 * {@code GET /api/activity-instances}. Lives in a service rather than inline in the controller
 * because the status derivation below mixes DB columns with the in-memory running cache - the one
 * piece of this that must not be re-derived ad hoc elsewhere. (An agent self-service twin of the
 * endpoint existed briefly on 2026-10-06 and was dropped the same day: Activities are deliberately
 * not part of the agent API, agents observe/steer them via the external API or not at all.)
 *
 * Status is the same three-way split the per-activity endpoints use ({@code RUNNING} from the
 * in-memory cache, else {@code FINISHED} if {@code finished} is set, else {@code PENDING_APPROVAL}),
 * which is exactly why status filtering can't be a plain WHERE clause: {@code RUNNING} is a key set
 * in {@link ActivityInstanceRunner}, so it's translated into id-in/id-not-in constraints before the
 * DB is asked. A crashed server leaves {@code started} set with neither a cache entry nor
 * {@code finished}, which this reports as {@code PENDING_APPROVAL} - same as the GUI, see
 * ActivityApiController#getInstance.
 */
@Service
public class ActivityInstanceQueryService
{
    public static final int MAX_PAGE_SIZE = 200;

    private final ActivityInstanceDAO activityInstanceDAO;
    private final ActivityInstanceRunner activityInstanceRunner;

    public ActivityInstanceQueryService(ActivityInstanceDAO activityInstanceDAO, ActivityInstanceRunner activityInstanceRunner)
    {
        this.activityInstanceDAO = activityInstanceDAO;
        this.activityInstanceRunner = activityInstanceRunner;
    }

    /** Thrown for a caller-side mistake (bad status, unparseable date); controllers map it to 400. */
    public static class InvalidFilterException extends RuntimeException
    {
        public InvalidFilterException(String message)
        {
            super(message);
        }
    }

    public static class Filter
    {
        public Long activityId;
        public String status;
        public String startedAfter;
        public String startedBefore;
        public Integer lastHours;
        public Integer page;
        public Integer size;
    }

    public static class Result
    {
        public Page<ActivityInstance> page;
        public Set<Long> runningIds;
        public Map<String, Object> appliedFilters;
    }

    public Result query(Filter filter)
    {
        String status = filter.status == null ? null : filter.status.trim().toUpperCase();

        if (status != null && !status.isEmpty() && !status.equals("RUNNING") && !status.equals("FINISHED") && !status.equals("PENDING_APPROVAL")) {
            throw new InvalidFilterException("Invalid status - use RUNNING, FINISHED or PENDING_APPROVAL");
        }

        Date startedAfter = parseDate(filter.startedAfter, "startedAfter", false);
        Date startedBefore = parseDate(filter.startedBefore, "startedBefore", true);

        if (filter.lastHours != null) {
            if (filter.lastHours < 1) {
                throw new InvalidFilterException("lastHours must be a positive number of hours");
            }

            startedAfter = Date.from(Instant.now().minusSeconds(filter.lastHours * 3600L));
        }

        Pageable pageable = PageRequest.of(
                Math.max(0, filter.page == null ? 0 : filter.page),
                Math.min(Math.max(1, filter.size == null ? 50 : filter.size), MAX_PAGE_SIZE));

        Set<Long> runningIds = activityInstanceRunner.getRunningInstanceIds();

        Result result = new Result();
        result.runningIds = runningIds;
        result.appliedFilters = new LinkedHashMap<>();
        result.appliedFilters.put("activityId", filter.activityId);
        result.appliedFilters.put("status", status == null || status.isEmpty() ? null : status);
        result.appliedFilters.put("startedAfter", startedAfter);
        result.appliedFilters.put("startedBefore", startedBefore);

        if ("RUNNING".equals(status)) {
            if (runningIds.isEmpty()) {
                result.page = new PageImpl<ActivityInstance>(Collections.<ActivityInstance>emptyList(), pageable, 0);
                return result;
            }

            result.page = activityInstanceDAO.getActivityInstancesForFilter(pageable, filter.activityId, startedAfter, startedBefore, null, runningIds, null);
        } else if ("FINISHED".equals(status)) {
            result.page = activityInstanceDAO.getActivityInstancesForFilter(pageable, filter.activityId, startedAfter, startedBefore, Boolean.TRUE, null, null);
        } else if ("PENDING_APPROVAL".equals(status)) {
            result.page = activityInstanceDAO.getActivityInstancesForFilter(pageable, filter.activityId, startedAfter, startedBefore, Boolean.FALSE, null, runningIds);
        } else {
            result.page = activityInstanceDAO.getActivityInstancesForFilter(pageable, filter.activityId, startedAfter, startedBefore, null, null, null);
        }

        return result;
    }

    /** Response body of the listing endpoint. */
    public Map<String, Object> toBody(Result result)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page", result.page.getNumber());
        body.put("size", result.page.getSize());
        body.put("totalElements", result.page.getTotalElements());
        body.put("runningNow", result.runningIds.size());
        body.put("filters", result.appliedFilters);
        body.put("instances", result.page.getContent().stream()
                .map(instance -> toSummary(instance, result.runningIds))
                .collect(Collectors.toList()));

        return body;
    }

    /** Summary without {@code console} (can be arbitrarily large) - the single-instance reads add it themselves. */
    public Map<String, Object> toSummary(ActivityInstance instance, Set<Long> runningIds)
    {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("instanceId", instance.getId());
        dto.put("activityId", instance.getActivity() != null ? instance.getActivity().getId() : null);
        dto.put("activityName", instance.getActivity() != null ? instance.getActivity().getName() : null);
        dto.put("status", statusOf(instance, runningIds));
        dto.put("started", instance.getStarted());
        dto.put("finished", instance.getFinished());
        dto.put("durationSeconds", instance.getStarted() != null && instance.getFinished() != null
                ? (instance.getFinished().getTime() - instance.getStarted().getTime()) / 1000 : null);
        dto.put("approved", instance.getApproved());

        return dto;
    }

    public String statusOf(ActivityInstance instance, Set<Long> runningIds)
    {
        if (runningIds.contains(instance.getId())) {
            return "RUNNING";
        }

        return instance.getFinished() != null ? "FINISHED" : "PENDING_APPROVAL";
    }

    /**
     * Accepts the formats a human or an agent is realistically going to type: a full ISO instant
     * ({@code 2026-10-05T06:00:00Z}), an offset date-time, a local date-time (server zone), or a
     * bare date ({@code 2026-10-05}) - which for an upper bound means end of that day, so
     * {@code startedBefore=2026-10-05} includes the whole 5th.
     */
    private Date parseDate(String value, String name, boolean endOfDayForBareDate)
    {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String v = value.trim();

        try {
            return Date.from(Instant.parse(v));
        } catch (DateTimeParseException ignored) { }

        try {
            return Date.from(OffsetDateTime.parse(v).toInstant());
        } catch (DateTimeParseException ignored) { }

        try {
            return Date.from(LocalDateTime.parse(v).atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) { }

        try {
            LocalDate date = LocalDate.parse(v);
            LocalDateTime dateTime = endOfDayForBareDate ? date.plusDays(1).atStartOfDay().minusNanos(1_000_000) : date.atStartOfDay();
            return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) { }

        throw new InvalidFilterException(name + " must be an ISO-8601 date or date-time, e.g. 2026-10-05 or 2026-10-05T06:00:00Z");
    }
}
