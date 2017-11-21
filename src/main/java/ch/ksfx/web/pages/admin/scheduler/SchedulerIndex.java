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

package ch.ksfx.web.pages.admin.scheduler;

import ch.ksfx.web.services.scheduler.SchedulerService;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.quartz.SchedulerException;
import org.springframework.security.access.annotation.Secured;

import java.util.Date;
import java.util.List;

/**
 * Created by Kejo on 14.01.2015.
 */
@Secured({"ROLE_ADMIN"})
public class SchedulerIndex
{
    @Inject
    private SchedulerService schedulerService;

    @Property
    private String job;

    @Property
    private String trigger;

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {

    }

    public List<String> getCurrentSchedules() throws SchedulerException
    {
        return schedulerService.getAllRunningJobs();
    }

    public List<String> getCurrentTriggers() throws SchedulerException
    {
        return schedulerService.getAllTriggers();
    }

    public void onActionFromPauseJob(String job) throws SchedulerException
    {
        schedulerService.pauseJob(job.split("\\.")[1], job.split("\\.")[0]);
    }

    public void onActionFromDeleteJob(String job) throws SchedulerException
    {
        schedulerService.deleteJob(job.split("\\.")[1], job.split("\\.")[0]);
    }

    public void onActionFromResumeJob(String job) throws SchedulerException
    {
        schedulerService.resumeJob(job.split("\\.")[1], job.split("\\.")[0]);
    }

    public boolean getPaused() throws SchedulerException
    {
        return schedulerService.isPaused(job.split("\\.")[1] + "Trigger");
    }

    public Date getNextFireTime() throws SchedulerException
    {
        System.out.println(job.split("\\.")[1] + "Trigger");
        return schedulerService.getNextFireTime(job.split("\\.")[1] + "Trigger");
    }
}
