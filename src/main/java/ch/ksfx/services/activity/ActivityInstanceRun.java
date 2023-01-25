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

import ch.ksfx.dao.activity.ActivityExecutionDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.dao.ebean.activity.EbeanActivityExecutionDAO;
import ch.ksfx.dao.ebean.activity.EbeanActivityInstanceDAO;
import ch.ksfx.model.activity.ActivityExecution;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.activity.ActivityInstancePersistentData;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.util.Console;
import ch.ksfx.services.systemlogger.SystemLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;


public class ActivityInstanceRun implements Runnable
{
    private SystemLogger systemLogger;
    private ServiceProvider serviceProvider;
    private boolean isRunning = true;
    private ActivityInstance activityInstance;
    private Logger logger = LoggerFactory.getLogger(ActivityInstanceRun.class);

    private ActivityInstanceDAO activityInstanceDAO;
    private ActivityExecutionDAO activityExecutionDAO;

    private ActivityExecution activityExecution;

    public ActivityInstanceRun(SystemLogger systemLogger, ServiceProvider serviceProvider, ActivityInstance activityInstance)
    {
        this.systemLogger = systemLogger;
        this.serviceProvider = serviceProvider;
        this.activityInstance = activityInstance;

        activityInstanceDAO = new EbeanActivityInstanceDAO();
        activityExecutionDAO = new EbeanActivityExecutionDAO(systemLogger, serviceProvider);

        this.activityInstance.setApproved(true);
        this.activityInstance.setStarted(new Date());
        activityInstanceDAO.saveOrUpdateActivityInstance(this.activityInstance);

        activityExecution = activityExecutionDAO.getActivityExecution(activityInstance.getActivity());
    }

    public void terminateSpidering()
    {
        activityExecution.terminateActivity();
    }

    @Override
    public void run()
    {
        try {
        	Console.startConsole(activityInstance);
        	
            System.out.println("Execution " + activityExecution);
            System.out.println("Instance " + activityInstance);
            List<ActivityInstancePersistentData> activityInstancePersistentDatas = activityExecution.executeActivity(activityInstance.getActivityInstanceParameters());

            for (ActivityInstancePersistentData activityInstancePersistentData : activityInstancePersistentDatas) {
                activityInstancePersistentData.setActivityInstance(activityInstance);
                activityInstanceDAO.saveOrUpdateActivityInstancePersistentData(activityInstancePersistentData);
            }

        } catch (Throwable e) {
            systemLogger.logMessage("FATAL_ACTIVITY","Error while executing activity", e);
            logger.error("Error while executing activity",e);
        } finally {
            RunningActivitiesCache.runningActivities.remove(activityInstance.getId());

            systemLogger.logMessage("ACTIVITY_DEBUG", "Finishing ActivityInstance... ");
            systemLogger.logMessage("ACTIVITY_DEBUG", "...with id: " + activityInstance.getId());
            systemLogger.logMessage("ACTIVITY_DEBUG", "Currently running activity instances: " + RunningActivitiesCache.runningActivities.keySet().toString());

        	activityInstance = activityInstanceDAO.getActivityInstanceForId(activityInstance.getId());
            activityInstance.setFinished(new Date());
            activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);
            
            Console.endConsole();
        }
    }

    public ActivityInstance getActivityInstance()
    {
        return activityInstance;
    }
}
