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

package ch.ksfx.services.scheduler;

import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.dao.spidering.SpideringConfigurationDAO;
import ch.ksfx.dao.spidering.SpideringDAO;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.spidering.SpideringConfiguration;
import ch.ksfx.services.activity.ActivityInstanceJob;
import ch.ksfx.services.configurationdatabase.ConfigurationDatabaseProvider;
import ch.ksfx.services.spidering.SpideringJob;
import ch.ksfx.services.activity.ActivityInstanceRunner;
import ch.ksfx.services.systemlogger.SystemLogger;
import ch.ksfx.services.spidering.SpideringRunner;
import ch.ksfx.services.SystemEnvironment;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Component
public class SchedulerService
{
    private ConfigurationDatabaseProvider configurationDatabaseProvider;
    private SpideringConfigurationDAO spideringConfigurationDAO;
    private SchedulerFactory schedulerFactory;
    private Scheduler scheduler;
    private SystemLogger systemLogger;
    private SystemEnvironment systemEnvironment;
    private SpideringRunner spideringRunner;
    private SpideringDAO spideringDAO;
    private ActivityDAO activityDAO;
    private ActivityInstanceDAO activityInstanceDAO;
    private ActivityInstanceRunner activityInstanceRunner;

    private Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    public SchedulerService(ConfigurationDatabaseProvider configurationDatabaseProvider, SystemLogger systemLogger, SystemEnvironment systemEnvironment, SpideringRunner spideringRunner, SpideringDAO spideringDAO, SpideringConfigurationDAO spideringConfigurationDAO, ActivityDAO activityDAO, ActivityInstanceDAO activityInstanceDAO, ActivityInstanceRunner activityInstanceRunner)
    {
        this.configurationDatabaseProvider = configurationDatabaseProvider;
        this.systemLogger = systemLogger;
        this.systemEnvironment = systemEnvironment;
        this.spideringRunner = spideringRunner;
        this.spideringDAO = spideringDAO;
        this.spideringConfigurationDAO = spideringConfigurationDAO;
        this.activityDAO = activityDAO;
        this.activityInstanceDAO = activityInstanceDAO;
        this.activityInstanceRunner = activityInstanceRunner;
    }

    @PostConstruct
    public void initializeScheduler()
    {
        logger.info("======================> Staring Scheduler");

        schedulerFactory = new StdSchedulerFactory();

        try {
            scheduler = schedulerFactory.getScheduler();
            scheduler.start();

            startSpideringSchedules();
            startActivitySchedules();

        } catch (Exception e) {
            systemLogger.logMessage("FATAL","Error while starting scheduler service", e);
        }
    }

    public List<String> getAllRunningJobs() throws SchedulerException
    {
        List<String> runningJobs = new ArrayList<String>();

        for(String group: scheduler.getJobGroupNames()) {
            for(JobKey jobKey : scheduler.getJobKeys(GroupMatcher.<JobKey>groupEquals(group))) {
                System.out.println("Found job identified by: " + jobKey);
                runningJobs.add(jobKey.toString());
            }
        }

        return runningJobs;
    }

    public List<String> getAllTriggers() throws SchedulerException
    {
        List<String> triggers = new ArrayList<String>();

        for(String group: scheduler.getTriggerGroupNames()) {
            for(TriggerKey triggerKey : scheduler.getTriggerKeys(GroupMatcher.<TriggerKey>groupEquals(group))) {
                System.out.println("Found trigger identified by: " + triggerKey);
                triggers.add(triggerKey.toString());
            }
        }

        return triggers;
    }

    public void scheduleSpidering(SpideringConfiguration spideringConfiguration)
    {
        JobDetail spideringJob = newJob(SpideringJob.class)
                .withIdentity("Spidering" + spideringConfiguration.getId().toString(), "Spiderings")
                .build();

        Trigger trigger = newTrigger()
                .withIdentity("Spidering" + spideringConfiguration.getId().toString() + "Trigger")
                .withSchedule(cronSchedule(spideringConfiguration.getCronSchedule()))
                .build();

        spideringJob.getJobDataMap().put("spideringDao", spideringDAO);
        spideringJob.getJobDataMap().put("spideringConfigurationDao", spideringConfigurationDAO);
        spideringJob.getJobDataMap().put("spideringConfigurationId", spideringConfiguration.getId());
        spideringJob.getJobDataMap().put("spideringRunner", spideringRunner);
        spideringJob.getJobDataMap().put("systemLogger", systemLogger);

        try {
            scheduler.scheduleJob(spideringJob, trigger);
        } catch (SchedulerException e) {
            systemLogger.logMessage("FATAL","Could not schedule Spidering job", e);
        }
    }

    public void scheduleActivity(Activity activity)
    {
        JobDetail spideringJob = newJob(ActivityInstanceJob.class)
                .withIdentity("Activity" + activity.getId().toString(), "Activities")
                .build();

        Trigger trigger = newTrigger()
                .withIdentity("Activity" + activity.getId().toString() + "Trigger")
                .withSchedule(cronSchedule(activity.getCronSchedule()))
                .build();

        spideringJob.getJobDataMap().put("activityDao", activityDAO);
        spideringJob.getJobDataMap().put("activityInstanceDao", activityInstanceDAO);
        spideringJob.getJobDataMap().put("activityId", activity.getId());
        spideringJob.getJobDataMap().put("activityInstanceRunner", activityInstanceRunner);
        spideringJob.getJobDataMap().put("systemLogger", systemLogger);

        try {
            scheduler.scheduleJob(spideringJob, trigger);
        } catch (SchedulerException e) {
            systemLogger.logMessage("FATAL","Could not schedule Spidering job", e);
        }
    }

    public void pauseJob(String jobName, String groupName) throws SchedulerException
    {
        JobKey jobKey = new JobKey(jobName, groupName);
        scheduler.pauseJob(jobKey);
    }

    public void resumeJob(String jobName, String groupName) throws SchedulerException
    {
        JobKey jobKey = new JobKey(jobName, groupName);
        scheduler.resumeJob(jobKey);
    }

    public void deleteJob(String jobName, String groupName) throws SchedulerException
    {
        JobKey jobKey = new JobKey(jobName, groupName);
        scheduler.deleteJob(jobKey);
    }

    public boolean jobExists(String jobName, String groupName) throws SchedulerException
    {
        JobKey jobKey = new JobKey(jobName, groupName);
        return scheduler.checkExists(jobKey);
    }

    public boolean isPaused(String triggerName) throws SchedulerException
    {
        TriggerKey triggerKey = new TriggerKey(triggerName);

        return (scheduler.getTriggerState(triggerKey) == Trigger.TriggerState.PAUSED);
    }

    public Date getNextFireTime(String triggerName) throws SchedulerException
    {
        TriggerKey triggerKey = new TriggerKey(triggerName);

        return scheduler.getTrigger(triggerKey).getNextFireTime();
    }

    private void startSpideringSchedules()
    {
        List<SpideringConfiguration> spideringConfigurations = spideringConfigurationDAO.getScheduledSpideringConfigurations();

        for (SpideringConfiguration spideringConfiguration : spideringConfigurations) {
            scheduleSpidering(spideringConfiguration);
        }
    }

    private void startActivitySchedules()
    {
        List<Activity> activities = activityDAO.getScheduledActivities();

        for (Activity activity : activities) {
            scheduleActivity(activity);
        }
    }
}
