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

package ch.ksfx.web.services.scheduler;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.spidering.SpideringConfiguration;
import org.quartz.*;
import java.util.Date;
import java.util.List;


public interface SchedulerService
{
    public void initializeScheduler();
    public List<String> getAllRunningJobs() throws SchedulerException;
    public List<String> getAllTriggers() throws SchedulerException;
    public void scheduleSpidering(SpideringConfiguration spideringConfiguration);
    public void scheduleActivity(Activity activity);
    public void pauseJob(String jobName, String groupName) throws SchedulerException;
    public void resumeJob(String jobName, String groupName) throws SchedulerException;
    public void deleteJob(String jobName, String groupName) throws SchedulerException;
    public boolean jobExists(String jobName, String groupName) throws SchedulerException;
    public boolean isPaused(String triggerName) throws SchedulerException;
    public Date getNextFireTime(String triggerName) throws SchedulerException;
}
