package ch.ksfx.services.project;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.AgenticConfigDAO;
import ch.ksfx.dao.CodeLibDAO;
import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.publishing.PublishingResourceDAO;
import ch.ksfx.dao.WikiDAO;
import ch.ksfx.dao.WikiPageDAO;
import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.IssueTrackerDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.model.CodeLib;
import ch.ksfx.model.Project;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.RequiredActivity;
import ch.ksfx.model.activity.TriggerActivity;
import ch.ksfx.model.issues.IssueTracker;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.model.publishing.PublishingResource;
import ch.ksfx.model.wiki.Wiki;
import ch.ksfx.services.agentic.AgentWorkspaceService;
import ch.ksfx.services.git.ActivityGitRepositoryService;
import ch.ksfx.services.systemlogger.SystemLogger;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Moves entities between projects - the ONLY place projects change (concept "Project as the
 * top-level context", section 10): the admin's Assignments screen plans a move (every entity gets
 * OK / BLOCKED with the reasons and side effects spelled out) and then executes exactly that plan.
 * The rules differ per type, which is why this is a service and not a column edit:
 *
 * <ul>
 *   <li>Wiki, Tracker - the FK only.</li>
 *   <li>Agent - FK + the workspace moves under the target project's folder (same mechanism the
 *       agent form uses), the target's Docker isolation applies from the next turn.</li>
 *   <li>Activity - BLOCKED while a required/trigger relation (either direction) points at an
 *       activity that will not be in the target project; the category is dropped if it belongs
 *       to another project; the Git file leaves the source repository and appears in the
 *       target's (or the code stays DB-only if the target has no repository).</li>
 *   <li>Report (publishing configuration + its resources) - like Activity, category and Git.</li>
 *   <li>Code Lib - BLOCKED if the target already has a lib of that name; a warning lists the
 *       source project's activities that still reference it by name; Git as above.</li>
 * </ul>
 */
@Service
public class ProjectAssignmentService
{
    public enum EntityType { AGENT, WIKI, TRACKER, ACTIVITY, REPORT, CODELIB }

    public static class Row
    {
        public Long id;
        public String name;
        public Long projectId;
        public String projectName;
        public String info;
    }

    public static class Item
    {
        public Long id;
        public String name;
        public String fromProject;
        public boolean blocked;
        public List<String> notes = new ArrayList<>();
    }

    private final ProjectDAO projectDAO;
    private final AgentDAO agentDAO;
    private final AgenticConfigDAO agenticConfigDAO;
    private final AgentWorkspaceService agentWorkspaceService;
    private final WikiDAO wikiDAO;
    private final WikiPageDAO wikiPageDAO;
    private final IssueTrackerDAO issueTrackerDAO;
    private final ActivityDAO activityDAO;
    private final PublishingConfigurationDAO publishingConfigurationDAO;
    private final PublishingResourceDAO publishingResourceDAO;
    private final CodeLibDAO codeLibDAO;
    private final ActivityGitRepositoryService gitService;
    private final SystemLogger systemLogger;

    public ProjectAssignmentService(ProjectDAO projectDAO, AgentDAO agentDAO, AgenticConfigDAO agenticConfigDAO,
                                    AgentWorkspaceService agentWorkspaceService, WikiDAO wikiDAO, WikiPageDAO wikiPageDAO,
                                    IssueTrackerDAO issueTrackerDAO, ActivityDAO activityDAO,
                                    PublishingConfigurationDAO publishingConfigurationDAO, PublishingResourceDAO publishingResourceDAO,
                                    CodeLibDAO codeLibDAO, ActivityGitRepositoryService gitService, SystemLogger systemLogger)
    {
        this.projectDAO = projectDAO;
        this.agentDAO = agentDAO;
        this.agenticConfigDAO = agenticConfigDAO;
        this.agentWorkspaceService = agentWorkspaceService;
        this.wikiDAO = wikiDAO;
        this.wikiPageDAO = wikiPageDAO;
        this.issueTrackerDAO = issueTrackerDAO;
        this.activityDAO = activityDAO;
        this.publishingConfigurationDAO = publishingConfigurationDAO;
        this.publishingResourceDAO = publishingResourceDAO;
        this.codeLibDAO = codeLibDAO;
        this.gitService = gitService;
        this.systemLogger = systemLogger;
    }

