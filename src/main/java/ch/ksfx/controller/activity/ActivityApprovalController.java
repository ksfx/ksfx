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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
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
        ActivityInstance activityInstance = activityInstanceDAO.getActivityInstanceForId(activityInstanceId);

        //String approval
        if (activityInstance.getActivity().getActivityApprovalStrategy() != null && activityInstance.getActivity().getActivityApprovalStrategy().getId() == 4l) {
            boolean hasApprovalString = false;

            if (activityInstance.getActivityInstanceParameters() != null) {
                for (ActivityInstanceParameter activityInstanceParameter : activityInstance.getActivityInstanceParameters()) {
                    if (activityInstanceParameter.getDataKey().equals("approvalString")) {
                        hasApprovalString = true;
                    }
                }
            }

            if (!hasApprovalString) {
                ActivityInstanceParameter activityInstanceParameter = new ActivityInstanceParameter("approvalString", "");
                activityInstanceParameter.setActivityInstance(activityInstance);

                activityInstanceDAO.saveOrUpdateActivityInstanceParameter(activityInstanceParameter);
            }
        }

        model.addAttribute("activityInstance", activityInstanceDAO.getActivityInstanceForId(activityInstanceId));

        return "activity/approve_activity_instance";
    }

    @GetMapping("/submitapproval/{activityinstanceid}/{state}")
    public String submitApproval(@PathVariable(value = "activityinstanceid", required = true) Long activityInstanceId, @PathVariable(value = "state", required = true) String state, Model model)
    {
        ActivityInstanceParameter activityInstanceParameter = null;

        if (state.equals("booleanYes")) {
            activityInstanceParameter = new ActivityInstanceParameter("boolean", "yes");
        }

        if (state.equals("booleanNo")) {
            activityInstanceParameter = new ActivityInstanceParameter("boolean", "no");
        }

        if (state.equals("booleanUnknown")) {
            activityInstanceParameter = new ActivityInstanceParameter("boolean", "unknown");
        }

        if (state.equals("tristateYes")) {
            activityInstanceParameter = new ActivityInstanceParameter("tristate", "yes");
        }

        if (state.equals("tristateNeutral")) {
            activityInstanceParameter = new ActivityInstanceParameter("tristate", "neutral");
        }

        if (state.equals("tristateNo")) {
            activityInstanceParameter = new ActivityInstanceParameter("tristate", "no");
        }

        if (state.equals("tristateUnknown")) {
            activityInstanceParameter = new ActivityInstanceParameter("tristate", "unknown");
        }

        saveParameterAndStartJob(activityInstanceParameter, activityInstanceId);

        model.addAttribute("started", true);

        return "activity/approve_activity_instance";
    }

    @PostMapping("/submitapproval/")
    public String submitApprovalSubmit(@Valid @ModelAttribute ActivityInstance activityInstance, BindingResult bindingResult, Model model, HttpServletRequest request)
    {
        for (ActivityInstanceParameter approvalActivityInstanceParameter : activityInstance.getActivityInstanceParameters()) {

            System.out.println("Activity instance parameter " + approvalActivityInstanceParameter.getDataKey() + " / " + approvalActivityInstanceParameter.getDataValue());

            approvalActivityInstanceParameter.setActivityInstance(activityInstance);

            activityInstanceDAO.saveOrUpdateActivityInstanceParameter(approvalActivityInstanceParameter);
        }

        activityInstance.setApproved(true);
        activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);

        activityInstanceRunner.runActivity(activityInstanceDAO.getActivityInstanceForId(activityInstance.getId()));

        model.addAttribute("started", true);

        return "activity/approve_activity_instance";
    }

    @GetMapping("/activityinstanceparameteradd/{activityinstanceid}")
    public String activityInstanceParameterAdd(@PathVariable(value = "activityinstanceid", required = true) Long activityInstanceId)
    {
        ActivityInstance activityInstance = activityInstanceDAO.getActivityInstanceForId(activityInstanceId);

        ActivityInstanceParameter activityInstanceParameter = new ActivityInstanceParameter("","");
        activityInstanceParameter.setActivityInstance(activityInstance);

        activityInstanceDAO.saveOrUpdateActivityInstanceParameter(activityInstanceParameter);

        return "redirect:/activity/approval/activityinstanceapprove/" + activityInstanceId;
    }

    @GetMapping("/activityinstanceparameterdelete/{activityinstanceparameterid}")
    public String activityInstanceParameterDelete(@PathVariable(value = "activityinstanceparameterid", required = true) Long activityInstanceParameterId)
    {
        ActivityInstanceParameter activityInstanceParameter = activityInstanceDAO.getActivityInstanceParameterForId(activityInstanceParameterId);
        activityInstanceDAO.deleteActivityInstanceParameter(activityInstanceParameter);

        return "redirect:/activity/approval/activityinstanceapprove/" + activityInstanceParameter.getActivityInstance().getId();
    }

    private void saveParameterAndStartJob(ActivityInstanceParameter activityInstanceParameter, Long activityInstanceId)
    {
        ActivityInstance activityInstance = activityInstanceDAO.getActivityInstanceForId(activityInstanceId);
        activityInstance.setApproved(true);

        activityInstanceParameter.setActivityInstance(activityInstance);

        activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);
        activityInstanceDAO.saveOrUpdateActivityInstanceParameter(activityInstanceParameter);

        activityInstanceRunner.runActivity(activityInstanceDAO.getActivityInstanceForId(activityInstance.getId()));
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
