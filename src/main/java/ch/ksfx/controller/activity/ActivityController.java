package ch.ksfx.controller.activity;

import org.springframework.beans.factory.annotation.Autowired;
import ch.ksfx.services.project.UserScope;
import ch.ksfx.services.project.ProjectContextService;
import ch.ksfx.model.Project;
import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.ActivityCategory;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.activity.ActivityInstanceParameter;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.services.activity.ActivityInstanceRunner;
import ch.ksfx.services.git.ActivityGitRepositoryService;
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
import ch.ksfx.web.ListSearch;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/activity")
public class ActivityController
{
    @Autowired
    private UserScope userScope;

    private ActivityDAO activityDAO;
    private final ProjectContextService projectContextService;
    private final ProjectDAO projectDAO;
    private ActivityInstanceDAO activityInstanceDAO;
    private ActivityInstanceRunner activityInstanceRunner;
    private ServiceProvider serviceProvider;
    private SchedulerService schedulerService;
    private ActivityGitRepositoryService activityGitRepositoryService;

    public ActivityController(ActivityDAO activityDAO,
                              ActivityInstanceDAO activityInstanceDAO,
                              ActivityInstanceRunner activityInstanceRunner,
                              ServiceProvider serviceProvider,
                              SchedulerService schedulerService,
                              ActivityGitRepositoryService activityGitRepositoryService, ProjectDAO projectDAO, ProjectContextService projectContextService)
    {
        this.activityDAO = activityDAO;
        this.projectContextService = projectContextService;
        this.projectDAO = projectDAO;
        this.activityInstanceDAO = activityInstanceDAO;
        this.activityInstanceRunner = activityInstanceRunner;
        this.serviceProvider = serviceProvider;
        this.schedulerService = schedulerService;
        this.activityGitRepositoryService = activityGitRepositoryService;
    }

    @ModelAttribute("gitSyncActive")
    public boolean gitSyncActive()
    {
        // "Is the CURRENT project Git-backed?" - drives the "changes stay in the database" hint on the pages of this area.
        return activityGitRepositoryService.isActive(projectContextService.current());
    }

    /**
     * The current project's activities - searchable by name (optionally also in the Groovy code),
     * the category is one part of that search (ListSearch: kept per project in the session, so the
     * pagination and sort links keep it).
     */
    @GetMapping("/")
    public String activityIndex(Pageable pageable, Model model, HttpServletRequest request)
    {
        Long projectId = projectContextService.current().getId();
        ListSearch search = ListSearch.resolve(request, "activities", projectId);
        Page<Activity> activitiesPage = activityDAO.searchActivities(pageable, projectId, search.getCategoryId(), search.getQuery(), search.isInCode());

        model.addAttribute("activitiesPage", activitiesPage);
        model.addAttribute("activityCategories", activityDAO.getAllActivityCategoriesForProject(projectId));
        model.addAttribute("search", search);
        model.addAttribute("activityInstanceRunner",activityInstanceRunner);

        return "activity/activity";
    }

    @GetMapping("/activityinstances/{activityid}")
    public String activityInstancesViewer(Pageable pageable, @PathVariable(value = "activityid", required = true) Long activityId, Model model)
    {
        projectContextService.follow(activityDAO.getActivityForId(activityId).getProject()); // deep links win
        Page<ActivityInstance> activityInstancePage = activityInstanceDAO.getActivityInstancesForPageableAndActivity(pageable, activityDAO.getActivityForId(activityId), false);

        model.addAttribute("activityInstanceRunner", activityInstanceRunner);
        model.addAttribute("activity", activityDAO.getActivityForId(activityId));
        model.addAttribute("activityInstancesPage", activityInstancePage);

        return "activity/activity_instances_viewer";
    }

