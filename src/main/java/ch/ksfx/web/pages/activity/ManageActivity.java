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
import ch.ksfx.model.activity.*;
import ch.ksfx.util.GenericSelectModel;
import ch.ksfx.util.StacktraceUtil;
import groovy.lang.GroovyClassLoader;
import org.apache.commons.io.IOUtils;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.PropertyAccess;
import org.quartz.CronExpression;
import org.springframework.security.access.annotation.Secured;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;


@Secured({"ROLE_ADMIN"})
public class ManageActivity
{
    @Property
    private Activity activity;

    @Property
    private RequiredActivity requiredActivity;

    @Property
    private TriggerActivity triggerActivity;

    @Property @Persist
    private GenericSelectModel<Activity> allActivities;

    @Property
    private GenericSelectModel<ActivityCategory> allActivityCategories;

    @Inject
    private ActivityDAO activityDAO;

    @InjectComponent
    private Form activityForm;

    @Inject
    private PropertyAccess propertyAccess;

    @Property
    private GenericSelectModel<ActivityApprovalStrategy> allActivityApprovalStrategies;

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long activityId)
    {
        activity = activityDAO.getActivityForId(activityId);
    }

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {
        if (activity == null) {
            activity = new Activity();

            InputStream is = null;
            String activityDemo = null;

            try {
                is = getClass().getClassLoader().getResourceAsStream("DemoActivity.groovy");

                StringWriter writer = new StringWriter();
                IOUtils.copy(is, writer, "UTF-8");
                activityDemo = writer.toString();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            activity.setGroovyCode(activityDemo);
        }

        allActivityApprovalStrategies = new GenericSelectModel<ActivityApprovalStrategy>(activityDAO.getAllActivityApprovalStrategies(),ActivityApprovalStrategy.class,"name","id",propertyAccess);
        allActivities = new GenericSelectModel<Activity>(activityDAO.getAllActivities(),Activity.class,"name","id",propertyAccess);
        allActivityCategories = new GenericSelectModel<ActivityCategory>(activityDAO.getAllActivityCategories(),ActivityCategory.class,"name","id",propertyAccess);
    }

    public Long onPassivate()
    {
        if (activity != null) {
            return activity.getId();
        }

        return null;
    }

    public void onActionFromAddRequiredActivity()
    {
        RequiredActivity requiredActivity = new RequiredActivity();
        requiredActivity.setActivity(activity);

        activityDAO.saveOrUpdateRequiredActivity(requiredActivity);
        activityDAO.saveOrUpdateActivity(activity);
    }

    public void onActionFromAddTriggerActivity()
    {
        TriggerActivity triggerActivity = new TriggerActivity();
        triggerActivity.setActivity(activity);

        activityDAO.saveOrUpdateTriggerActivity(triggerActivity);
        activityDAO.saveOrUpdateActivity(activity);
    }

    public void onSuccess()
    {
        if (activity.getRequiredActivities() != null) {
            for (RequiredActivity requiredActivity : activity.getRequiredActivities()) {
                activityDAO.saveOrUpdateRequiredActivity(requiredActivity);
            }
        }

        if (activity.getTriggerActivities() != null) {
            for (TriggerActivity triggerActivity : activity.getTriggerActivities()) {
                activityDAO.saveOrUpdateTriggerActivity(triggerActivity);
            }
        }

        activityDAO.saveOrUpdateActivity(activity);
    }

    public void onValidateFromActivityForm()
    {
        try {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(activity.getGroovyCode());

            Constructor cons = clazz.getDeclaredConstructor(ObjectLocator.class);
        } catch (Exception e) {
            activityForm.recordError(StacktraceUtil.getStackTrace(e));
        }

        if (activity.getCronSchedule() != null && !activity.getCronSchedule().isEmpty()) {
            try {
                CronExpression cronExpression = new CronExpression(activity.getCronSchedule());
            } catch (Exception e) {
                activityForm.recordError("Cron Schedule not valid");
            }
        }
    }
}
