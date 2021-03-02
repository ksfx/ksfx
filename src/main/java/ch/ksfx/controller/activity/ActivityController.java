package ch.ksfx.controller.activity;

import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.util.StacktraceUtil;
import groovy.lang.GroovyClassLoader;
import org.quartz.CronExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.lang.reflect.Constructor;

@Controller
@RequestMapping("/activity")
public class ActivityController
{
    private ActivityDAO activityDAO;
    private ServiceProvider serviceProvider;

    public ActivityController(ActivityDAO activityDAO, ServiceProvider serviceProvider)
    {
        this.activityDAO = activityDAO;
        this.serviceProvider = serviceProvider;
    }

    @GetMapping("/")
    public String publishingIndex(Pageable pageable, Model model)
    {
        Page<Activity> activitiesPage = activityDAO.getActivitiesForPageableAndActivityCategory(pageable, null);

        model.addAttribute("activitiesPage", activitiesPage);

        return "activity/activity";
    }

    @GetMapping({"/activityedit", "/activityedit/{id}"})
    public String activityEdit(@PathVariable(value = "id", required = false) Long activityId, Model model)
    {
        Activity activity = new Activity();

        if (activity != null) {
            activity = activityDAO.getActivityForId(activityId);
        }

        model.addAttribute("allActivityApprovalStrategies", activityDAO.getAllActivityApprovalStrategies());
        model.addAttribute("allActivityCategories", activityDAO.getAllActivityCategories());
        model.addAttribute("activity", activity);

        return "activity/activity_edit";
    }

    @PostMapping({"/activityedit", "/activityedit/{id}"})
    public String activitySubmit(@PathVariable(value = "id", required = false) Long activityId, @Valid @ModelAttribute Activity activity, BindingResult bindingResult, Model model)
    {
        validateActivity(activity, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("allActivityApprovalStrategies", activityDAO.getAllActivityApprovalStrategies());
            model.addAttribute("allActivityCategories", activityDAO.getAllActivityCategories());

            return "activity/activity_edit";
        }

        return "redirect:/activity/activityedit/" + activity.getId();
    }

    public void validateActivity(Activity activity, BindingResult bindingResult)
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
}
