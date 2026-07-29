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

/**
 * Batch migration/unlink of Activity, CodeLib and Publishing (report) source code against this
 * instance's Git repository (see {@link ActivityGitRepositoryService}). Idempotent - skips
 * anything that already has a gitPath, so it can be re-run incrementally.
 */
@Service
public class GitSyncMigrationService
{
    private final ActivityDAO activityDAO;
    private final CodeLibDAO codeLibDAO;
    private final PublishingConfigurationDAO publishingConfigurationDAO;
    private final PublishingResourceDAO publishingResourceDAO;
    private final ActivityGitRepositoryService activityGitRepositoryService;

    public GitSyncMigrationService(ActivityDAO activityDAO,
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

    public String migrateAllToGit() throws GitAPIException, IOException
    {
        activityGitRepositoryService.sync();

        int migratedActivities = 0;

        for (Activity activity : activityDAO.getAllActivities()) {
            if (activity.getGitPath() != null) {
                continue;
            }

            if (activity.getGroovyCode() == null || activity.getGroovyCode().isEmpty()) {
                continue;
            }

            String slug = activityGitRepositoryService.uniqueSlug(
                    activityGitRepositoryService.slugify(activity.getName()),
                    ActivityGitRepositoryService.ACTIVITIES_DIRECTORY);
            String gitPath = ActivityGitRepositoryService.ACTIVITIES_DIRECTORY + "/" + slug + ".groovy";

            activityGitRepositoryService.writeFile(gitPath, activity.getGroovyCode());

            activity.setGitPath(gitPath);
            activityDAO.saveOrUpdateActivity(activity);

            migratedActivities++;
        }

        int migratedCodeLibs = 0;

        for (CodeLib codeLib : codeLibDAO.getAllCodeLibs()) {
            if (codeLib.getGitPath() != null) {
                continue;
            }

            if (codeLib.getGroovyCode() == null || codeLib.getGroovyCode().isEmpty()) {
                continue;
            }

            String slug = activityGitRepositoryService.uniqueSlug(
                    activityGitRepositoryService.slugify(codeLib.getName()),
                    ActivityGitRepositoryService.LIBS_DIRECTORY);
            String gitPath = ActivityGitRepositoryService.LIBS_DIRECTORY + "/" + slug + ".groovy";

            activityGitRepositoryService.writeFile(gitPath, codeLib.getGroovyCode());

            codeLib.setGitPath(gitPath);
            codeLibDAO.saveOrUpdateCodeLib(codeLib);

            migratedCodeLibs++;
        }

        int migratedReports = 0;

        for (PublishingConfiguration publishingConfiguration : publishingConfigurationDAO.getAllPublishingConfigurations()) {
            if (publishingConfiguration.getGitPath() != null) {
                continue;
            }

            if (publishingConfiguration.getPublishingStrategy() == null || publishingConfiguration.getPublishingStrategy().isEmpty()) {
                continue;
            }

            String slug = activityGitRepositoryService.uniqueSlug(
                    activityGitRepositoryService.slugify(publishingConfiguration.getName()),
                    ActivityGitRepositoryService.REPORTS_DIRECTORY);
            String gitPath = ActivityGitRepositoryService.REPORTS_DIRECTORY + "/" + slug + ".groovy";

            activityGitRepositoryService.writeFile(gitPath, publishingConfiguration.getPublishingStrategy());

            publishingConfiguration.setGitPath(gitPath);
            publishingConfigurationDAO.saveOrUpdatePublishingConfiguration(publishingConfiguration);

            migratedReports++;
        }

        int migratedReportResources = 0;

        for (PublishingResource publishingResource : publishingResourceDAO.getAllPublishingResources()) {
            if (publishingResource.getGitPath() != null) {
                continue;
            }

            if (publishingResource.getPublishingStrategy() == null || publishingResource.getPublishingStrategy().isEmpty()) {
                continue;
            }

            String slug = activityGitRepositoryService.uniqueSlug(
                    activityGitRepositoryService.slugify(publishingResource.getTitle()),
                    ActivityGitRepositoryService.REPORT_RESOURCES_DIRECTORY);
            String gitPath = ActivityGitRepositoryService.REPORT_RESOURCES_DIRECTORY + "/" + slug + ".groovy";

            activityGitRepositoryService.writeFile(gitPath, publishingResource.getPublishingStrategy());

            publishingResource.setGitPath(gitPath);
            publishingResourceDAO.saveOrUpdatePublishingResource(publishingResource);

            migratedReportResources++;
        }

        if (migratedActivities > 0 || migratedCodeLibs > 0 || migratedReports > 0 || migratedReportResources > 0) {
            activityGitRepositoryService.commitAndPush("Initial migration of activity, code lib and report scripts from database");
        }

        return migratedActivities + " Activity/-ies, " + migratedCodeLibs + " Code Lib(s), "
                + migratedReports + " Report(s) and " + migratedReportResources + " Report Resource(s) migrated.";
    }

    /**
     * Reverts every Git-linked Activity, CodeLib and Publishing entity back to DB-only (clears
     * gitPath, the DB-cached source is untouched so nothing is lost) and deletes the local clone,
     * so the migration can be re-run from scratch or the admin can point this instance at a
     * different repository.
     */
    public String unlinkAllFromGit() throws IOException
    {
        int unlinkedActivities = 0;

        for (Activity activity : activityDAO.getAllActivities()) {
            if (activity.getGitPath() != null) {
                activity.setGitPath(null);
                activityDAO.saveOrUpdateActivity(activity);

                unlinkedActivities++;
            }
        }

        int unlinkedCodeLibs = 0;

        for (CodeLib codeLib : codeLibDAO.getAllCodeLibs()) {
            if (codeLib.getGitPath() != null) {
                codeLib.setGitPath(null);
                codeLibDAO.saveOrUpdateCodeLib(codeLib);

                unlinkedCodeLibs++;
            }
        }

        int unlinkedReports = 0;

        for (PublishingConfiguration publishingConfiguration : publishingConfigurationDAO.getAllPublishingConfigurations()) {
            if (publishingConfiguration.getGitPath() != null) {
                publishingConfiguration.setGitPath(null);
                publishingConfigurationDAO.saveOrUpdatePublishingConfiguration(publishingConfiguration);

                unlinkedReports++;
            }
        }

        int unlinkedReportResources = 0;

        for (PublishingResource publishingResource : publishingResourceDAO.getAllPublishingResources()) {
            if (publishingResource.getGitPath() != null) {
                publishingResource.setGitPath(null);
                publishingResourceDAO.saveOrUpdatePublishingResource(publishingResource);

                unlinkedReportResources++;
            }
        }

        activityGitRepositoryService.deleteLocalClone();

        return unlinkedActivities + " Activity/-ies, " + unlinkedCodeLibs + " Code Lib(s), "
                + unlinkedReports + " Report(s) and " + unlinkedReportResources + " Report Resource(s) unlinked from Git, local clone removed.";
    }
}