    @GetMapping({"/activityedit", "/activityedit/{id}"})
    public String activityEdit(@PathVariable(value = "id", required = false) Long activityId, Model model)
    {
        if (activityId == null) {
            userScope.requireWrite(projectContextService.current()); // a new activity is a write into the current project
        }

        Activity activity = new Activity();

        if (activityId != null) {
            activity = activityDAO.getActivityForId(activityId);
            projectContextService.follow(activity.getProject()); // deep links win

            if (ActivityGitRepositoryService.hasGitPath(activity.getGitPath()) && activityGitRepositoryService.isActive(activity.getProject())) {
                try {
                    activityGitRepositoryService.sync(activity.getProject());
                    activity.setGroovyCode(activityGitRepositoryService.readActivitySource(activity.getProject(), activity.getGitPath()));
                } catch (Exception e) {
                    model.addAttribute("gitSyncWarning", "Konnte nicht mit Git synchronisieren, zeige zwischengespeicherten Stand: " + e.getMessage());
                }
            }
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
        model.addAttribute("allActivityCategories", activityDAO.getAllActivityCategoriesForProject(projectContextService.current().getId()));
        model.addAttribute("activity", activity);

        return "activity/activity_edit";
    }

    @PostMapping({"/activityedit", "/activityedit/{id}"})
    public String activitySubmit(@PathVariable(value = "id", required = false) Long activityId, @Valid @ModelAttribute Activity activity, BindingResult bindingResult,
                                 @RequestParam(value = "run", defaultValue = "false") boolean run, Model model)
    {
        activityValidate(activity, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("allActivityApprovalStrategies", activityDAO.getAllActivityApprovalStrategies());
            model.addAttribute("allActivityCategories", activityDAO.getAllActivityCategoriesForProject(projectContextService.current().getId()));

            return "activity/activity_edit";
        }

        if (activity.getActivityCategory().getId() == 0) {
            activity.setActivityCategory(null);
        }

        // The form carries no project (projects change only in Admin > Projects > Assignments) -
        // keep the stored one on edit, default project on create. Must precede the Git block,
        // which writes into the project's repository.
        Activity previousActivity = activity.getId() != null ? activityDAO.getActivityForId(activity.getId()) : null;
        activity.setProject(previousActivity != null && previousActivity.getProject() != null ? previousActivity.getProject() : projectContextService.current());
        userScope.requireWrite(activity.getProject());

        if (activityGitRepositoryService.isActive(activity.getProject())) {
            try {
                if (!ActivityGitRepositoryService.hasGitPath(activity.getGitPath())) {
                    String slug = activityGitRepositoryService.uniqueSlug(activity.getProject(), 
                            activityGitRepositoryService.slugify(activity.getName()),
                            ActivityGitRepositoryService.ACTIVITIES_DIRECTORY);
                    activity.setGitPath(ActivityGitRepositoryService.ACTIVITIES_DIRECTORY + "/" + slug + ".groovy");
                    activityGitRepositoryService.writeActivitySource(activity.getProject(), activity.getGitPath(), activity.getGroovyCode(), "Create activity: " + activity.getName());
                } else {
                    Set<String> siblingPaths = new HashSet<>();
                    for (Activity other : activityDAO.getAllActivities()) {
                        if (!other.getId().equals(activity.getId()) && ActivityGitRepositoryService.hasGitPath(other.getGitPath())) {
                            siblingPaths.add(other.getGitPath());
                        }
                    }

                    String desiredPath = activityGitRepositoryService.desiredPath(ActivityGitRepositoryService.ACTIVITIES_DIRECTORY, activity.getName(), activity.getGitPath(), siblingPaths);

                    if (!desiredPath.equals(activity.getGitPath())) {
                        activityGitRepositoryService.renameAndWriteActivitySource(activity.getProject(), activity.getGitPath(), desiredPath, activity.getGroovyCode(), "Rename activity: " + activity.getName());
                        activity.setGitPath(desiredPath);
                    } else {
                        activityGitRepositoryService.writeActivitySource(activity.getProject(), activity.getGitPath(), activity.getGroovyCode(), "Update activity: " + activity.getName());
                    }
                }
            } catch (Exception e) {
                bindingResult.rejectValue("groovyCode", "activity.groovyCode", "Konnte nicht ins Git-Repository schreiben: " + e.getMessage() + StacktraceUtil.getStackTrace(e));

                model.addAttribute("allActivityApprovalStrategies", activityDAO.getAllActivityApprovalStrategies());
                model.addAttribute("allActivityCategories", activityDAO.getAllActivityCategoriesForProject(projectContextService.current().getId()));

                return "activity/activity_edit";
            }
        }

        activityDAO.saveOrUpdateActivity(activity);

        // "Save & Run" on the edit page: runs exactly what was just saved (a plain run link would
        // run the stored code and silently ignore unsaved edits), then shows its instances.
        // Reloaded: the form-bound activity carries its approval strategy as a bare id without a
        // name, which ActivityInstanceRunner would read as "approval required" and never start.
        if (run) {
            startInstance(activityDAO.getActivityForId(activity.getId()));

            return "redirect:/activity/activityinstances/" + activity.getId();
        }

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
        Activity activity = activityDAO.getActivityForId(activityId);
        userScope.requireWrite(activity.getProject());
        startInstance(activity);

        // Straight to the activity's instances, where the new run and its console appear - not
        // back to page 1 of the list.
        return "redirect:/activity/activityinstances/" + activityId;
    }

    /** Starts one run of the activity (no parameters) - the list's play icon and the edit page's "Save & Run". */
    private void startInstance(Activity activity)
    {
        ActivityInstance activityInstance = new ActivityInstance();
        activityInstance.setActivity(activity);

        activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);

        activityInstanceRunner.runActivity(activityInstance);
    }

