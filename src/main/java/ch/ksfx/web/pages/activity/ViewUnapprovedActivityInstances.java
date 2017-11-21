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
import ch.ksfx.model.activity.ActivityInstanceParameter;
import ch.ksfx.model.activity.ActivityInstancePersistentData;
import ch.ksfx.web.services.activity.ActivityInstanceRunner;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;

import java.util.ArrayList;
import java.util.List;


@Secured({"ROLE_ADMIN"})
@Import(library = "context:scripts/viewunapprovedactivityinstances.js")
public class ViewUnapprovedActivityInstances
{
    @Inject
    private ActivityInstanceDAO activityInstanceDAO;

    @Inject
    private ActivityDAO activityDAO;

    @Inject
    private ComponentResources componentResources;

    @Inject
    private ActivityInstanceRunner activityInstanceRunner;

    @Property
    private ActivityInstance activityInstance;

    @Property
    private ActivityInstancePersistentData activityInstancePersistentData;

    @Property
    private Activity activity;

    @Property
    private Integer parameterIndex;
    
    @Property
    @Persist
    private Long activeActivityInstanceId;
    
    @Property
    @Persist
    private List<ActivityInstanceParameter> approvalActivityInstanceParameters;

    @Property
    private ActivityInstanceParameter activityInstanceParameter;
    
    @Property
    private String approvalString;

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {

    }

    public List<ActivityInstance> getAllActivityInstancesForActivity()
    {
        return activityInstanceDAO.getActivityInstancesWithApprovalRequired();
    }

    public void onActionFromDelete(Long activityInstanceId)
    {
        activityInstanceDAO.deleteActivityInstance(activityInstanceDAO.getActivityInstanceForId(activityInstanceId));
    }

    public void onActionFromClearActivityInstances()
    {
        List<ActivityInstance> activityInstances = activityInstanceDAO.getActivityInstancesWithApprovalRequired();

        for (ActivityInstance activityInstance : activityInstances) {
            activityInstanceDAO.deleteActivityInstance(activityInstance);
        }
    }

    public boolean getBooleanApproval()
    {
        if (activeActivityInstanceId == null) {
            return false;
        }

        return activityInstanceDAO.getActivityInstanceForId(activeActivityInstanceId).getActivity().getActivityApprovalStrategy().getId() == 2l;
    }

    public boolean getTristateApproval()
    {
        if (activeActivityInstanceId == null) {
            return false;
        }

        return activityInstanceDAO.getActivityInstanceForId(activeActivityInstanceId).getActivity().getActivityApprovalStrategy().getId() == 3l;
    }

    public boolean getStringApproval()
    {
        if (activeActivityInstanceId == null) {
            return false;
        }

        return activityInstanceDAO.getActivityInstanceForId(activeActivityInstanceId).getActivity().getActivityApprovalStrategy().getId() == 4l;
    }

    public boolean getMapApproval()
    {
        if (activeActivityInstanceId == null) {
            return false;
        }

        return activityInstanceDAO.getActivityInstanceForId(activeActivityInstanceId).getActivity().getActivityApprovalStrategy().getId() == 5l;
    }
    
    public boolean getIsActivityInstanceActiveActivityInstance()
    {
       return activityInstance.getId().equals(activeActivityInstanceId);
    }
    
    public void onActionFromApproveActivityInstance(Long activityInstanceId)
    {
        if (approvalActivityInstanceParameters == null || !activityInstanceId.equals(activeActivityInstanceId)) {
            approvalActivityInstanceParameters = new ArrayList<ActivityInstanceParameter>();
            activeActivityInstanceId =  activityInstanceId;
        }
    }

    public void onActionFromAddActivityInstanceParameter(Long activityInstanceId)
    {
        if (approvalActivityInstanceParameters == null || !activityInstanceId.equals(activeActivityInstanceId)) {
            approvalActivityInstanceParameters = new ArrayList<ActivityInstanceParameter>();
            activeActivityInstanceId =  activityInstanceId;
        }

        ActivityInstanceParameter activityInstanceParameter = new ActivityInstanceParameter("","");

        approvalActivityInstanceParameters.add(activityInstanceParameter);
    }

    public void onActionFromCloseParameterWindow()
    {
        componentResources.discardPersistentFieldChanges();
        approvalActivityInstanceParameters = new ArrayList<ActivityInstanceParameter>();
        activeActivityInstanceId = null;
    }