    // =====================================================================================
    // Listing
    // =====================================================================================

    public List<Row> list(EntityType type)
    {
        List<Row> rows = new ArrayList<>();

        switch (type) {
            case AGENT:
                for (Agent a : agentDAO.getAllAgents()) {
                    rows.add(row(a.getId(), a.getName(), a.getProject(), a.getEnabled() ? "" : "disabled"));
                }
                break;
            case WIKI:
                for (Wiki w : wikiDAO.getAllWikis()) {
                    rows.add(row(w.getId(), w.getName(), w.getProject(), wikiPageDAO.getAllWikiPages(w.getId()).size() + " pages"));
                }
                break;
            case TRACKER:
                for (IssueTracker t : issueTrackerDAO.getAllIssueTrackers()) {
                    rows.add(row(t.getId(), t.getName(), t.getProject(), ""));
                }
                break;
            case ACTIVITY:
                for (Activity a : activityDAO.getAllActivities()) {
                    rows.add(row(a.getId(), a.getName(), a.getProject(),
                            (a.getActivityCategory() != null ? a.getActivityCategory().getName() : "")
                                    + (ActivityGitRepositoryService.hasGitPath(a.getGitPath()) ? " · " + a.getGitPath() : "")));
                }
                break;
            case REPORT:
                for (PublishingConfiguration c : publishingConfigurationDAO.getAllPublishingConfigurations()) {
                    rows.add(row(c.getId(), c.getName(), c.getProject(),
                            (c.getPublishingCategory() != null ? c.getPublishingCategory().getName() : "")
                                    + (ActivityGitRepositoryService.hasGitPath(c.getGitPath()) ? " · " + c.getGitPath() : "")));
                }
                break;
            case CODELIB:
                for (CodeLib l : codeLibDAO.getAllCodeLibs()) {
                    rows.add(row(l.getId(), l.getName(), l.getProject(), ActivityGitRepositoryService.hasGitPath(l.getGitPath()) ? l.getGitPath() : ""));
                }
                break;
        }

        return rows;
    }

    private Row row(Long id, String name, Project project, String info)
    {
        Row row = new Row();
        row.id = id;
        row.name = name;
        row.projectId = project != null ? project.getId() : null;
        row.projectName = project != null ? project.getName() : "-";
        row.info = info == null ? "" : info.trim();

        return row;
    }

    // =====================================================================================
    // Plan (dry run) and execute
    // =====================================================================================

    public List<Item> plan(EntityType type, List<Long> ids, Project target)
    {
        return run(type, ids, target, false);
    }

    public List<Item> execute(EntityType type, List<Long> ids, Project target)
    {
        List<Item> items = run(type, ids, target, true);

        for (Item item : items) {
            systemLogger.logMessage("PROJECTS", "Move " + type + " '" + item.name + "' (" + item.id + ") " + item.fromProject + " -> " + target.getName()
                    + (item.blocked ? " BLOCKED: " : ": ") + String.join("; ", item.notes));
        }

        return items;
    }

    private List<Item> run(EntityType type, List<Long> ids, Project target, boolean execute)
    {
        List<Item> items = new ArrayList<>();

        for (Long id : ids) {
            Item item;

            try {
                switch (type) {
                    case AGENT: item = moveAgent(id, target, execute); break;
                    case WIKI: item = moveWiki(id, target, execute); break;
                    case TRACKER: item = moveTracker(id, target, execute); break;
                    case ACTIVITY: item = moveActivity(id, target, execute); break;
                    case REPORT: item = moveReport(id, target, execute); break;
                    case CODELIB: item = moveCodeLib(id, target, execute); break;
                    default: continue;
                }
            } catch (Exception e) {
                item = new Item();
                item.id = id;
                item.name = "#" + id;
                item.blocked = true;
                item.notes.add("failed: " + e.getMessage());
                systemLogger.logMessage("PROJECTS", "Move " + type + " " + id + " failed", e);
            }

            items.add(item);
        }

        return items;
    }

