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

import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.activity.ActivityInstanceParameter;
import ch.ksfx.model.activity.ActivityInstancePersistentData;
import org.apache.tapestry5.grid.GridDataSource;

import java.util.List;


public interface ActivityInstanceDAO
{
    public void saveOrUpdateActivityInstance(ActivityInstance activityInstance);
    public void saveOrUpdateActivityInstancePersistentData(ActivityInstancePersistentData activityInstancePersistentData);
    public void saveOrUpdateActivityInstanceParameter(ActivityInstanceParameter activityInstanceParameter);
    public List<ActivityInstance> getAllActivityInstances();
    public List<ActivityInstance> getActivityInstancesWithApprovalRequired();
    public List<ActivityInstance> getActivityInstancesForActivity(Activity activity);
    public ActivityInstance getActivityInstanceForId(Long activityInstanceId);
    public void deleteActivityInstance(ActivityInstance activityInstance);
    public GridDataSource getActivityInstanceGridDataSourceForActivity(Activity activity);
    public void appendConsole(Long activityInstanceId, String dataToAppend);
}