    @GetMapping({"/activityschedule/{id}"})
    public String activitySchedule(@PathVariable(value = "id", required = true) Long activityId)
    {
        Activity activity = activityDAO.getActivityForId(activityId);
        userScope.requireWrite(activity.getProject());
        activity.setCronScheduleEnabled(true);

        activityDAO.saveOrUpdateActivity(activity);

        schedulerService.scheduleActivity(activity);

        return "redirect:/activity/";
    }

    @GetMapping({"/activitydeleteschedule/{id}"})
    public String activityDeleteSchedule(@PathVariable(value = "id", required = true) Long activityId) throws SchedulerException
    {
        Activity activity = activityDAO.getActivityForId(activityId);
        userScope.requireWrite(activity.getProject());
        activity.setCronScheduleEnabled(false);

        schedulerService.deleteJob("Activity" + activity.getId().toString(),"Activities");

        activityDAO.saveOrUpdateActivity(activity);

        return "redirect:/activity/";
    }

    @GetMapping({"/activitydelete/{id}"})
    public String activityDelete(@PathVariable(value = "id", required = true) Long activityId, RedirectAttributes redirectAttributes)
    {
        Activity activity = activityDAO.getActivityForId(activityId);
        userScope.requireWrite(activity.getProject());
        if (ActivityGitRepositoryService.hasGitPath(activity.getGitPath()) && activityGitRepositoryService.isActive(activity.getProject())) {
            try {
                activityGitRepositoryService.deleteAndPush(activity.getProject(), activity.getGitPath(), "Delete activity: " + activity.getName());
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("gitSyncWarning", "Konnte Datei nicht aus Git löschen: " + e.getMessage());
            }
        }

        activityDAO.deleteActivity(activity);

        return "redirect:/activity/";
    }

    @GetMapping({"/activityrunwithparameters/{activityid}"})
    public String activityRunWithParameters(@PathVariable(value = "activityid", required = true) Long activityId, Model model, HttpServletRequest request)
    {
        if (request.getSession().getAttribute("activityInstance") == null || ((ActivityInstance) request.getSession().getAttribute("activityInstance")).getActivity().getId() != activityId) {
            ActivityInstance activityInstance = new ActivityInstance();
            activityInstance.setActivity(activityDAO.getActivityForId(activityId));
        userScope.requireWrite(activityInstance.getActivity().getProject());

            request.getSession().setAttribute("activityInstance", activityInstance);
        }

//        model.addAttribute("activityId",activityId);
        model.addAttribute("activityInstance", request.getSession().getAttribute("activityInstance"));

        return "activity/run_with_parameters";
    }

    @PostMapping({"/activityrunwithparameters/","/activityrunwithparameters/{activityid}"})
    public String activityRunWithParametersSubmit(@PathVariable(value = "activityid", required = false) Long activityId, @Valid @ModelAttribute ActivityInstance activityInstance, BindingResult bindingResult, Model model, HttpServletRequest request)
    {
        Long targetActivityId = activityInstance.getActivity() != null && activityInstance.getActivity().getId() != null ? activityInstance.getActivity().getId() : activityId;
        userScope.requireWrite(activityDAO.getActivityForId(targetActivityId).getProject());

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
        userScope.requireWrite(activityDAO.getActivityForId(activityId).getProject());

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
        userScope.requireWrite(activityInstance.getActivity().getProject());
        activityInstanceDAO.deleteActivityInstance(activityInstance);

        return "redirect:/activity/activityinstances/" + activityInstance.getActivity().getId();
    }

    @GetMapping({"/activityinstanceterminate/{activityInstanceId}"})
    public String activityInstanceTerminate(@PathVariable(value = "activityInstanceId", required = true) Long activityInstanceId)
    {
        ActivityInstance activityInstance = activityInstanceDAO.getActivityInstanceForId(activityInstanceId);
        userScope.requireWrite(activityInstance.getActivity().getProject());
        activityInstanceRunner.terminateActivity(activityInstance);

        return "redirect:/activity/activityinstances/" + activityInstance.getActivity().getId();
    }
}
