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

    public CodeLibLoaderService(CodeLibDAO codeLibDAO,
                                 ActivityGitRepositoryService activityGitRepositoryService,
                                 SystemLogger systemLogger)
    {
        this.codeLibDAO = codeLibDAO;
        this.activityGitRepositoryService = activityGitRepositoryService;
        this.systemLogger = systemLogger;
    }

    public Object instantiate(String codeLibName, ServiceProvider serviceProvider)
    {
        CodeLib codeLib = codeLibDAO.getCodeLibForName(codeLibName);

        if (codeLib == null) {
            throw new IllegalArgumentException("CodeLib not found: " + codeLibName);
        }

        String groovyCode = codeLib.getGroovyCode();

        if (codeLib.getGitPath() != null && activityGitRepositoryService.isActive()) {
            try {
                activityGitRepositoryService.sync();
                groovyCode = activityGitRepositoryService.readActivitySource(codeLib.getGitPath());
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
}
