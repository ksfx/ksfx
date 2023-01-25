package ch.ksfx.controller.activity;

import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.ActivityCategory;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.activity.ActivityInstanceParameter;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.services.activity.ActivityInstanceRunner;
import ch.ksfx.services.scheduler.SchedulerService;
import ch.ksfx.util.StacktraceUtil;
import groovy.lang.GroovyClassLoader;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/activity")
public class ActivityController
{
    private ActivityDAO activityDAO;
    private ActivityInstanceDAO activityInstanceDAO;
    private ActivityInstanceRunner activityInstanceRunner;
    private ServiceProvider serviceProvider;
    private SchedulerService schedulerService;

    public ActivityController(ActivityDAO activityDAO,
                              ActivityInstanceDAO activityInstanceDAO,
                              ActivityInstanceRunner activityInstanceRunner,
                              ServiceProvider serviceProvider,
                              SchedulerService schedulerService)
    {
        this.activityDAO = activityDAO;
        this.activityInstanceDAO = activityInstanceDAO;
        this.activityInstanceRunner = activityInstanceRunner;
        this.serviceProvider = serviceProvider;
        this.schedulerService = schedulerService;
    }

    @GetMapping("/")
    public String activityIndex(Pageable pageable, Model model, HttpServletRequest request)
    {
        String selectedActivityCategory = null;
        ActivityCategory activityCategory = null;

        if (request.getSession().getAttribute("selectedActivityCategory") != null) {
            selectedActivityCategory = (String) request.getSession().getAttribute("selectedActivityCategory");
        }

        if (selectedActivityCategory != null && selectedActivityCategory.equals("0")) {
            selectedActivityCategory = null;
        }

        if (selectedActivityCategory != null) {
            activityCategory = activityDAO.getActivityCategoryForId(Long.parseLong(selectedActivityCategory));
        }

        Page<Activity> activitiesPage = activityDAO.getActivitiesForPageableAndActivityCategory(pageable, activityCategory);

        model.addAttribute("activitiesPage", activitiesPage);
        model.addAttribute("activityCategories", activityDAO.getAllActivityCategories());
        model.addAttribute("selectedActivityCategory", selectedActivityCategory);
        model.addAttribute("activityInstanceRunner",activityInstanceRunner);

        return "activity/activity";
    }

    @PostMapping("/selectactivitycategory")
    public String selectTag(@RequestParam(name = "selectedActivityCategory") String selectedActivityCategory, HttpServletRequest request)
    {
        request.getSession().setAttribute("selectedActivityCategory", selectedActivityCategory);

        return "redirect:/activity/";
    }

    @GetMapping("/activityinstances/{activityid}")
    public String activityInstancesViewer(Pageable pageable, @PathVariable(value = "activityid", required = true) Long activityId, Model model)
    {
        Page<ActivityInstance> activityInstancePage = activityInstanceDAO.getActivityInstancesForPageableAndActivity(pageable, activityDAO.getActivityForId(activityId), false);

        model.addAttribute("activityInstanceRunner", activityInstanceRunner);
        model.addAttribute("activity", activityDAO.getActivityForId(activityId));
        model.addAttribute("activityInstancesPage", activityInstancePage);

        return "activity/activity_instances_viewer";
    }

    @GetMapping({"/activityedit", "/activityedit/{id}"})
    public String activityEdit(@PathVariable(value = "id", required = false) Long activityId, Model model)
    {
        Activity activity = new Activity();

        if (activityId != null) {
            activity = activityDAO.getActivityForId(activityId);
        } else {
            InputStream inputStream = null;
            String activityDemo = null;

            try {
                inputStream = getClass().getClassLoader().getResourceAsStream("groovy/DemoActivity.groovy");

                activityDemo = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            activity.setGroovyCode(activityDemo);
        }

        model.addAttribute("allActivityApprovalStrategies", activityDAO.getAllActivityApprovalStrategies());
        model.addAttribute("allActivityCategories", activityDAO.getAllActivityCategories());
        model.addAttribute("activity", activity);

        return "activity/activity_edit";
    }

    @PostMapping({"/activityedit", "/activityedit/{id}"})
    public String activitySubmit(@PathVariable(value = "id", required = false) Long activityId, @Valid @ModelAttribute Activity activity, BindingResult bindingResult, Model model)
    {
        activityValidate(activity, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("allActivityApprovalStrategies", activityDAO.getAllActivityApprovalStrategies());
            model.addAttribute("allActivityCategories", activityDAO.getAllActivityCategories());

            return "activity/activity_edit";
        }

        if (activity.getActivityCategory().getId() == 0) {
            activity.setActivityCategory(null);
        }

        activityDAO.saveOrUpdateActivity(activity);

        return "redirect:/activity/activityedit/" + activity.getId();
    }

    public void activityValidate(Activity activity, BindingResult bindingResult)
    {
        try {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(activity.getGroovyCode());

            Constructor cons = clazz.getDeclaredConstructor(ServiceProvider.class);
            cons.newInstance(serviceProvider);
        } catch (Exception e) {
            bindingResult.rejectValue("groovyCode", "activity.groovyCode", e.getMessage() + StacktraceUtil.getStackTrace(e));
        }

        if (activity.getCronSchedule() != null && !activity.getCronSchedule().isEmpty()) {
            try {
                CronExpression cronExpression = new CronExpression(activity.getCronSchedule());
            } catch (Exception e) {
                bindingResult.rejectValue("cronSchedule", "activity.cronSchedule","Cron Schedule not valid");
            }
        }
    }

    @GetMapping({"/activityrun/{id}"})
    public String activityRun(@PathVariable(value = "id", required = true) Long activityId)
    {
        ActivityInstance activityInstance = new ActivityInstance();
        activityInstance.setActivity(activityDAO.getActivityForId(activityId));

        activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);

        activityInstanceRunner.runActivity(activityInstance);

        return "redirect:/activity/";
    }

