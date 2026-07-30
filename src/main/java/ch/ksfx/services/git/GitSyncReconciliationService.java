/**
 *
 * Copyright (C) 2011-2017 KSFX. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ksfx.services.git;

import ch.ksfx.dao.CodeLibDAO;
import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.publishing.PublishingResourceDAO;
import ch.ksfx.model.CodeLib;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.model.publishing.PublishingResource;
import ch.ksfx.services.systemlogger.SystemLogger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reconciles KSFX's database against Git, but the two sides are the master for different things:
 * <ul>
 *     <li><b>KSFX is the master for existence and naming</b> - a git file with no matching live
 *     gitPath is deleted (the KSFX row was deleted); a live entity whose name/title no longer
 *     matches its gitPath's slug gets its file renamed (collision-aware against sibling entities'
 *     current paths - a slug freed up by another rename/delete in the same run is reclaimed); a
 *     live entity whose file is missing from git gets it recreated from the DB-cached source
 *     (nothing else to recreate it from in that specific case).</li>
 *     <li><b>Git is the master for source code content</b> - whenever a live entity's file still
 *     exists in git (whether or not it needed a rename), its current git content is pulled back
 *     into the DB cache field (groovyCode/publishingStrategy), overwriting whatever was cached
 *     there. This is what lets code be authored directly in Git (e.g. by an agent) and picked up
 *     by KSFX without ever touching the KSFX editor - reconciliation is the "bring the DB cache
 *     up to date" action for that workflow.</li>
 * </ul>
 * See {@link ActivityGitRepositoryService} for the underlying git plumbing.
 */
@Service
public class GitSyncReconciliationService
{
    private final ActivityDAO activityDAO;
    private final CodeLibDAO codeLibDAO;
    private final PublishingConfigurationDAO publishingConfigurationDAO;
    private final PublishingResourceDAO publishingResourceDAO;
    private final ActivityGitRepositoryService activityGitRepositoryService;
    private final SystemLogger systemLogger;

    public GitSyncReconciliationService(ActivityDAO activityDAO,
                                         CodeLibDAO codeLibDAO,
                                         PublishingConfigurationDAO publishingConfigurationDAO,
                                         PublishingResourceDAO publishingResourceDAO,
                                         ActivityGitRepositoryService activityGitRepositoryService,
                                         SystemLogger systemLogger)
    {
        this.activityDAO = activityDAO;
        this.codeLibDAO = codeLibDAO;
        this.publishingConfigurationDAO = publishingConfigurationDAO;
        this.publishingResourceDAO = publishingResourceDAO;
        this.activityGitRepositoryService = activityGitRepositoryService;
        this.systemLogger = systemLogger;
    }

    public String reconcile() throws GitAPIException, IOException
    {
        activityGitRepositoryService.sync();

        Result result = new Result();

        reconcileActivities(result);
        reconcileCodeLibs(result);
        reconcileReports(result);
        reconcileReportResources(result);

        if (result.gitChanges() > 0) {
            activityGitRepositoryService.commitAndPush("Reconciliation: sync Git with KSFX entries (deletes/renames)");
        }

        StringBuilder message = new StringBuilder();
        message.append(result.deleted.size()).append(" Datei(en) gelöscht, ")
                .append(result.renamed.size()).append(" umbenannt, ")
                .append(result.recreated.size()).append(" wiederhergestellt, ")
                .append(result.contentUpdated.size()).append(" Code-Cache(s) aus Git aktualisiert.");

        if (!result.deleted.isEmpty()) {
            message.append(" Gelöscht: ").append(String.join(", ", result.deleted)).append(".");
        }

        if (!result.renamed.isEmpty()) {
            message.append(" Umbenannt: ").append(String.join(", ", result.renamed)).append(".");
        }

        if (!result.recreated.isEmpty()) {
            message.append(" Wiederhergestellt: ").append(String.join(", ", result.recreated)).append(".");
        }

        if (!result.contentUpdated.isEmpty()) {
            message.append(" Code aktualisiert: ").append(String.join(", ", result.contentUpdated)).append(".");
        }

        systemLogger.logMessage("GITSYNC", "Reconciliation: " + message);

        return message.toString();
    }

