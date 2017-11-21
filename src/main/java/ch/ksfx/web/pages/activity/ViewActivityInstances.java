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

package ch.ksfx.web.pages.activity;

import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.activity.ActivityInstancePersistentData;
import ch.ksfx.web.services.activity.ActivityInstanceRunner;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.springframework.security.access.annotation.Secured;

import java.util.List;


@Secured({"ROLE_ADMIN"})
public class ViewActivityInstances
{
    @Inject
    private ActivityInstanceDAO activityInstanceDAO;

    @Inject
    private ActivityDAO activityDAO;
    
    @Inject
    private PageRenderLinkSource pageRenderLinkSource;

    @Inject
    private ActivityInstanceRunner activityInstanceRunner;

    @Property
    private ActivityInstance activityInstance;

    @Property
    private ActivityInstancePersistentData activityInstancePersistentData;

    @Property
    private Activity activity;

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long activityId)
    {
        activity = activityDAO.getActivityForId(activityId);
    }

    public Long onPassivate()
    {
        if (activity != null) {
            return activity.getId();
        }

        return null;
    }

    public boolean getActivityInstanceRunning()
    {
        return activityInstanceRunner.isActivityInstanceRunning(activityInstance);
    }

    public void onActionFromTerminateActivityInstance(Long activityInstanceId)
    {
        activityInstanceRunner.terminateActivity(activityInstanceDAO.getActivityInstanceForId(activityInstanceId));
    }

    public GridDataSource getAllActivityInstancesForActivity()
    {
        return activityInstanceDAO.getActivityInstanceGridDataSourceForActivity(activity);
    }

    public void onActionFromDelete(Long activityInstanceId)
    {
        activityInstanceDAO.deleteActivityInstance(activityInstanceDAO.getActivityInstanceForId(activityInstanceId));
    }

    public void onActionFromClearActivityInstances()
    {
        List<ActivityInstance> activityInstances = activityInstanceDAO.getActivityInstancesForActivity(activity);

        for (ActivityInstance activityInstance : activityInstances) {
            activityInstanceDAO.deleteActivityInstance(activityInstance);
        }
    }
    
    public String getIframeConsoleLink()
    {
    	return pageRenderLinkSource.createPageRenderLinkWithContext("ViewConsoleForEntityPlain", getConsoleContextParameters()).toURI();	
    }
    
    public Object[] getConsoleContextParameters()
    {
        if (activityInstance != null) {
            return new Object[]{activityInstance.getId(),
                    "activityInstance"};
        }

        return null;
    }
}
