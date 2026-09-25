package ch.ksfx.controller.api;

import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.publishing.PublishingResourceDAO;
import ch.ksfx.model.publishing.PublishingCategory;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.model.publishing.PublishingResource;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.services.git.ActivityGitRepositoryService;
import ch.ksfx.services.publishing.PublicationLoaderRunner;
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
 * External /api/** CRUDL surface for {@link PublishingConfiguration} ("reports"/"publications" in
 * the UI, otherwise managed via /publishing), plus a run-trigger endpoint - the Publishing sibling
 * of {@link ActivityApiController}, built to the exact same conventions (DTOs, Git-lockstep,
 * validation-as-plain-400 instead of BindingResult).
 *
 * <p>Unlike Activity, Publishing has no per-run instance/history entity - {@link
 * PublishingConfiguration#getConsole()} is a single field on the configuration itself, overwritten
 * on every run ({@code Console.startConsole}/{@code endConsole} in {@code PublicationViewerController}).
 * There is therefore no {@code /instances} or {@code /running} analog here: reading the current
 * {@code console} value is exactly what {@link #get} already returns, and "is it running right now"
 * would need the same in-memory {@code RunningPublicationsCache} Activity uses, deliberately left
 * out until an actual caller needs it - narrower surface, added on demand rather than speculatively.
 *
 * <p>{@link #run} always triggers the same fire-and-forget path the MVC "Generate now" action uses
 * ({@link PublicationLoaderRunner#loadPublication}, internally thread-pooled) - unlike Activity,
 * there is no synchronous variant of this method to accidentally call from here. The <em>separate</em>
 * synchronous execution path ({@code PublicationViewerController.loadPublishingStrategy}, used to
 * render a publication's output live in a browser/HTTP response) is not reachable from this
 * controller at all, by construction - not just by convention - since this class never references
 * it.
 */
@RestController
@RequestMapping("/api/publishing-configurations")
public class PublishingConfigurationApiController
{
    private final PublishingConfigurationDAO publishingConfigurationDAO;
    private final PublishingResourceDAO publishingResourceDAO;
    private final PublicationLoaderRunner publicationLoaderRunner;
    private final SchedulerService schedulerService;
    private final ServiceProvider serviceProvider;
    private final ActivityGitRepositoryService activityGitRepositoryService;
    private final SystemLogger systemLogger;

