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

package ch.ksfx.dao.ebean.activity;

import ch.ksfx.dao.activity.ActivityExecutionDAO;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.ActivityExecution;
import ch.ksfx.web.services.ObjectLocatorService;
import ch.ksfx.web.services.logger.SystemLogger;
import groovy.lang.GroovyClassLoader;
import org.apache.tapestry5.ioc.ObjectLocator;

import java.lang.reflect.Constructor;


public class EbeanActivityExecutionDAO implements ActivityExecutionDAO
{
    private SystemLogger systemLogger;
    private ObjectLocatorService objectLocatorService;

    public EbeanActivityExecutionDAO(SystemLogger systemLogger, ObjectLocatorService objectLocatorService)
    {
        this.systemLogger = systemLogger;
        this.objectLocatorService = objectLocatorService;
    }

    @Override
    public ActivityExecution getActivityExecution(Activity activity)
    {
        System.out.println("activity " + activity);
        try {
            if (activity.getGroovyCode() == null || activity.getGroovyCode().isEmpty()) {
                throw new IllegalArgumentException("Result unit modifier has no code");
            }

            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(activity.getGroovyCode());

            Constructor cons = clazz.getDeclaredConstructor(ObjectLocator.class);

            return (ActivityExecution) cons.newInstance(objectLocatorService.getObjectLocator());
        } catch (Exception e) {
            e.printStackTrace();
            systemLogger.logMessage("FATAL","Error while getting activity execution strategy",e);
        }

        return null;
    }
}