    private Item item(Long id, String name, Project from)
    {
        Item item = new Item();
        item.id = id;
        item.name = name;
        item.fromProject = from != null ? from.getName() : "-";

        return item;
    }

    private boolean same(Project a, Project b)
    {
        return a != null && b != null && a.getId().equals(b.getId());
    }

    // ---- Agent ----

    private Item moveAgent(Long id, Project target, boolean execute) throws Exception
    {
        Agent agent = agentDAO.getAgentForId(id);
        Item item = item(id, agent.getName(), agent.getProject());

        if (same(agent.getProject(), target)) {
            item.notes.add("already in " + target.getName());
            return item;
        }

        AgenticConfig config = agenticConfigDAO.getAgenticConfig();
        item.notes.add(config != null ? "workspace moves under the target project's folder" : "no Agentic configuration - workspace path unchanged");

        if (target.getDockerIsolationEnabled() != (agent.getProject() != null && agent.getProject().getDockerIsolationEnabled())) {
            item.notes.add(target.getDockerIsolationEnabled() ? "target project runs agents in Docker - applies from the next turn" : "target project runs agents without Docker");
        }

        if (execute) {
            if (config != null) {
                Path oldWorkspace = agentWorkspaceService.resolveWorkspace(agent, config);
                agent.setProject(target);
                Path newWorkspace = agentWorkspaceService.resolveWorkspace(agent, config);
                agentWorkspaceService.moveWorkspaceIfNeeded(oldWorkspace, newWorkspace);
                agent.setWorkspacePath(newWorkspace.toString());
            } else {
                agent.setProject(target);
            }

            agentDAO.saveOrUpdateAgent(agent);
        }

        return item;
    }

    // ---- Wiki / Tracker ----

    private Item moveWiki(Long id, Project target, boolean execute)
    {
        Wiki wiki = wikiDAO.getWikiForId(id);
        Item item = item(id, wiki.getName(), wiki.getProject());

        if (same(wiki.getProject(), target)) {
            item.notes.add("already in " + target.getName());
            return item;
        }

        if (execute) {
            wiki.setProject(target);
            wikiDAO.saveOrUpdateWiki(wiki);
        }

        return item;
    }

    private Item moveTracker(Long id, Project target, boolean execute)
    {
        IssueTracker tracker = issueTrackerDAO.getIssueTrackerForId(id);
        Item item = item(id, tracker.getName(), tracker.getProject());

        if (same(tracker.getProject(), target)) {
            item.notes.add("already in " + target.getName());
            return item;
        }

        item.notes.add("issues assigned to agents of other projects keep their assignee");

        if (execute) {
            tracker.setProject(target);
            issueTrackerDAO.saveOrUpdateIssueTracker(tracker);
        }

        return item;
    }

    // ---- Activity ----

