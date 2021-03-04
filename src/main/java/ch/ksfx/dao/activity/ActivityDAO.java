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


package ch.ksfx.dao.activity;

import ch.ksfx.model.activity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface ActivityDAO
{
    public void saveOrUpdateActivity(Activity activity);
    public void saveOrUpdateRequiredActivity(RequiredActivity requiredActivity);
    public void saveOrUpdateTriggerActivity(TriggerActivity triggerActivity);
    public void saveOrUpdateActivityCategory(ActivityCategory activityCategory);
    public List<Activity> getAllActivities();
    public List<Activity> getAllActivitiesForActivityCategory(ActivityCategory activityCategory);
    public Page<Activity> getActivitiesForPageableAndActivityCategory(Pageable pageable, ActivityCategory activityCategory);
    public Activity getActivityForId(Long activityId);
    public void deleteActivity(Activity activity);
    public List<Activity> getScheduledActivities();
    public List<ActivityApprovalStrategy> getAllActivityApprovalStrategies();
    public ActivityApprovalStrategy getActivityApprovalStrategyForId(Long activityApprovalStrategyId);
    public ActivityCategory getActivityCategoryForId(Long activityCategoryId);
    public List<ActivityCategory> getAllActivityCategories();
    public void deleteActivityCategory(ActivityCategory activityCategory);
}
