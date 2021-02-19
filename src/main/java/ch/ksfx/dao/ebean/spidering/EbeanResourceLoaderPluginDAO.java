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

package ch.ksfx.dao.ebean.spidering;

import ch.ksfx.dao.spidering.ResourceLoaderPluginDAO;
import ch.ksfx.model.spidering.ResourceLoaderPlugin;
import ch.ksfx.model.spidering.ResourceLoaderPluginConfiguration;
import groovy.lang.GroovyClassLoader;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Constructor;

@Repository
public class EbeanResourceLoaderPluginDAO implements ResourceLoaderPluginDAO
{
//    private SystemLogger systemLogger;
//    private ObjectLocatorService objectLocatorService;

    public EbeanResourceLoaderPluginDAO(/*SystemLogger systemLogger, ObjectLocatorService objectLocatorService*/)
    {
//        this.systemLogger = systemLogger;
//        this.objectLocatorService = objectLocatorService;
    }

    @Override
    public ResourceLoaderPlugin getResourceLoaderPlugin(ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration)
    {
        System.out.println("resource loader plugin configuration " + resourceLoaderPluginConfiguration);
        try {
            if (resourceLoaderPluginConfiguration.getGroovyCode() == null || resourceLoaderPluginConfiguration.getGroovyCode().isEmpty()) {
                throw new IllegalArgumentException("Resource Loader Plugin Configuration unit modifier has no code");
            }

            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(resourceLoaderPluginConfiguration.getGroovyCode());

            Constructor cons = clazz.getDeclaredConstructor(/*ObjectLocator.class*/);

            return (ResourceLoaderPlugin) cons.newInstance(/*objectLocatorService.getObjectLocator()*/);
        } catch (Exception e) {
            e.printStackTrace();
//            systemLogger.logMessage("FATAL","Error while getting Resource Loader Plugin Configuration",e);
        }

        return null;
    }
}

