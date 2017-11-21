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

package ch.ksfx.web.services.activity;

import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.util.RunningActivitiesCache;
import ch.ksfx.web.services.ObjectLocatorService;
import ch.ksfx.web.services.logger.SystemLogger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class ActivityInstanceRunner
{
    private ThreadPoolExecutor threadPoolExecutor;
    private final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(5);

    private SystemLogger systemLogger;
    private ObjectLocatorService objectLocatorService;

    public ActivityInstanceRunner(SystemLogger systemLogger, ObjectLocatorService objectLocatorService)
    {
        this.systemLogger = systemLogger;
        this.objectLocatorService = objectLocatorService;

        this.threadPoolExecutor = new ThreadPoolExecutor(2, 5, 100, TimeUnit.SECONDS, queue);
    }

    public boolean isActivityInstanceRunning(ActivityInstance activityInstance)
    {
        return RunningActivitiesCache.runningActivities.containsKey(activityInstance.getId());
    }

    public void runActivity(ActivityInstance activityInstance)
    {
        if (activityInstance.getApproved() || activityInstance.getActivity().getActivityApprovalStrategy().getName().equalsIgnoreCase("none")) {
            startActivity(activityInstance);
        }
    }

    private void startActivity(ActivityInstance activityInstance)
    {
        ActivityInstanceRun activityInstanceRun = new ActivityInstanceRun(systemLogger, objectLocatorService, activityInstance);
        RunningActivitiesCache.runningActivities.put(activityInstance.getId(), activityInstanceRun);

        threadPoolExecutor.execute(activityInstanceRun);
    }

    public void terminateActivity(ActivityInstance activityInstance)
    {
        if (RunningActivitiesCache.runningActivities.containsKey(activityInstance.getId())) {
            RunningActivitiesCache.runningActivities.get(activityInstance.getId()).terminateSpidering();
        }
    }
}
