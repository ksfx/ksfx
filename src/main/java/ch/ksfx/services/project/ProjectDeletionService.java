package ch.ksfx.services.project;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.AgentScheduleDAO;
import ch.ksfx.dao.AgenticConfigDAO;
import ch.ksfx.dao.CodeLibDAO;
import ch.ksfx.dao.GitSyncConfigDAO;
import ch.ksfx.dao.IssueDAO;
import ch.ksfx.dao.IssueTrackerDAO;
import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.WikiDAO;
import ch.ksfx.dao.WikiPageDAO;
import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgentSchedule;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.model.CodeLib;
import ch.ksfx.model.GitSyncConfig;
import ch.ksfx.model.Project;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.ActivityCategory;
import ch.ksfx.model.issues.IssueStatus;
import ch.ksfx.model.issues.IssueTracker;
import ch.ksfx.model.publishing.PublishingCategory;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.model.wiki.Wiki;
import ch.ksfx.services.agentic.AgentWorkspaceService;
import ch.ksfx.services.agentic.AgenticDockerService;
import ch.ksfx.services.git.ActivityGitRepositoryService;
import ch.ksfx.services.scheduler.SchedulerService;
import ch.ksfx.services.systemlogger.SystemLogger;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Deleting a project deletes EVERYTHING in it (decided 2026-10-10, concept section 9 Q3): the
 * agents with their messages and schedules, the wikis with every page/version/attachment, the
 * trackers with every issue, the activities with every instance, the reports with their
 * resources, the categories, the code libs, the Git configuration - and on disk the agents'
 * workspaces and the project's local Git clone, plus the Docker container. Deliberately NOT
 * touched: the remote Git repository (the code history survives the project) and the data layer
 * (time series etc.) the project's activities wrote, which is global by design.
 *
 * Guarded twice: {@link #summary} is shown first so the admin sees the counts, and the controller
 * requires the project name to be typed. The default project and the last remaining project can
 * never be deleted.
 */
@Service
public class ProjectDeletionService
{
    private final ProjectDAO projectDAO;
    private final AgentDAO agentDAO;
    private final AgentScheduleDAO agentScheduleDAO;
    private final AgenticConfigDAO agenticConfigDAO;
    private final AgentWorkspaceService agentWorkspaceService;
    private final AgenticDockerService agenticDockerService;
    private final WikiDAO wikiDAO;
    private final WikiPageDAO wikiPageDAO;
    private final IssueTrackerDAO issueTrackerDAO;
    private final IssueDAO issueDAO;
    private final ActivityDAO activityDAO;
    private final ActivityInstanceDAO activityInstanceDAO;
    private final PublishingConfigurationDAO publishingConfigurationDAO;
    private final CodeLibDAO codeLibDAO;
    private final GitSyncConfigDAO gitSyncConfigDAO;
    private final ActivityGitRepositoryService gitService;
    private final SchedulerService schedulerService;
    private final SystemLogger systemLogger;

    public ProjectDeletionService(ProjectDAO projectDAO, AgentDAO agentDAO, AgentScheduleDAO agentScheduleDAO, AgenticConfigDAO agenticConfigDAO,
                                  AgentWorkspaceService agentWorkspaceService, AgenticDockerService agenticDockerService, WikiDAO wikiDAO,
                                  WikiPageDAO wikiPageDAO, IssueTrackerDAO issueTrackerDAO, IssueDAO issueDAO, ActivityDAO activityDAO,
                                  ActivityInstanceDAO activityInstanceDAO, PublishingConfigurationDAO publishingConfigurationDAO, CodeLibDAO codeLibDAO,
                                  GitSyncConfigDAO gitSyncConfigDAO, ActivityGitRepositoryService gitService, SchedulerService schedulerService,
                                  SystemLogger systemLogger)
    {
        this.projectDAO = projectDAO;
        this.agentDAO = agentDAO;
        this.agentScheduleDAO = agentScheduleDAO;
        this.agenticConfigDAO = agenticConfigDAO;
        this.agentWorkspaceService = agentWorkspaceService;
        this.agenticDockerService = agenticDockerService;
        this.wikiDAO = wikiDAO;
        this.wikiPageDAO = wikiPageDAO;
        this.issueTrackerDAO = issueTrackerDAO;
        this.issueDAO = issueDAO;
        this.activityDAO = activityDAO;
        this.activityInstanceDAO = activityInstanceDAO;
        this.publishingConfigurationDAO = publishingConfigurationDAO;
        this.codeLibDAO = codeLibDAO;
        this.gitSyncConfigDAO = gitSyncConfigDAO;
        this.gitService = gitService;
        this.schedulerService = schedulerService;
        this.systemLogger = systemLogger;
    }

    /** Why the project cannot be deleted, or null if it can. */
    public String deletionBlocker(Project project)
    {
        if (project.getDefaultProject()) {
            return "The default project cannot be deleted.";
        }

        if (projectDAO.getAllProjects().size() <= 1) {
            return "The last remaining project cannot be deleted.";
        }

        return null;
    }

    /** Ordered "what will be deleted" lines for the confirmation page. */
    public Map<String, String> summary(Project project)
    {
        Map<String, String> summary = new LinkedHashMap<>();
        Long id = project.getId();

        List<Agent> agents = agentDAO.getAgentsForProject(id);
        summary.put("Agents (with all messages and schedules)", String.valueOf(agents.size()));

        int wikis = 0, pages = 0;
        for (Wiki wiki : wikiDAO.getAllWikis()) {
            if (belongs(wiki.getProject(), id)) {
                wikis++;
                pages += wikiPageDAO.getAllWikiPages(wiki.getId()).size();
            }
        }
        summary.put("Wikis (with pages, versions, attachments)", wikis + " wikis, " + pages + " pages");

        int trackers = 0, issues = 0;
        for (IssueTracker tracker : issueTrackerDAO.getAllIssueTrackers()) {
            if (belongs(tracker.getProject(), id)) {
                trackers++;
                issues += issueDAO.countIssuesForTracker(tracker.getId(), Arrays.asList(IssueStatus.values()));
            }
        }
        summary.put("Issue trackers (with issues, comments, attachments)", trackers + " trackers, " + issues + " issues");

        int activities = 0; long instances = 0;
        for (Activity activity : activityDAO.getAllActivities()) {
            if (belongs(activity.getProject(), id)) {
                activities++;
                instances += activityInstanceDAO.getActivityInstancesForPageableAndActivity(PageRequest.of(0, 1), activity, false).getTotalElements();
            }
        }
        summary.put("Activities (with instances and their data)", activities + " activities, " + instances + " instances");

        int reports = 0;
        for (PublishingConfiguration configuration : publishingConfigurationDAO.getAllPublishingConfigurations()) {
            if (belongs(configuration.getProject(), id)) {
                reports++;
            }
        }
        summary.put("Reports (with resources and caches)", String.valueOf(reports));

        int libs = 0;
        for (CodeLib codeLib : codeLibDAO.getAllCodeLibs()) {
            if (belongs(codeLib.getProject(), id)) {
                libs++;
            }
        }
        summary.put("Code libs", String.valueOf(libs));

        GitSyncConfig gitConfig = gitSyncConfigDAO.getGitSyncConfigForProject(id);
        summary.put("Git", gitConfig == null ? "no repository configured"
                : "local clone " + gitConfig.getLocalClonePath() + " is deleted; the remote repository " + gitConfig.getRepoUrl() + " is NOT touched");

        AgenticConfig config = agenticConfigDAO.getAgenticConfig();
        summary.put("Workspaces on disk", config != null ? agentWorkspaceService.resolveProjectWorkspace(project, config).toString() + " (deleted)" : "no Agentic configuration - nothing on disk");
        summary.put("Docker container", project.getDockerIsolationEnabled() ? "removed" : "none");
        summary.put("Not touched", "the data layer (time series, data structure) written by this project's activities");

        return summary;
    }

    public void delete(Project project) throws Exception
    {
        String blocker = deletionBlocker(project);

        if (blocker != null) {
            throw new IllegalStateException(blocker);
        }

        Long id = project.getId();
        systemLogger.logMessage("PROJECTS", "Deleting project '" + project.getName() + "' (" + id + ") with all content");

        // agents: Quartz jobs first, then the rows (messages/schedules cascade in the DB)
        for (Agent agent : agentDAO.getAgentsForProject(id)) {
            for (AgentSchedule schedule : agentScheduleDAO.getSchedulesForAgent(agent.getId())) {
                schedulerService.deleteJob("AgentSchedule" + schedule.getId(), "AgentSchedules");
            }

            agentDAO.deleteAgent(agent);
        }

        for (Wiki wiki : new ArrayList<>(wikiDAO.getAllWikis())) {
            if (belongs(wiki.getProject(), id)) {
                wikiDAO.deleteWiki(wiki);
            }
        }

        for (IssueTracker tracker : new ArrayList<>(issueTrackerDAO.getAllIssueTrackers())) {
            if (belongs(tracker.getProject(), id)) {
                issueTrackerDAO.deleteIssueTracker(tracker);
            }
        }

        for (Activity activity : new ArrayList<>(activityDAO.getAllActivities())) {
            if (belongs(activity.getProject(), id)) {
                schedulerService.deleteJob("Activity" + activity.getId(), "Activities");
                activityDAO.deleteActivity(activity);
            }
        }

        for (ActivityCategory category : new ArrayList<>(activityDAO.getAllActivityCategories())) {
            if (belongs(category.getProject(), id)) {
                activityDAO.deleteActivityCategory(category);
            }
        }

        for (PublishingConfiguration configuration : new ArrayList<>(publishingConfigurationDAO.getAllPublishingConfigurations())) {
            if (belongs(configuration.getProject(), id)) {
                schedulerService.deleteJob("PublishingConfiguration" + configuration.getId(), "Publications");
                publishingConfigurationDAO.deletePublishingConfiguration(configuration);
            }
        }

        for (PublishingCategory category : new ArrayList<>(publishingConfigurationDAO.getAllPublishingCategories())) {
            if (belongs(category.getProject(), id)) {
                publishingConfigurationDAO.deletePublishingCategory(category);
            }
        }

        for (CodeLib codeLib : new ArrayList<>(codeLibDAO.getAllCodeLibs())) {
            if (belongs(codeLib.getProject(), id)) {
                codeLibDAO.deleteCodeLib(codeLib);
            }
        }

        // disk: local Git clone (never the remote), the project's workspace tree, the container
        gitService.deleteLocalClone(project);

        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        if (config != null) {
            deleteTree(agentWorkspaceService.resolveProjectWorkspace(project, config));
        }

        if (project.getDockerIsolationEnabled()) {
            agenticDockerService.deleteContainer(project); // never throws
        }

        projectDAO.deleteProject(project); // git_sync_config cascades via FK
        systemLogger.logMessage("PROJECTS", "Deleted project '" + project.getName() + "' (" + id + ")");
    }

    private boolean belongs(Project project, Long id)
    {
        return project != null && id.equals(project.getId());
    }

    private void deleteTree(Path root) throws Exception
    {
        if (root == null || !Files.exists(root)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
}
