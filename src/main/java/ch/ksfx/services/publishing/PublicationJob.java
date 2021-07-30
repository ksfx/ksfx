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
import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.services.activity.ActivityInstanceRunner;
import ch.ksfx.services.systemlogger.SystemLogger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;


public class PublicationJob implements Job
{
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();

        Long publishingConfigurationId = (Long) jobDataMap.get("publishingConfigurationId");
        SystemLogger systemLogger = (SystemLogger) jobDataMap.get("systemLogger");
        PublishingConfigurationDAO publishingConfigurationDAO = (PublishingConfigurationDAO) jobDataMap.get("publishingConfigurationDAO");
        PublicationLoaderRunner publicationLoaderRunner = (PublicationLoaderRunner) jobDataMap.get("publicationLoaderRunner");

        PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);

        publicationLoaderRunner.loadPublication(publishingConfiguration);
    }
}