    private void reconcileActivities(Result result) throws IOException
    {
        List<Activity> activities = activityDAO.getAllActivities();

        Set<String> expectedPaths = new HashSet<>();
        for (Activity activity : activities) {
            if (activity.getGitPath() != null) {
                expectedPaths.add(activity.getGitPath());
            }
        }

        removeOrphans(ActivityGitRepositoryService.ACTIVITIES_DIRECTORY, expectedPaths, result);

        Set<String> siblingPaths = new HashSet<>(expectedPaths);

        for (Activity activity : activities) {
            if (activity.getGitPath() == null) {
                continue;
            }

            siblingPaths.remove(activity.getGitPath());

            String desiredPath = activityGitRepositoryService.desiredPath(ActivityGitRepositoryService.ACTIVITIES_DIRECTORY, activity.getName(), activity.getGitPath(), siblingPaths);
            String resolvedContent = reconcileEntityFile(activity.getGitPath(), desiredPath, activity.getGroovyCode(), result);

            boolean pathChanged = !desiredPath.equals(activity.getGitPath());
            boolean contentChanged = !resolvedContent.equals(activity.getGroovyCode());

            if (pathChanged || contentChanged) {
                activity.setGitPath(desiredPath);
                activity.setGroovyCode(resolvedContent);
                activityDAO.saveOrUpdateActivity(activity);
            }

            siblingPaths.add(desiredPath);
        }
    }

    private void reconcileCodeLibs(Result result) throws IOException
    {
        List<CodeLib> codeLibs = codeLibDAO.getAllCodeLibs();

        Set<String> expectedPaths = new HashSet<>();
        for (CodeLib codeLib : codeLibs) {
            if (codeLib.getGitPath() != null) {
                expectedPaths.add(codeLib.getGitPath());
            }
        }

        removeOrphans(ActivityGitRepositoryService.LIBS_DIRECTORY, expectedPaths, result);

        Set<String> siblingPaths = new HashSet<>(expectedPaths);

        for (CodeLib codeLib : codeLibs) {
            if (codeLib.getGitPath() == null) {
                continue;
            }

            siblingPaths.remove(codeLib.getGitPath());

            String desiredPath = activityGitRepositoryService.desiredPath(ActivityGitRepositoryService.LIBS_DIRECTORY, codeLib.getName(), codeLib.getGitPath(), siblingPaths);
            String resolvedContent = reconcileEntityFile(codeLib.getGitPath(), desiredPath, codeLib.getGroovyCode(), result);

            boolean pathChanged = !desiredPath.equals(codeLib.getGitPath());
            boolean contentChanged = !resolvedContent.equals(codeLib.getGroovyCode());

            if (pathChanged || contentChanged) {
                codeLib.setGitPath(desiredPath);
                codeLib.setGroovyCode(resolvedContent);
                codeLibDAO.saveOrUpdateCodeLib(codeLib);
            }

            siblingPaths.add(desiredPath);
        }
    }

    private void reconcileReports(Result result) throws IOException
    {
        List<PublishingConfiguration> reports = publishingConfigurationDAO.getAllPublishingConfigurations();

        Set<String> expectedPaths = new HashSet<>();
        for (PublishingConfiguration report : reports) {
            if (report.getGitPath() != null) {
                expectedPaths.add(report.getGitPath());
            }
        }

        removeOrphans(ActivityGitRepositoryService.REPORTS_DIRECTORY, expectedPaths, result);

        Set<String> siblingPaths = new HashSet<>(expectedPaths);

        for (PublishingConfiguration report : reports) {
            if (report.getGitPath() == null) {
                continue;
            }

            siblingPaths.remove(report.getGitPath());

            String desiredPath = activityGitRepositoryService.desiredPath(ActivityGitRepositoryService.REPORTS_DIRECTORY, report.getName(), report.getGitPath(), siblingPaths);
            String resolvedContent = reconcileEntityFile(report.getGitPath(), desiredPath, report.getPublishingStrategy(), result);

            boolean pathChanged = !desiredPath.equals(report.getGitPath());
            boolean contentChanged = !resolvedContent.equals(report.getPublishingStrategy());

            if (pathChanged || contentChanged) {
                report.setGitPath(desiredPath);
                report.setPublishingStrategy(resolvedContent);
                publishingConfigurationDAO.saveOrUpdatePublishingConfiguration(report);
            }

            siblingPaths.add(desiredPath);
        }
    }

