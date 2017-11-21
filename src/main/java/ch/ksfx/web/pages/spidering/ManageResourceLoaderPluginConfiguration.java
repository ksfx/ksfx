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

package ch.ksfx.web.pages.spidering;

import ch.ksfx.dao.spidering.ResourceLoaderPluginConfigurationDAO;
import ch.ksfx.model.spidering.ResourceLoaderPluginConfiguration;
import ch.ksfx.util.StacktraceUtil;
import groovy.lang.GroovyClassLoader;
import org.apache.commons.io.IOUtils;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;


public class ManageResourceLoaderPluginConfiguration
{
    @Property
    private ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration;

    @Inject
    private ResourceLoaderPluginConfigurationDAO resourceLoaderPluginConfigurationDAO;

    @InjectComponent
    private Form resourceLoaderPluginConfigurationForm;

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long resourceLoaderPluginConfigurationId)
    {
        resourceLoaderPluginConfiguration = resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId);
    }

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {
        if (resourceLoaderPluginConfiguration == null) {
            resourceLoaderPluginConfiguration = new ResourceLoaderPluginConfiguration();

            InputStream is = null;
            String resourceLoaderPluginDemo = null;

            try {
                is = getClass().getClassLoader().getResourceAsStream("DemoResourceLoaderPlugin.groovy");

                StringWriter writer = new StringWriter();
                IOUtils.copy(is, writer, "UTF-8");
                resourceLoaderPluginDemo = writer.toString();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            resourceLoaderPluginConfiguration.setGroovyCode(resourceLoaderPluginDemo);
        }
    }

    public Long onPassivate()
    {
        if (resourceLoaderPluginConfiguration != null) {
            return resourceLoaderPluginConfiguration.getId();
        }

        return null;
    }

    public void onSuccess()
    {
        resourceLoaderPluginConfigurationDAO.saveOrUpdate(resourceLoaderPluginConfiguration);
    }

    public void onValidateFromResourceLoaderPluginConfigurationForm()
    {
        try {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(resourceLoaderPluginConfiguration.getGroovyCode());

            Constructor cons = clazz.getDeclaredConstructor(ObjectLocator.class);
        } catch (Exception e) {
            resourceLoaderPluginConfigurationForm.recordError(StacktraceUtil.getStackTrace(e));
        }
    }
}
