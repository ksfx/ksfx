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

package ch.ksfx.services.spidering;

import ch.ksfx.dao.spidering.SpideringConfigurationDAO;
import ch.ksfx.dao.spidering.SpideringDAO;
import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.services.systemlogger.SystemLogger;
import ch.ksfx.services.spidering.SpideringRunner;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;


public class SpideringJob implements Job
{
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();

        Long spideringConfigurationId = (Long) jobDataMap.get("spideringConfigurationId");
        SystemLogger systemLogger = (SystemLogger) jobDataMap.get("systemLogger");
        SpideringDAO spideringDAO = (SpideringDAO) jobDataMap.get("spideringDao");
        SpideringConfigurationDAO spideringConfigurationDAO = (SpideringConfigurationDAO) jobDataMap.get("spideringConfigurationDao");
        SpideringRunner spideringRunner = (SpideringRunner) jobDataMap.get("spideringRunner");

        Spidering spidering = new Spidering();
        spidering.setSpideringConfiguration(spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId));
        spidering.setStarted(new Date());

        spideringDAO.saveOrUpdate(spidering);

        spideringRunner.runSpidering(spidering);
    }
}
