package ch.ksfx.controller.activity;

import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.ActivityInstance;
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
import javax.validation.Valid;
import java.io.*;
import java.lang.reflect.Constructor;
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
    public String activityIndex(Pageable pageable, Model model)
    {
        Page<Activity> activitiesPage = activityDAO.getActivitiesForPageableAndActivityCategory(pageable, null);

        model.addAttribute("activitiesPage", activitiesPage);

        return "activity/activity";
    }

    @GetMapping("/activityinstances/{activityid}")
    public String activityInstancesViewer(Pageable pageable, @PathVariable(value = "activityid", required = true) Long activityId, Model model)
    {
        Page<ActivityInstance> activityInstancePage = activityInstanceDAO.getActivityInstancesForPageableAndActivity(pageable, activityDAO.getActivityForId(activityId));

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
}