    private void reconcileReportResources(Result result) throws IOException
    {
        List<PublishingResource> resources = publishingResourceDAO.getAllPublishingResources();

        Set<String> expectedPaths = new HashSet<>();
        for (PublishingResource resource : resources) {
            if (resource.getGitPath() != null) {
                expectedPaths.add(resource.getGitPath());
            }
        }

        removeOrphans(ActivityGitRepositoryService.REPORT_RESOURCES_DIRECTORY, expectedPaths, result);

        Set<String> siblingPaths = new HashSet<>(expectedPaths);

        for (PublishingResource resource : resources) {
            if (resource.getGitPath() == null) {
                continue;
            }

            siblingPaths.remove(resource.getGitPath());

            String desiredPath = activityGitRepositoryService.desiredPath(ActivityGitRepositoryService.REPORT_RESOURCES_DIRECTORY, resource.getTitle(), resource.getGitPath(), siblingPaths);
            String resolvedContent = reconcileEntityFile(resource.getGitPath(), desiredPath, resource.getPublishingStrategy(), result);

            boolean pathChanged = !desiredPath.equals(resource.getGitPath());
            boolean contentChanged = !resolvedContent.equals(resource.getPublishingStrategy());

            if (pathChanged || contentChanged) {
                resource.setGitPath(desiredPath);
                resource.setPublishingStrategy(resolvedContent);
                publishingResourceDAO.saveOrUpdatePublishingResource(resource);
            }

            siblingPaths.add(desiredPath);
        }
    }

    /** Deletes any file under directory that doesn't correspond to a currently live entity's gitPath. */
    private void removeOrphans(String directory, Set<String> expectedPaths, Result result) throws IOException
    {
        for (String actualPath : activityGitRepositoryService.listFiles(directory)) {
            if (!expectedPaths.contains(actualPath)) {
                activityGitRepositoryService.deleteFile(actualPath);
                result.deleted.add(actualPath);
            }
        }
    }

    /**
     * Ensures the file exists at desiredGitPath (recreating it from cachedSource if it was
     * missing - KSFX is the master for existence, there's nothing else to recreate it from; or
     * moving it there if it was at a different path - the move itself doesn't touch content).
     * Returns the content that should now be cached in the DB: freshly read from Git if the file
     * already existed (Git is the master for content), or cachedSource itself if the file had to
     * be recreated from it.
     */
    private String reconcileEntityFile(String currentGitPath, String desiredGitPath, String cachedSource, Result result) throws IOException
    {
        if (!activityGitRepositoryService.fileExists(currentGitPath)) {
            String content = cachedSource != null ? cachedSource : "";
            activityGitRepositoryService.writeFile(desiredGitPath, content);
            result.recreated.add(desiredGitPath);

            return content;
        }

        if (!desiredGitPath.equals(currentGitPath)) {
            activityGitRepositoryService.moveFile(currentGitPath, desiredGitPath);
            result.renamed.add(currentGitPath + " -> " + desiredGitPath);
        }

        String gitContent = activityGitRepositoryService.readActivitySource(desiredGitPath);

        if (!gitContent.equals(cachedSource)) {
            result.contentUpdated.add(desiredGitPath);
        }

        return gitContent;
    }

    private static class Result
    {
        List<String> deleted = new ArrayList<>();
        List<String> renamed = new ArrayList<>();
        List<String> recreated = new ArrayList<>();
        List<String> contentUpdated = new ArrayList<>();

        /** Git-side changes only - contentUpdated is DB-side (pulled from Git) and needs no commit. */
        int gitChanges()
        {
            return deleted.size() + renamed.size() + recreated.size();
        }
    }
}
