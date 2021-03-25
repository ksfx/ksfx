package ch.ksfx.controller.activity;

import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.activity.ActivityInstanceParameter;
import ch.ksfx.model.activity.ActivityInstancePersistentData;
import ch.ksfx.services.activity.ActivityInstanceRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/activity/approval")
public class ActivityApprovalController
{
    private ActivityInstanceDAO activityInstanceDAO;
    private ActivityDAO activityDAO;
    private ActivityInstanceRunner activityInstanceRunner;

    public ActivityApprovalController(ActivityInstanceDAO activityInstanceDAO, ActivityDAO activityDAO, ActivityInstanceRunner activityInstanceRunner)
    {
        this.activityInstanceDAO = activityInstanceDAO;
        this.activityDAO = activityDAO;
        this.activityInstanceRunner = activityInstanceRunner;
    }

    @GetMapping("/")
    public String activityApprovalIndex(Pageable pageable, Model model)
    {
        model.addAttribute("unapprovedActivityInstancesPage", activityInstanceDAO.getActivityInstancesForPageableAndActivity(pageable,null, true));

        return "activity/unapproved_activity_instances";
    }

    @GetMapping("/activityinstanceapprove/{activityinstanceid}")
    public String activityInstanceApprove(@PathVariable(value = "activityinstanceid", required = true) Long activityInstanceId, Model model)
    {
        model.addAttribute("activityInstance", activityInstanceDAO.getActivityInstanceForId(activityInstanceId));

        return "activity/approve_activity_instance";
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

    /*
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
    */
}
