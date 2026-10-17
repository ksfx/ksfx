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

package ch.ksfx.services.codelib;

import ch.ksfx.dao.CodeLibDAO;
import java.util.List;
import ch.ksfx.util.Console;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.CodeLib;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.services.git.ActivityGitRepositoryService;
import ch.ksfx.services.systemlogger.SystemLogger;
import groovy.lang.GroovyClassLoader;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;

/**
 * Loads a {@link CodeLib} by name and instantiates its Groovy class, reading the source from Git
 * when the CodeLib has a gitPath and Git sync is active (falling back to the DB-cached groovyCode
 * on any read failure) - mirrors {@code EbeanActivityExecutionDAO.getActivityExecution()} for
 * Activity, but for CodeLib. Lets Groovy scripts (Activities, and eventually Publishing
 * Strategies - see the class Javadoc on {@link CodeLib}) replace ad-hoc NoteFile/GenericDataStore
 * lookups with a single call that's actually git-sync-aware, instead of hand-rolling (and
 * inevitably drifting from) this same resolve-then-compile-then-instantiate logic themselves.
 *
 * This is the rewiring {@link CodeLibMigrationService}'s Javadoc explicitly deferred as "a
 * separate follow-up" when the SQL writer was first migrated out of its NoteFile.
 *
 * The instantiated class must have a public constructor taking a single {@link ServiceProvider}
 * argument - the same convention {@code ActivityExecution} implementations already follow.
 */
@Service
public class CodeLibLoaderService
{
    private final CodeLibDAO codeLibDAO;
    private final ActivityGitRepositoryService activityGitRepositoryService;
    private final SystemLogger systemLogger;
    private final ActivityInstanceDAO activityInstanceDAO;
    private final PublishingConfigurationDAO publishingConfigurationDAO;

    public CodeLibLoaderService(CodeLibDAO codeLibDAO,
                                 ActivityGitRepositoryService activityGitRepositoryService,
                                 SystemLogger systemLogger, ActivityInstanceDAO activityInstanceDAO, PublishingConfigurationDAO publishingConfigurationDAO)
    {
        this.codeLibDAO = codeLibDAO;
        this.activityGitRepositoryService = activityGitRepositoryService;
        this.systemLogger = systemLogger;
        this.activityInstanceDAO = activityInstanceDAO;
        this.publishingConfigurationDAO = publishingConfigurationDAO;
    }

    public Object instantiate(String codeLibName, ServiceProvider serviceProvider)
    {
        CodeLib codeLib = resolve(codeLibName);

        if (codeLib == null) {
            throw new IllegalArgumentException("CodeLib not found: " + codeLibName);
        }

        String groovyCode = codeLib.getGroovyCode();

        if (ActivityGitRepositoryService.hasGitPath(codeLib.getGitPath()) && activityGitRepositoryService.isActive(codeLib.getProject())) {
            try {
                activityGitRepositoryService.sync(codeLib.getProject());
                groovyCode = activityGitRepositoryService.readActivitySource(codeLib.getProject(), codeLib.getGitPath());
            } catch (Exception e) {
                systemLogger.logMessage("WARN", "Could not read CodeLib '" + codeLibName + "' source from Git, falling back to cached groovyCode", e);
            }
        }

        if (groovyCode == null || groovyCode.isEmpty()) {
            throw new IllegalArgumentException("CodeLib has no code: " + codeLibName);
        }

        try {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(groovyCode);
            Constructor cons = clazz.getDeclaredConstructor(ServiceProvider.class);

            return cons.newInstance(serviceProvider);
        } catch (Exception e) {
            throw new RuntimeException("Could not instantiate CodeLib '" + codeLibName + "'", e);
        }
    }

    /**
     * Code Libs belong to projects (2026-10-10), so a name alone may match several. The one of
     * the calling run's project wins: the running thread knows its activity instance or
     * publishing configuration (Console's thread-locals), and that entity knows its project. A
     * single match is used regardless (an activity may legitimately use a lib of another project
     * as long as the name is unambiguous); several matches without a resolvable project context
     * are an error rather than a coin toss.
     */
    private CodeLib resolve(String codeLibName)
    {
        List<CodeLib> candidates = codeLibDAO.getCodeLibsForName(codeLibName);

        if (candidates.isEmpty()) {
            return null;
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        Long projectId = currentProjectId();

        if (projectId != null) {
            for (CodeLib candidate : candidates) {
                if (candidate.getProject() != null && projectId.equals(candidate.getProject().getId())) {
                    return candidate;
                }
            }
        }

        throw new IllegalArgumentException("CodeLib name '" + codeLibName + "' exists in " + candidates.size()
                + " projects and the calling run's project " + (projectId == null ? "is unknown" : "has no lib of that name")
                + " - rename one of them or give this run's project its own copy");
    }

    private Long currentProjectId()
    {
        Long activityInstanceId = Console.currentActivityInstanceId();

        if (activityInstanceId != null) {
            ActivityInstance instance = activityInstanceDAO.getActivityInstanceForId(activityInstanceId);

            if (instance != null && instance.getActivity() != null && instance.getActivity().getProject() != null) {
                return instance.getActivity().getProject().getId();
            }
        }

        Long publishingConfigurationId = Console.currentPublishingConfigurationId();

        if (publishingConfigurationId != null) {
            PublishingConfiguration configuration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);

            if (configuration != null && configuration.getProject() != null) {
                return configuration.getProject().getId();
            }
        }

        return null;
    }
}
