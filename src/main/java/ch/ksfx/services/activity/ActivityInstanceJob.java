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

package ch.ksfx.services.activity;

import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.services.systemlogger.SystemLogger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;


public class ActivityInstanceJob implements Job
{
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();

        Long activityId = (Long) jobDataMap.get("activityId");
        SystemLogger systemLogger = (SystemLogger) jobDataMap.get("systemLogger");
        ActivityInstanceDAO activityInstanceDAO = (ActivityInstanceDAO) jobDataMap.get("activityInstanceDao");
        ActivityDAO activityDAO = (ActivityDAO) jobDataMap.get("activityDao");
        ActivityInstanceRunner activityInstanceRunner = (ActivityInstanceRunner) jobDataMap.get("activityInstanceRunner");

        ActivityInstance activityInstance = new ActivityInstance();
        activityInstance.setActivity(activityDAO.getActivityForId(activityId));
        activityInstance.setStarted(new Date());

        activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);

        activityInstanceRunner.runActivity(activityInstance);
    }
}
