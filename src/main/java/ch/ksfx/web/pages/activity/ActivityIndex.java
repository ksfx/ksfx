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
import ch.ksfx.model.activity.ActivityCategory;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.activity.ActivityInstanceParameter;
import ch.ksfx.util.GenericSelectModel;
import ch.ksfx.web.services.activity.ActivityInstanceRunner;
import ch.ksfx.web.services.scheduler.SchedulerService;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.PropertyAccess;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.quartz.SchedulerException;
import org.springframework.security.access.annotation.Secured;

import java.util.ArrayList;
import java.util.List;


@Secured({"ROLE_ADMIN"})
@Import(library = "context:scripts/activity.js")
public class ActivityIndex
{
    @Inject
    private ActivityDAO activityDAO;

    @Inject
    private ActivityInstanceDAO activityInstanceDAO;

    @Inject
    private ActivityInstanceRunner activityInstanceRunner;

    @Inject
    private SchedulerService schedulerService;

    @Property
    private Activity activity;

    @Property
    @Persist
    private List<ActivityInstanceParameter> activityInstanceParameters;

    @Property
    @Persist
    private Long activeActivityId;

    @Property
    private ActivityInstanceParameter activityInstanceParameter;

    @Property
    private Integer parameterIndex;

    @Inject
    private ComponentResources componentResources;

    @Inject
    private PropertyAccess propertyAccess;

    @Property
    private GenericSelectModel<ActivityCategory> allActivityCategories;

    @Property
    @Persist
    private ActivityCategory activityCategory;
    
    @Inject
    private PageRenderLinkSource pageRenderLinkSource;

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {
        allActivityCategories = new GenericSelectModel<ActivityCategory>(activityDAO.getAllActivityCategories(),ActivityCategory.class,"name","id",propertyAccess);
    }

    public List<Activity> getAllActivities()
    {
        return activityDAO.getAllActivitiesForActivityCategory(activityCategory);
    }

    public void onActionFromDelete(Long activityId)
    {
        activityDAO.deleteActivity(activityDAO.getActivityForId(activityId));
    }

    public void onActionFromRunActivity(Long activityId)
    {
        ActivityInstance activityInstance = new ActivityInstance();
        activityInstance.setActivity(activityDAO.getActivityForId(activityId));

        activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);

        activityInstanceRunner.runActivity(activityInstance);
    }

    public void onActionFromSchedule(Long activityId)
    {
        Activity activity = activityDAO.getActivityForId(activityId);
        activity.setCronScheduleEnabled(true);

        activityDAO.saveOrUpdateActivity(activity);

        schedulerService.scheduleActivity(activity);
    }

    public void onActionFromDeleteSchedule(Long activityId) throws SchedulerException
    {
        Activity activity = activityDAO.getActivityForId(activityId);
        activity.setCronScheduleEnabled(false);

        schedulerService.deleteJob("Activity" + activity.getId().toString(),"Activities");

        activityDAO.saveOrUpdateActivity(activity);
    }

    public boolean getScheduled() throws SchedulerException
    {
        return schedulerService.jobExists("Activity" + activity.getId().toString(),"Activities");
    }

    public void onSuccessFromActivityRunForm(Long activityId)
    {
        ActivityInstance activityInstance = new ActivityInstance();
        activityInstance.setActivity(activityDAO.getActivityForId(activityId));

        activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);

        for (ActivityInstanceParameter activityInstanceParameter : activityInstanceParameters) {

            System.out.println("Activity instance parameter " + activityInstanceParameter.getDataKey() + " / " + activityInstanceParameter.getDataValue());

            activityInstanceParameter.setActivityInstance(activityInstance);

            activityInstanceDAO.saveOrUpdateActivityInstanceParameter(activityInstanceParameter);
        }

        activityInstanceRunner.runActivity(activityInstanceDAO.getActivityInstanceForId(activityInstance.getId()));
    }

    public void onActionFromRunActivityWithParameters(Long activityId)
    {
        if (activityInstanceParameters == null || !activityId.equals(activeActivityId)) {
            activityInstanceParameters = new ArrayList<ActivityInstanceParameter>();
            activeActivityId =  activityId;
        }
    }

    public void onActionFromAddActivityInstanceParameter(Long activityId)
    {
        if (activityInstanceParameters == null || !activityId.equals(activeActivityId)) {
            activityInstanceParameters = new ArrayList<ActivityInstanceParameter>();
            activeActivityId =  activityId;
        }

        ActivityInstanceParameter activityInstanceParameter = new ActivityInstanceParameter("","");

        activityInstanceParameters.add(activityInstanceParameter);
    }

    public void onActionFromCloseParameterWindow()
    {
        componentResources.discardPersistentFieldChanges();
        activityInstanceParameters = new ArrayList<ActivityInstanceParameter>();
        activeActivityId = null;
    }

    public void onActionFromCancelParameterWindow()
    {
        onActionFromCloseParameterWindow();
    }

    public boolean getIsActivityActivityActive()
    {
       return activity.getId().equals(activeActivityId);
    }

    public boolean getHasActiveActivity()
    {
        return activeActivityId != null;
    }
    
    public String getIframeInfoLink()
    {
    	return pageRenderLinkSource.createPageRenderLinkWithContext("admin/note/viewnotesforentityplain", getInfoContextParameters()).toURI();	
    }
    
    public Object[] getInfoContextParameters()
    {
        if (activity != null) {
            return new Object[]{null,
                    activity.getId(),
                    null,null,null};
        }

        return null;
    }
}
