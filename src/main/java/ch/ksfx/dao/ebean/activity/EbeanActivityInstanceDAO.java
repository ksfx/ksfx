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


import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.activity.ActivityInstanceParameter;
import ch.ksfx.model.activity.ActivityInstancePersistentData;
import ch.ksfx.util.EbeanGridDataSource;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import org.apache.tapestry5.grid.GridDataSource;

import java.util.List;


public class EbeanActivityInstanceDAO implements ActivityInstanceDAO
{
    @Override
    public void saveOrUpdateActivityInstance(ActivityInstance activityInstance)
    {
        if (activityInstance.getId() != null) {
            Ebean.update(activityInstance);
        } else {
            Ebean.save(activityInstance);
        }
    }

    @Override
    public void saveOrUpdateActivityInstancePersistentData(ActivityInstancePersistentData activityInstancePersistentData)
    {
        if (activityInstancePersistentData.getId() != null) {
            Ebean.update(activityInstancePersistentData);
        } else {
            Ebean.save(activityInstancePersistentData);
        }
    }

    @Override
    public void saveOrUpdateActivityInstanceParameter(ActivityInstanceParameter activityInstanceParameter)
    {
        if (activityInstanceParameter.getId() != null) {
            Ebean.update(activityInstanceParameter);
        } else {
            Ebean.save(activityInstanceParameter);
        }
    }

    @Override
    public List<ActivityInstance> getAllActivityInstances()
    {
        return Ebean.find(ActivityInstance.class).findList();
    }

    @Override
    public List<ActivityInstance> getActivityInstancesForActivity(Activity activity)
    {
        return Ebean.find(ActivityInstance.class).where().eq("activity.id", activity.getId()).findList();
    }

    public List<ActivityInstance> getActivityInstancesWithApprovalRequired()
    {
        return Ebean.find(ActivityInstance.class).where().eq("approved", false).order().asc("id").findList();
    }

    @Override
    public ActivityInstance getActivityInstanceForId(Long activityInstanceId)
    {
        return Ebean.find(ActivityInstance.class, activityInstanceId);
    }

    @Override
    public void deleteActivityInstance(ActivityInstance activityInstance)
    {
        for (ActivityInstanceParameter activityInstanceParameter : activityInstance.getActivityInstanceParameters()) {
            Ebean.delete(activityInstanceParameter);
        }

        for (ActivityInstancePersistentData activityInstancePersistentData : activityInstance.getActivityInstancePersistentDatas()) {
            Ebean.delete(activityInstancePersistentData);
        }

        Ebean.delete(activityInstance);
    }

    @Override
    public GridDataSource getActivityInstanceGridDataSourceForActivity(Activity activity)
    {
        ExpressionList expressionList = Ebean.find(ActivityInstance.class).where().eq("activity.id", activity.getId());

        return new EbeanGridDataSource(expressionList, ActivityInstance.class);
    }
    
    @Override
    public void appendConsole(Long activityInstanceId, String dataToAppend)
    {
    	ActivityInstance activityInstance = getActivityInstanceForId(activityInstanceId);
    	activityInstance.setConsole(((activityInstance.getConsole() == null)?"":activityInstance.getConsole()) + dataToAppend);
    	saveOrUpdateActivityInstance(activityInstance);
		//SqlUpdate update = Ebean.createSqlUpdate("UPDATE activity_instance SET console = CONCAT(console, :data) WHERE id = :id");
        //update.setParameter("data", dataToAppend);
        //update.setParameter("id", activityInstanceId);
       	//update.execute();	
    }
}
