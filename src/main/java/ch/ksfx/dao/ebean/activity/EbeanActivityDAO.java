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

package ch.ksfx.dao.ebean.activity;

import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.model.activity.*;
import ch.ksfx.model.note.NoteActivity;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanActivityDAO implements ActivityDAO
{
    @Override
    public void saveOrUpdateActivity(Activity activity)
    {
        if (activity.getId() != null) {
            Ebean.update(activity);
        } else {
            Ebean.save(activity);
        }
    }

    @Override
    public void saveOrUpdateRequiredActivity(RequiredActivity requiredActivity)
    {
        if (requiredActivity.getId() != null) {
            Ebean.update(requiredActivity);
        } else {
            Ebean.save(requiredActivity);
        }
    }

    @Override
    public void saveOrUpdateTriggerActivity(TriggerActivity triggersActivity)
    {
        if (triggersActivity.getId() != null) {
            Ebean.update(triggersActivity);
        } else {
            Ebean.save(triggersActivity);
        }
    }

    @Override
    public List<Activity> getAllActivities()
    {
        return Ebean.find(Activity.class).findList();
    }

    @Override
    public List<Activity> getAllActivitiesForActivityCategory(ActivityCategory activityCategory)
    {
        if (activityCategory == null) {
            return Ebean.find(Activity.class).findList();
        } else {
            return Ebean.find(Activity.class).where().eq("activityCategory", activityCategory).findList();
        }
    }

    @Override
    public Activity getActivityForId(Long activityId)
    {
        return Ebean.find(Activity.class, activityId);
    }

    @Override
    public void deleteActivity(Activity activity)
    {
        List<NoteActivity> noteActivities = Ebean.find(NoteActivity.class).where().eq("activity",activity).findList();
        
        for (NoteActivity noteActivity : noteActivities) {
            Ebean.delete(noteActivity);   
        }

        Ebean.delete(activity);
    }

    @Override
    public List<Activity> getScheduledActivities()
    {
        return Ebean.find(Activity.class).where().eq("cronScheduleEnabled",true).findList();
    }

    @Override
    public ActivityApprovalStrategy getActivityApprovalStrategyForId(Long activityApprovalStrategyId)
    {
        return Ebean.find(ActivityApprovalStrategy.class, activityApprovalStrategyId);
    }

    @Override
    public void saveOrUpdateActivityCategory(ActivityCategory activityCategory)
    {
        if (activityCategory.getId() != null) {
            Ebean.update(activityCategory);
        } else {
            Ebean.save(activityCategory);
        }
    }

    @Override
    public ActivityCategory getActivityCategoryForId(Long activityCategoryId)
    {
        return Ebean.find(ActivityCategory.class, activityCategoryId);
    }

    @Override
    public List<ActivityCategory> getAllActivityCategories()
    {
        return Ebean.find(ActivityCategory.class).findList();
    }

    @Override
    public void deleteActivityCategory(ActivityCategory activityCategory)
    {
        Ebean.delete(activityCategory);
    }

    @Override
    public List<ActivityApprovalStrategy> getAllActivityApprovalStrategies()
    {
        return Ebean.find(ActivityApprovalStrategy.class).findList();
    }
}