    public void onActionFromCancelParameterWindow()
    {
        onActionFromCloseParameterWindow();
    }
    
    public void onActionFromBooleanYes(Long activityInstanceId)
    {
		ActivityInstanceParameter activityInstanceParameter = new ActivityInstanceParameter("boolean", "yes");	
		saveParameterAndStartJob(activityInstanceParameter, activityInstanceId);
    }
    
    public void onActionFromBooleanNo(Long activityInstanceId)
    {
    	ActivityInstanceParameter activityInstanceParameter = new ActivityInstanceParameter("boolean", "no");
    	saveParameterAndStartJob(activityInstanceParameter, activityInstanceId);
    }
    
    public void onActionFromBooleanUnknown(Long activityInstanceId)
    {
    	ActivityInstanceParameter activityInstanceParameter = new ActivityInstanceParameter("boolean", "unknown");
    	saveParameterAndStartJob(activityInstanceParameter, activityInstanceId);
    }
    
    public void onActionFromTristateYes(Long activityInstanceId)
    {
    	ActivityInstanceParameter activityInstanceParameter = new ActivityInstanceParameter("tristate", "yes");
    	saveParameterAndStartJob(activityInstanceParameter, activityInstanceId);
    }
    
    public void onActionFromTristateNeutral(Long activityInstanceId)
    {
    	ActivityInstanceParameter activityInstanceParameter = new ActivityInstanceParameter("tristate", "neutral");
    	saveParameterAndStartJob(activityInstanceParameter, activityInstanceId);
    }
    
    public void onActionFromTristateNo(Long activityInstanceId)
    {
    	ActivityInstanceParameter activityInstanceParameter = new ActivityInstanceParameter("tristate", "no");
    	saveParameterAndStartJob(activityInstanceParameter, activityInstanceId);
    }
    
    public void onActionFromTristateUnknown(Long activityInstanceId)
    {
    	ActivityInstanceParameter activityInstanceParameter = new ActivityInstanceParameter("tristate", "unknown");
    	saveParameterAndStartJob(activityInstanceParameter, activityInstanceId);
    }
    
    private void saveParameterAndStartJob(ActivityInstanceParameter activityInstanceParameter, Long activityInstanceId)
    {
    	ActivityInstance activityInstance = activityInstanceDAO.getActivityInstanceForId(activityInstanceId);
        activityInstance.setApproved(true);

    	activityInstanceParameter.setActivityInstance(activityInstance);

        activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);
    	activityInstanceDAO.saveOrUpdateActivityInstanceParameter(activityInstanceParameter);
    	
    	activityInstanceRunner.runActivity(activityInstanceDAO.getActivityInstanceForId(activityInstance.getId()));

        componentResources.discardPersistentFieldChanges();
        approvalActivityInstanceParameters = new ArrayList<ActivityInstanceParameter>();
        activeActivityInstanceId = null;
    }
    
    public void onSuccessFromApproveActivityInstanceForm(Long activityInstanceId)
    {
        ActivityInstance activityInstance = activityInstanceDAO.getActivityInstanceForId(activityInstanceId);
        
        if (approvalString != null) {
        	ActivityInstanceParameter approvalStringInstanceParameter = new ActivityInstanceParameter("approvalString", approvalString);
            approvalStringInstanceParameter.setActivityInstance(activityInstance);
        	
        	activityInstanceDAO.saveOrUpdateActivityInstanceParameter(approvalStringInstanceParameter);
        }

        for (ActivityInstanceParameter approvalActivityInstanceParameter : approvalActivityInstanceParameters) {

            System.out.println("Activity instance parameter " + approvalActivityInstanceParameter.getDataKey() + " / " + approvalActivityInstanceParameter.getDataValue());

            approvalActivityInstanceParameter.setActivityInstance(activityInstance);

            activityInstanceDAO.saveOrUpdateActivityInstanceParameter(approvalActivityInstanceParameter);
        }

        activityInstance.setApproved(true);
        activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);

        activityInstanceRunner.runActivity(activityInstanceDAO.getActivityInstanceForId(activityInstance.getId()));
    }
    
    public boolean getHasActiveActivityInstance()
    {
        return activeActivityInstanceId != null;
    }
    
    public ActivityInstance getActiveActivityInstance()
    {
        if (activeActivityInstanceId == null) {
            return null;
        }

        return activityInstanceDAO.getActivityInstanceForId(activeActivityInstanceId);
    }
}
