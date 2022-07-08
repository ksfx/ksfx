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

package ch.ksfx.services.publishing;

import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.services.activity.ActivityInstanceRun;
import ch.ksfx.services.activity.RunningActivitiesCache;
import ch.ksfx.services.systemlogger.SystemLogger;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class PublicationLoaderRunner
{
    private ThreadPoolExecutor threadPoolExecutor;
    private final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(5);

    private SystemLogger systemLogger;
    private ServiceProvider serviceProvider;
    private PublishingConfigurationDAO publishingConfigurationDAO;
    private Environment environment;

    public PublicationLoaderRunner(SystemLogger systemLogger, ServiceProvider serviceProvider, PublishingConfigurationDAO publishingConfigurationDAO, Environment environment)
    {
        this.systemLogger = systemLogger;
        this.serviceProvider = serviceProvider;
        this.publishingConfigurationDAO = publishingConfigurationDAO;
        this.environment = environment;

        this.threadPoolExecutor = new ThreadPoolExecutor(2, 100, 100, TimeUnit.SECONDS, queue);
    }

    public boolean isPublicationRunning(PublishingConfiguration publishingConfiguration)
    {
        return RunningPublicationsCache.runningPublications.containsKey(publishingConfiguration.getId());
    }

//    public void runPublication(PublishingConfiguration publishingConfiguration)
//    {
//        loadPublication(publishingConfiguration);
//    }

    public void loadPublication(PublishingConfiguration publishingConfiguration)
    {
        PublicationLoad publicationLoad = new PublicationLoad(systemLogger, serviceProvider, publishingConfiguration, publishingConfigurationDAO, environment);
        RunningPublicationsCache.runningPublications.put(publishingConfiguration.getId(), publicationLoad);

        threadPoolExecutor.execute(publicationLoad);
    }

//    public void terminateActivity(PublishingConfiguration publishingConfiguration)
//    {
//        if (RunningPublicationsCache.runningPublications.containsKey(publishingConfiguration.getId())) {
//            RunningPublicationsCache.runningPublications.get(publishingConfiguration.getId()).terminateSpidering();
//        }
//    }
}
