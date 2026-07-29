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
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Makes Git mirror KSFX's database exactly for filenames/existence - the database is the master
 * for which entries exist and what they're named. Git remains the master for the actual source
 * code content (see {@link ActivityGitRepositoryService}). Concretely, for each of
 * Activity/CodeLib/PublishingConfiguration/PublishingResource:
 * <ul>
 *     <li>a git file with no matching live gitPath is deleted (the KSFX row was deleted)</li>
 *     <li>a live entity whose name/title no longer matches its gitPath's slug gets its file
 *     renamed (collision-aware against sibling entities' current paths - a slug freed up by
 *     another rename/delete in the same run is reclaimed)</li>
 *     <li>a live entity whose file is missing from git gets it recreated from the DB-cached
 *     source</li>
 * </ul>
 */
@Service
public class GitSyncReconciliationService
{
    private final ActivityDAO activityDAO;
    private final CodeLibDAO codeLibDAO;
    private final PublishingConfigurationDAO publishingConfigurationDAO;
    private final PublishingResourceDAO publishingResourceDAO;
    private final ActivityGitRepositoryService activityGitRepositoryService;

    public GitSyncReconciliationService(ActivityDAO activityDAO,
                                         CodeLibDAO codeLibDAO,
                                         PublishingConfigurationDAO publishingConfigurationDAO,
                                         PublishingResourceDAO publishingResourceDAO,
                                         ActivityGitRepositoryService activityGitRepositoryService)
    {
        this.activityDAO = activityDAO;
        this.codeLibDAO = codeLibDAO;
        this.publishingConfigurationDAO = publishingConfigurationDAO;
        this.publishingResourceDAO = publishingResourceDAO;
        this.activityGitRepositoryService = activityGitRepositoryService;
    }

    public String reconcile() throws GitAPIException, IOException
    {
        activityGitRepositoryService.sync();

        Result result = new Result();

        reconcileActivities(result);
        reconcileCodeLibs(result);
        reconcileReports(result);
        reconcileReportResources(result);

        if (result.total() > 0) {
            activityGitRepositoryService.commitAndPush("Reconciliation: sync Git with KSFX entries (deletes/renames)");
        }

        StringBuilder message = new StringBuilder();
        message.append(result.deleted.size()).append(" Datei(en) gelöscht, ")
                .append(result.renamed.size()).append(" umbenannt, ")
                .append(result.recreated.size()).append(" wiederhergestellt.");

        if (!result.deleted.isEmpty()) {
            message.append(" Gelöscht: ").append(String.join(", ", result.deleted)).append(".");
        }

        if (!result.renamed.isEmpty()) {
            message.append(" Umbenannt: ").append(String.join(", ", result.renamed)).append(".");
        }

        if (!result.recreated.isEmpty()) {
            message.append(" Wiederhergestellt: ").append(String.join(", ", result.recreated)).append(".");
        }

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

            reconcileEntityFile(activity.getGitPath(), desiredPath, activity.getGroovyCode(), result);

            if (!desiredPath.equals(activity.getGitPath())) {
                activity.setGitPath(desiredPath);
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

            reconcileEntityFile(codeLib.getGitPath(), desiredPath, codeLib.getGroovyCode(), result);

            if (!desiredPath.equals(codeLib.getGitPath())) {
                codeLib.setGitPath(desiredPath);
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

            reconcileEntityFile(report.getGitPath(), desiredPath, report.getPublishingStrategy(), result);

            if (!desiredPath.equals(report.getGitPath())) {
                report.setGitPath(desiredPath);
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

            reconcileEntityFile(resource.getGitPath(), desiredPath, resource.getPublishingStrategy(), result);

            if (!desiredPath.equals(resource.getGitPath())) {
                resource.setGitPath(desiredPath);
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

    private void reconcileEntityFile(String currentGitPath, String desiredGitPath, String cachedSource, Result result) throws IOException
    {
        if (!activityGitRepositoryService.fileExists(currentGitPath)) {
            activityGitRepositoryService.writeFile(desiredGitPath, cachedSource != null ? cachedSource : "");
            result.recreated.add(desiredGitPath);
        } else if (!desiredGitPath.equals(currentGitPath)) {
            activityGitRepositoryService.moveFile(currentGitPath, desiredGitPath);
            result.renamed.add(currentGitPath + " -> " + desiredGitPath);
        }
    }

    private static class Result
    {
        List<String> deleted = new ArrayList<>();
        List<String> renamed = new ArrayList<>();
        List<String> recreated = new ArrayList<>();

        int total()
        {
            return deleted.size() + renamed.size() + recreated.size();
        }
    }
}