    private Item moveActivity(Long id, Project target, boolean execute) throws Exception
    {
        Activity activity = activityDAO.getActivityForId(id);
        Item item = item(id, activity.getName(), activity.getProject());
        Project source = activity.getProject();

        if (same(source, target)) {
            item.notes.add("already in " + target.getName());
            return item;
        }

        // Required/trigger relations must stay inside one project - either direction.
        for (RequiredActivity required : activity.getRequiredActivities() != null ? activity.getRequiredActivities() : new ArrayList<RequiredActivity>()) {
            if (required.getRequiredActivity() != null && !same(required.getRequiredActivity().getProject(), target)) {
                item.blocked = true;
                item.notes.add("requires '" + required.getRequiredActivity().getName() + "' which stays in " + nameOf(required.getRequiredActivity().getProject()));
            }
        }

        for (TriggerActivity trigger : activity.getTriggerActivities() != null ? activity.getTriggerActivities() : new ArrayList<TriggerActivity>()) {
            if (trigger.getTriggerActivity() != null && !same(trigger.getTriggerActivity().getProject(), target)) {
                item.blocked = true;
                item.notes.add("triggers '" + trigger.getTriggerActivity().getName() + "' which stays in " + nameOf(trigger.getTriggerActivity().getProject()));
            }
        }

        for (Activity other : activityDAO.getAllActivities()) {
            if (other.getId().equals(activity.getId()) || same(other.getProject(), target)) {
                continue;
            }

            for (RequiredActivity required : other.getRequiredActivities() != null ? other.getRequiredActivities() : new ArrayList<RequiredActivity>()) {
                if (required.getRequiredActivity() != null && required.getRequiredActivity().getId().equals(activity.getId())) {
                    item.blocked = true;
                    item.notes.add("required by '" + other.getName() + "' in " + nameOf(other.getProject()));
                }
            }

            for (TriggerActivity trigger : other.getTriggerActivities() != null ? other.getTriggerActivities() : new ArrayList<TriggerActivity>()) {
                if (trigger.getTriggerActivity() != null && trigger.getTriggerActivity().getId().equals(activity.getId())) {
                    item.blocked = true;
                    item.notes.add("triggered by '" + other.getName() + "' in " + nameOf(other.getProject()));
                }
            }
        }

        if (item.blocked) {
            return item;
        }

        boolean dropCategory = activity.getActivityCategory() != null && !same(activity.getActivityCategory().getProject(), target);

        if (dropCategory) {
            item.notes.add("category '" + activity.getActivityCategory().getName() + "' belongs to " + nameOf(activity.getActivityCategory().getProject()) + " - cleared");
        }

        describeGitMove(item, source, target, activity.getGitPath());

        if (execute) {
            String content = currentSource(source, activity.getGitPath(), activity.getGroovyCode());
            String newPath = gitService.moveToProject(source, target, activity.getGitPath(), ActivityGitRepositoryService.ACTIVITIES_DIRECTORY, content,
                    "Move activity to project " + target.getName() + ": " + activity.getName());

            if (dropCategory) {
                activity.setActivityCategory(null);
            }

            activity.setProject(target);
            activity.setGitPath(newPath);
            activity.setGroovyCode(content);
            activityDAO.saveOrUpdateActivity(activity);
        }

        return item;
    }

    // ---- Report ----

    private Item moveReport(Long id, Project target, boolean execute) throws Exception
    {
        PublishingConfiguration configuration = publishingConfigurationDAO.getPublishingConfigurationForId(id);
        Item item = item(id, configuration.getName(), configuration.getProject());
        Project source = configuration.getProject();

        if (same(source, target)) {
            item.notes.add("already in " + target.getName());
            return item;
        }

        boolean dropCategory = configuration.getPublishingCategory() != null && !same(configuration.getPublishingCategory().getProject(), target);

        if (dropCategory) {
            item.notes.add("category '" + configuration.getPublishingCategory().getName() + "' belongs to " + nameOf(configuration.getPublishingCategory().getProject()) + " - cleared");
        }

        List<PublishingResource> resources = publishingResourceDAO.getAllPublishingResourcesForPublishingConfiguration(configuration);

        if (!resources.isEmpty()) {
            item.notes.add(resources.size() + " resource(s) move along");
        }

        describeGitMove(item, source, target, configuration.getGitPath());

        if (execute) {
            String content = currentSource(source, configuration.getGitPath(), configuration.getPublishingStrategy());
            String newPath = gitService.moveToProject(source, target, configuration.getGitPath(), ActivityGitRepositoryService.REPORTS_DIRECTORY, content,
                    "Move report to project " + target.getName() + ": " + configuration.getName());

            for (PublishingResource resource : resources) {
                String resourceContent = currentSource(source, resource.getGitPath(), resource.getPublishingStrategy());
                String resourcePath = gitService.moveToProject(source, target, resource.getGitPath(), ActivityGitRepositoryService.REPORT_RESOURCES_DIRECTORY, resourceContent,
                        "Move report resource to project " + target.getName() + ": " + resource.getTitle());
                resource.setGitPath(resourcePath);
                resource.setPublishingStrategy(resourceContent);
                publishingResourceDAO.saveOrUpdatePublishingResource(resource);
            }

            if (dropCategory) {
                configuration.setPublishingCategory(null);
            }

            configuration.setProject(target);
            configuration.setGitPath(newPath);
            configuration.setPublishingStrategy(content);
            publishingConfigurationDAO.saveOrUpdatePublishingConfiguration(configuration);
        }

        return item;
    }