    @GetMapping({"/activityschedule/{id}"})
    public String activitySchedule(@PathVariable(value = "id", required = true) Long activityId)
    {
        Activity activity = activityDAO.getActivityForId(activityId);
        activity.setCronScheduleEnabled(true);

        activityDAO.saveOrUpdateActivity(activity);

        schedulerService.scheduleActivity(activity);

        return "redirect:/activity/";
    }

    @GetMapping({"/activitydeleteschedule/{id}"})
    public String activityDeleteSchedule(@PathVariable(value = "id", required = true) Long activityId) throws SchedulerException
    {
        Activity activity = activityDAO.getActivityForId(activityId);
        activity.setCronScheduleEnabled(false);

        schedulerService.deleteJob("Activity" + activity.getId().toString(),"Activities");

        activityDAO.saveOrUpdateActivity(activity);

        return "redirect:/activity/";
    }

    @GetMapping({"/activitydelete/{id}"})
    public String activityDelete(@PathVariable(value = "id", required = true) Long activityId)
    {
        Activity activity = activityDAO.getActivityForId(activityId);

        activityDAO.deleteActivity(activity);

        return "redirect:/activity/";
    }

    @GetMapping({"/activityrunwithparameters/{activityid}"})
    public String activityRunWithParameters(@PathVariable(value = "activityid", required = true) Long activityId, Model model, HttpServletRequest request)
    {
        if (request.getSession().getAttribute("activityInstance") == null || ((ActivityInstance) request.getSession().getAttribute("activityInstance")).getActivity().getId() != activityId) {
            ActivityInstance activityInstance = new ActivityInstance();
            activityInstance.setActivity(activityDAO.getActivityForId(activityId));

            request.getSession().setAttribute("activityInstance", activityInstance);
        }

//        model.addAttribute("activityId",activityId);
        model.addAttribute("activityInstance", request.getSession().getAttribute("activityInstance"));

        return "activity/run_with_parameters";
    }

    @PostMapping({"/activityrunwithparameters/","/activityrunwithparameters/{activityid}"})
    public String activityRunWithParametersSubmit(@PathVariable(value = "activityid", required = false) Long activityId, @Valid @ModelAttribute ActivityInstance activityInstance, BindingResult bindingResult, Model model, HttpServletRequest request)
    {
        activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);

        System.out.println(activityInstance.getActivity());

        for (ActivityInstanceParameter activityInstanceParameter : activityInstance.getActivityInstanceParameters()) {

            System.out.println("Activity instance parameter " + activityInstanceParameter.getDataKey() + " / " + activityInstanceParameter.getDataValue());

            activityInstanceParameter.setActivityInstance(activityInstance);

            activityInstanceDAO.saveOrUpdateActivityInstanceParameter(activityInstanceParameter);
        }

        activityInstanceRunner.runActivity(activityInstanceDAO.getActivityInstanceForId(activityInstance.getId()));

        request.getSession().removeAttribute("activityInstance");

        model.addAttribute("started", true);

        return "activity/run_with_parameters";
    }

    @GetMapping({"/activityinstanceparameteradd/{activityid}"})
    public String activityRunWithParametersAddParameter(@PathVariable(value = "activityid", required = true) Long activityId, HttpServletRequest request)
    {
        if (request.getSession().getAttribute("activityInstance") == null) {
            request.getSession().setAttribute("activityInstance", new ActivityInstance());
        }

        ActivityInstance activityInstance = (ActivityInstance) request.getSession().getAttribute("activityInstance");

        if (activityInstance.getActivityInstanceParameters() == null) {
            activityInstance.setActivityInstanceParameters(new ArrayList<ActivityInstanceParameter>());
        }

        activityInstance.getActivityInstanceParameters().add(new ActivityInstanceParameter());

        request.getSession().setAttribute("activityInstance", activityInstance);

        return "redirect:/activity/activityrunwithparameters/" + activityId;
    }

    @GetMapping({"/activityinstancedelete/{activityInstanceId}"})
    public String activityInstanceDelete(@PathVariable(value = "activityInstanceId", required = true) Long activityInstanceId)
    {
        ActivityInstance activityInstance = activityInstanceDAO.getActivityInstanceForId(activityInstanceId);

        activityInstanceDAO.deleteActivityInstance(activityInstance);

        return "redirect:/activity/activityinstances/" + activityInstance.getActivity().getId();
    }

    @GetMapping({"/activityinstanceterminate/{activityInstanceId}"})
    public String activityInstanceTerminate(@PathVariable(value = "activityInstanceId", required = true) Long activityInstanceId)
    {
        ActivityInstance activityInstance = activityInstanceDAO.getActivityInstanceForId(activityInstanceId);

        activityInstanceRunner.terminateActivity(activityInstance);

        return "redirect:/activity/activityinstances/" + activityInstance.getActivity().getId();
    }
}