    public PublishingConfigurationApiController(PublishingConfigurationDAO publishingConfigurationDAO,
                                                 PublishingResourceDAO publishingResourceDAO,
                                                 PublicationLoaderRunner publicationLoaderRunner,
                                                 SchedulerService schedulerService,
                                                 ServiceProvider serviceProvider,
                                                 ActivityGitRepositoryService activityGitRepositoryService,
                                                 SystemLogger systemLogger)
    {
        this.publishingConfigurationDAO = publishingConfigurationDAO;
        this.publishingResourceDAO = publishingResourceDAO;
        this.publicationLoaderRunner = publicationLoaderRunner;
        this.schedulerService = schedulerService;
        this.serviceProvider = serviceProvider;
        this.activityGitRepositoryService = activityGitRepositoryService;
        this.systemLogger = systemLogger;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) Long publishingCategoryId)
    {
        List<PublishingConfiguration> configurations;

        if (publishingCategoryId != null) {
            PublishingCategory category = publishingConfigurationDAO.getPublishingCategoryForId(publishingCategoryId);

            if (category == null) {
                return ResponseEntity.badRequest().body(errorBody("No publishing category with id " + publishingCategoryId));
            }

            configurations = publishingConfigurationDAO.getPublishingConfigurationsForPublishingCategory(category);
        } else {
            configurations = publishingConfigurationDAO.getAllPublishingConfigurations();
        }

        return ResponseEntity.ok(configurations.stream().map(PublishingConfigurationApiDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id)
    {
        PublishingConfiguration configuration = publishingConfigurationDAO.getPublishingConfigurationForId(id);

        if (configuration == null) {
            return notFound();
        }

        return ResponseEntity.ok(PublishingConfigurationApiDto.fromDetailed(configuration, resolvePublishingStrategy(configuration.getGitPath(), configuration.getPublishingStrategy())));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PublishingConfigurationApiRequest body)
    {
        if (body.name == null || body.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("name is required"));
        }

        String validationError = validate(body.publishingStrategy, body.cronSchedule);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(errorBody(validationError));
        }

        PublishingConfiguration configuration = new PublishingConfiguration();
        configuration.setName(body.name);
        configuration.setUri(body.uri);
        configuration.setCronSchedule(body.cronSchedule);
        configuration.setCronScheduleEnabled(body.cronScheduleEnabled);
        configuration.setPublishingStrategy(body.publishingStrategy);
        configuration.setPublishingVisibility(body.publishingVisibility);
        configuration.setEmbedInLayout(body.embedInLayout);
        configuration.setLayoutIntegration(body.layoutIntegration);
        configuration.setAllowInternalLoad(body.allowInternalLoad);

        if (body.publishingCategoryId != null) {
            PublishingCategory category = publishingConfigurationDAO.getPublishingCategoryForId(body.publishingCategoryId);

            if (category == null) {
                return ResponseEntity.badRequest().body(errorBody("No publishing category with id " + body.publishingCategoryId));
            }

            configuration.setPublishingCategory(category);
        }

        if (activityGitRepositoryService.isActive() && configuration.getPublishingStrategy() != null) {
            try {
                String slug = activityGitRepositoryService.uniqueSlug(
                        activityGitRepositoryService.slugify(configuration.getName()),
                        ActivityGitRepositoryService.REPORTS_DIRECTORY);
                configuration.setGitPath(ActivityGitRepositoryService.REPORTS_DIRECTORY + "/" + slug + ".groovy");
                activityGitRepositoryService.writeActivitySource(configuration.getGitPath(), configuration.getPublishingStrategy(), "Create report: " + configuration.getName());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody("Could not write to Git repository: " + e.getMessage()));
            }
        }

        publishingConfigurationDAO.saveOrUpdatePublishingConfiguration(configuration);

        if (configuration.getCronScheduleEnabled()) {
            schedulerService.schedulePublication(configuration);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(PublishingConfigurationApiDto.from(configuration));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PublishingConfigurationApiRequest body) throws SchedulerException
    {
        PublishingConfiguration configuration = publishingConfigurationDAO.getPublishingConfigurationForId(id);

        if (configuration == null) {
            return notFound();
        }

        if (body.name == null || body.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("name is required"));
        }

        if (configuration.getLockedForEditing()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody("This publishing configuration is locked for editing"));
        }

        // publishingStrategy is only validated/overwritten if actually present in the request body -
        // an update that only e.g. toggles cronScheduleEnabled shouldn't have to resend the script,
        // and shouldn't have it wiped either. Same pattern as ActivityApiController#update.
        if (body.publishingStrategy != null) {
            String validationError = validate(body.publishingStrategy, body.cronSchedule);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(errorBody(validationError));
            }

            configuration.setPublishingStrategy(body.publishingStrategy);
        } else {
            String validationError = validate(null, body.cronSchedule);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(errorBody(validationError));
            }
        }

        if (body.publishingCategoryId != null) {
            PublishingCategory category = publishingConfigurationDAO.getPublishingCategoryForId(body.publishingCategoryId);

            if (category == null) {
                return ResponseEntity.badRequest().body(errorBody("No publishing category with id " + body.publishingCategoryId));
            }

            configuration.setPublishingCategory(category);
        }

        configuration.setName(body.name);
        configuration.setUri(body.uri);
        configuration.setCronSchedule(body.cronSchedule);
        configuration.setCronScheduleEnabled(body.cronScheduleEnabled);
        configuration.setPublishingVisibility(body.publishingVisibility);
        configuration.setEmbedInLayout(body.embedInLayout);
        configuration.setLayoutIntegration(body.layoutIntegration);
        configuration.setAllowInternalLoad(body.allowInternalLoad);

        if (activityGitRepositoryService.isActive() && configuration.getPublishingStrategy() != null) {
            try {
                if (configuration.getGitPath() == null) {
                    String slug = activityGitRepositoryService.uniqueSlug(
                            activityGitRepositoryService.slugify(configuration.getName()),
                            ActivityGitRepositoryService.REPORTS_DIRECTORY);
                    configuration.setGitPath(ActivityGitRepositoryService.REPORTS_DIRECTORY + "/" + slug + ".groovy");
                    activityGitRepositoryService.writeActivitySource(configuration.getGitPath(), configuration.getPublishingStrategy(), "Create report: " + configuration.getName());
                } else {
                    Set<String> siblingPaths = new HashSet<>();
                    for (PublishingConfiguration other : publishingConfigurationDAO.getAllPublishingConfigurations()) {
                        if (!other.getId().equals(configuration.getId()) && other.getGitPath() != null) {
                            siblingPaths.add(other.getGitPath());
                        }
                    }

                    String desiredPath = activityGitRepositoryService.desiredPath(ActivityGitRepositoryService.REPORTS_DIRECTORY, configuration.getName(), configuration.getGitPath(), siblingPaths);

                    if (!desiredPath.equals(configuration.getGitPath())) {
                        activityGitRepositoryService.renameAndWriteActivitySource(configuration.getGitPath(), desiredPath, configuration.getPublishingStrategy(), "Rename report: " + configuration.getName());
                        configuration.setGitPath(desiredPath);
                    } else {
                        activityGitRepositoryService.writeActivitySource(configuration.getGitPath(), configuration.getPublishingStrategy(), "Update report: " + configuration.getName());
                    }
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody("Could not write to Git repository: " + e.getMessage()));
            }
        }

        publishingConfigurationDAO.saveOrUpdatePublishingConfiguration(configuration);

        schedulerService.deleteJob("PublishingConfiguration" + configuration.getId(), "Publications");
        if (configuration.getCronScheduleEnabled()) {
            schedulerService.schedulePublication(configuration);
        }

        return ResponseEntity.ok(PublishingConfigurationApiDto.from(configuration));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) throws SchedulerException
    {
        PublishingConfiguration configuration = publishingConfigurationDAO.getPublishingConfigurationForId(id);

        if (configuration == null) {
            return notFound();
        }

        // Mirrors PublishingController#publishingConfigurationDelete: deleting the configuration
        // cascades (DB foreign key) to its PublishingResources without going through
        // publishingResourceDAO, so their Git files have to be cleaned up here explicitly too, same
        // as the MVC controller does - otherwise API-triggered deletes would leave orphaned Git
        // files the MVC path never would.
        if (activityGitRepositoryService.isActive()) {
            try {
                activityGitRepositoryService.sync();

                boolean deletedAny = false;

                if (configuration.getGitPath() != null) {
                    activityGitRepositoryService.deleteFile(configuration.getGitPath());
                    deletedAny = true;
                }

                for (PublishingResource resource : publishingResourceDAO.getAllPublishingResourcesForPublishingConfiguration(configuration)) {
                    if (resource.getGitPath() != null) {
                        activityGitRepositoryService.deleteFile(resource.getGitPath());
                        deletedAny = true;
                    }
                }

                if (deletedAny) {
                    activityGitRepositoryService.commitAndPush("Delete report: " + configuration.getName());
                }
            } catch (Exception e) {
                systemLogger.logMessage("WARN", "Could not delete PublishingConfiguration '" + configuration.getName() + "' from Git", e);
            }
        }

        schedulerService.deleteJob("PublishingConfiguration" + id, "Publications");
        publishingConfigurationDAO.deletePublishingConfiguration(configuration);

        return ResponseEntity.noContent().build();
    }

    /**
     * Always triggers the background path - {@link PublicationLoaderRunner#loadPublication} is the
     * only execution method it calls, and that method is fire-and-forget/thread-pooled by
     * construction (see class javadoc). Unlike Activity's {@code /run}, there is no approval-gate
     * concept here, so this always reports {@code started: true} - "triggered" is a more honest name
     * than "started" given the run happens on a background thread this response doesn't wait for,
     * but the field is named to match {@code ActivityRunResponseDto} for consistency across both APIs.
     */
    @PostMapping("/{id}/run")
    public ResponseEntity<?> run(@PathVariable Long id)
    {
        PublishingConfiguration configuration = publishingConfigurationDAO.getPublishingConfigurationForId(id);

        if (configuration == null) {
            return notFound();
        }

        publicationLoaderRunner.loadPublication(configuration);

        return ResponseEntity.accepted().body(PublishingRunResponseDto.from(configuration));
    }

    /**
     * Same two checks as the MVC form's PublishingController#validatePublishingConfiguration, ported
     * to a plain error-message-or-null return - including that check's existing narrowness (only the
     * single-arg {@code (ServiceProvider)} constructor is checked, not the
     * {@code (ServiceProvider, List)} overload {@code PublicationViewerController} actually falls
     * back to at run time) - kept identical on purpose so the API never accepts a script the MVC form
     * would reject, or vice versa. publishingStrategy may be null (update endpoint, script not being
     * changed) - only cron is checked then.
     */
    private String validate(String publishingStrategy, String cronSchedule)
    {
        if (publishingStrategy != null) {
            try {
                Class<?> clazz = new GroovyClassLoader().parseClass(publishingStrategy);
                clazz.getDeclaredConstructor(ServiceProvider.class);
            } catch (Exception e) {
                return "publishingStrategy does not compile: " + e.getMessage();
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
     * Mirrors PublishingController#publishingConfigurationEdit's own Git-freshness behaviour: if the
     * configuration is Git-backed and sync is active, re-fetch and return the live Git content
     * instead of the possibly-stale DB cache, falling back to the cache on any Git error rather than
     * failing the whole request.
     */
    private String resolvePublishingStrategy(String gitPath, String cachedPublishingStrategy)
    {
        if (gitPath == null || !activityGitRepositoryService.isActive()) {
            return cachedPublishingStrategy;
        }

        try {
            activityGitRepositoryService.sync();
            return activityGitRepositoryService.readActivitySource(gitPath);
        } catch (Exception e) {
            systemLogger.logMessage("WARN", "Could not read Git source for gitPath '" + gitPath + "', falling back to cached publishingStrategy", e);
            return cachedPublishingStrategy;
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

    private static class PublishingConfigurationApiRequest
    {
        public String name;
        public String uri;
        public String cronSchedule;
        public boolean cronScheduleEnabled;
        public String publishingStrategy;
        public String publishingVisibility;
        public boolean embedInLayout;
        public String layoutIntegration;
        public boolean allowInternalLoad;
        public Long publishingCategoryId;
    }

    /**
     * Deliberately not the raw entity - avoids serializing its lazy Ebean relations
     * (publishingConfigurationCacheDatas). publishingStrategy AND console are both omitted from
     * list/create/update responses (via {@link #from}) since either can be large; {@link #get} uses
     * {@link #fromDetailed} instead, the only place publishingStrategy is populated
     * (@JsonInclude(NON_NULL) keeps both out of the JSON entirely everywhere else). console is always
     * read straight from the entity (never Git-resolved - it isn't Git-backed data) whenever
     * fromDetailed is used.
     */
    private static class PublishingConfigurationApiDto
    {
        public Long id;
        public String name;
        public String uri;
        public String cronSchedule;
        public boolean cronScheduleEnabled;
        public String publishingVisibility;
        public boolean embedInLayout;
        public String layoutIntegration;
        public boolean allowInternalLoad;
        public boolean lockedForEditing;
        public boolean lockedForCacheUpdate;
        public Long publishingCategoryId;
        public String publishingCategoryName;
        public String gitPath;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String publishingStrategy;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String console;

        static PublishingConfigurationApiDto from(PublishingConfiguration configuration)
        {
            PublishingConfigurationApiDto dto = new PublishingConfigurationApiDto();
            dto.id = configuration.getId();
            dto.name = configuration.getName();
            dto.uri = configuration.getUri();
            dto.cronSchedule = configuration.getCronSchedule();
            dto.cronScheduleEnabled = configuration.getCronScheduleEnabled();
            dto.publishingVisibility = configuration.getPublishingVisibility();
            dto.embedInLayout = configuration.getEmbedInLayout();
            dto.layoutIntegration = configuration.getLayoutIntegration();
            dto.allowInternalLoad = configuration.getAllowInternalLoad();
            dto.lockedForEditing = configuration.getLockedForEditing();
            dto.lockedForCacheUpdate = configuration.getLockedForCacheUpdate();
            dto.gitPath = configuration.getGitPath();

            if (configuration.getPublishingCategory() != null) {
                dto.publishingCategoryId = configuration.getPublishingCategory().getId();
                dto.publishingCategoryName = configuration.getPublishingCategory().getName();
            }

            return dto;
        }

        static PublishingConfigurationApiDto fromDetailed(PublishingConfiguration configuration, String resolvedPublishingStrategy)
        {
            PublishingConfigurationApiDto dto = from(configuration);
            dto.publishingStrategy = resolvedPublishingStrategy;
            dto.console = configuration.getConsole();

            return dto;
        }
    }

    private static class PublishingRunResponseDto
    {
        public Long publishingConfigurationId;
        public boolean started;

        static PublishingRunResponseDto from(PublishingConfiguration configuration)
        {
            PublishingRunResponseDto dto = new PublishingRunResponseDto();
            dto.publishingConfigurationId = configuration.getId();
            dto.started = true;

            return dto;
        }
    }
}