    // ---- Code Lib ----

    private Item moveCodeLib(Long id, Project target, boolean execute) throws Exception
    {
        CodeLib codeLib = codeLibDAO.getCodeLibForId(id);
        Item item = item(id, codeLib.getName(), codeLib.getProject());
        Project source = codeLib.getProject();

        if (same(source, target)) {
            item.notes.add("already in " + target.getName());
            return item;
        }

        CodeLib clash = codeLibDAO.getCodeLibForProjectAndName(target.getId(), codeLib.getName());

        if (clash != null) {
            item.blocked = true;
            item.notes.add(target.getName() + " already has a code lib named '" + codeLib.getName() + "'");
            return item;
        }

        List<String> referencing = new ArrayList<>();

        for (Activity activity : activityDAO.getAllActivities()) {
            if (same(activity.getProject(), source) && activity.getGroovyCode() != null && activity.getGroovyCode().contains("\"" + codeLib.getName() + "\"")) {
                referencing.add(activity.getName());
            }
        }

        if (!referencing.isEmpty()) {
            item.notes.add("still referenced by name in " + nameOf(source) + ": " + String.join(", ", referencing) + " (resolves across projects while the name stays unique)");
        }

        describeGitMove(item, source, target, codeLib.getGitPath());

        if (execute) {
            String content = currentSource(source, codeLib.getGitPath(), codeLib.getGroovyCode());
            String newPath = gitService.moveToProject(source, target, codeLib.getGitPath(), ActivityGitRepositoryService.LIBS_DIRECTORY, content,
                    "Move code lib to project " + target.getName() + ": " + codeLib.getName());

            codeLib.setProject(target);
            codeLib.setGitPath(newPath);
            codeLib.setGroovyCode(content);
            codeLibDAO.saveOrUpdateCodeLib(codeLib);
        }

        return item;
    }

    // ---- shared ----

    private void describeGitMove(Item item, Project source, Project target, String gitPath)
    {
        boolean sourceGit = ActivityGitRepositoryService.hasGitPath(gitPath) && gitService.isActive(source);
        boolean targetGit = gitService.isActive(target);

        if (sourceGit && targetGit) {
            item.notes.add("Git: file leaves " + nameOf(source) + "'s repository and is created in " + target.getName() + "'s (history does not follow)");
        } else if (sourceGit) {
            item.notes.add("Git: file is deleted from " + nameOf(source) + "'s repository - " + target.getName() + " has no repository, the code lives in the database only");
        } else if (targetGit) {
            item.notes.add("Git: file is created in " + target.getName() + "'s repository");
        }
    }

    /** The authoritative source: the Git file if the source project is Git-backed and has one, else the DB cache. */
    private String currentSource(Project source, String gitPath, String cached) throws Exception
    {
        if (ActivityGitRepositoryService.hasGitPath(gitPath) && gitService.isActive(source)) {
            try {
                gitService.sync(source);
                return gitService.readActivitySource(source, gitPath);
            } catch (Exception e) {
                systemLogger.logMessage("PROJECTS", "Could not read " + gitPath + " from Git, using the cached source", e);
            }
        }

        return cached;
    }

    private String nameOf(Project project)
    {
        return project != null ? project.getName() : "-";
    }
}
