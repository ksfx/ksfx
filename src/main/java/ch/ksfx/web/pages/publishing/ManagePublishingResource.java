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

package ch.ksfx.web.pages.publishing;

import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.publishing.PublishingResourceDAO;
import ch.ksfx.model.PublishingConfiguration;
import ch.ksfx.model.publishing.PublishingResource;
import ch.ksfx.util.StacktraceUtil;
import groovy.lang.GroovyClassLoader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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


@Secured({"ROLE_ADMIN"})
public class ManagePublishingResource
{
	@Property
	private PublishingResource publishingResource;
	
	@Inject
	private PublishingResourceDAO publishingResourceDAO;
	
	@Inject
	private PublishingConfigurationDAO publishingConfigurationDAO;

    @InjectComponent
    private Form publishingResourceForm;
	
	@Secured({"ROLE_ADMIN"})
	public void onActivate(Long publishingResourceId)
	{
		if (publishingResource == null) {
			publishingResource = publishingResourceDAO.getPublishingResourceForId(publishingResourceId);
		}
	}
	
	@Secured({"ROLE_ADMIN"})
	public void onActivate(Long publishingConfigurationId, String createNew)
	{
		if (createNew.equalsIgnoreCase("new")) {
			PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
			publishingResource = new PublishingResource();
			publishingResource.setPublishingConfiguration(publishingConfiguration);
			
			InputStream is = null;
            String demoPublishingConfiguration = null;

            try {
                is = getClass().getClassLoader().getResourceAsStream("DemoPublishingResource.groovy");

                StringWriter writer = new StringWriter();
                IOUtils.copy(is, writer, "UTF-8");
                demoPublishingConfiguration = writer.toString();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            publishingResource.setPublishingStrategy(demoPublishingConfiguration);
			
			publishingResourceDAO.saveOrUpdatePublishingResource(publishingResource);
		}
	}
	
	public void onSuccess()
	{
		publishingResourceDAO.saveOrUpdatePublishingResource(publishingResource);
	}
	
	public Long onPassivate()
    {
        if (publishingResource != null) {
            return publishingResource.getId();
        }

        return null;
    }

    public void onValidateFromPublishingResourceForm()
    {
        if (publishingResource.getPublishingConfiguration().getLockedForEditing() == true) {
            publishingResourceForm.recordError("This publishing configuration is locked, please unlock it first!");
        }

        try {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(publishingResource.getPublishingStrategy());

            Constructor cons = clazz.getDeclaredConstructor(ObjectLocator.class);
        } catch (Exception e) {
            publishingResourceForm.recordError(StacktraceUtil.getStackTrace(e));
        }
    }
}
 


