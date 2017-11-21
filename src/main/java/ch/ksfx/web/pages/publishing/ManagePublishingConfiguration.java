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
import ch.ksfx.dao.publishing.PublishingSharedDataDAO;
import ch.ksfx.model.PublishingCategory;
import ch.ksfx.model.PublishingConfiguration;
import ch.ksfx.model.publishing.PublishingResource;
import ch.ksfx.model.publishing.PublishingSharedData;
import ch.ksfx.util.GenericSelectModel;
import ch.ksfx.util.StacktraceUtil;
import groovy.lang.GroovyClassLoader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.PropertyAccess;
import org.springframework.security.access.annotation.Secured;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.List;


@Secured({"ROLE_ADMIN"})
public class ManagePublishingConfiguration
{
    @Inject
    private PublishingConfigurationDAO publishingConfigurationDAO;
	
	@Inject
	private PublishingResourceDAO publishingResourceDAO;
	
	@Inject
	private PublishingSharedDataDAO publishingSharedDataDAO;

    @InjectComponent
    private Form publishingConfigurationForm;

    @Property
    private PublishingConfiguration publishingConfiguration;
	
	@Property
	private PublishingResource publishingResource;
	
	@Property
	private PublishingSharedData publishingSharedData;

    @Property
    private GenericSelectModel<PublishingCategory> allPublishingCategories;

    @Inject
    private PropertyAccess propertyAccess;

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long publishingConfigurationId)
    {
        publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
    }

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {
        if (publishingConfiguration == null) {
            publishingConfiguration = new PublishingConfiguration();

            InputStream is = null;
            String demoPublishingConfiguration = null;

            try {
                is = getClass().getClassLoader().getResourceAsStream("DemoPublishingConfiguration.groovy");

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

            publishingConfiguration.setPublishingStrategy(demoPublishingConfiguration);
        }

        allPublishingCategories = new GenericSelectModel<PublishingCategory>(publishingConfigurationDAO.getAllPublishingCategories(),PublishingCategory.class,"name","id",propertyAccess);
    }
	
	public void onActionFromDeletePublishingResource(Long publishingResourceId)
	{
		publishingResourceDAO.deletePublishingResource(publishingResourceDAO.getPublishingResourceForId(publishingResourceId));
	}
	
	public List<PublishingResource> getAllPublishingResources()
	{
		return publishingResourceDAO.getAllPublishingResourcesForPublishingConfiguration(publishingConfiguration);
	}
	
	public void onActionFromDeletePublishingSharedData(Long publishingSharedDataId)
	{
		publishingSharedDataDAO.deletePublishingSharedData(publishingSharedDataDAO.getPublishingSharedDataForId(publishingSharedDataId));
	}
	
	public List<PublishingSharedData> getAllPublishingSharedDatas()
	{
		return publishingSharedDataDAO.getAllPublishingSharedDatasForPublishingConfiguration(publishingConfiguration);
	}

    public Long onPassivate()
    {
        if (publishingConfiguration != null) {
            return publishingConfiguration.getId();
        }

        return null;
    }

    public void onSuccessFromPublishingConfigurationForm()
    {
        PublishingConfiguration publishingConfigurationOld = null;

        if (publishingConfiguration.getId() != null) {
            publishingConfigurationOld = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfiguration.getId());
        }

        if (publishingConfigurationOld == null || publishingConfigurationOld.getLockedForEditing() == false || publishingConfiguration.getLockedForEditing() == false) {
            publishingConfigurationDAO.saveOrUpdatePublishingConfiguration(publishingConfiguration);
        }
    }

    public void onValidateFromPublishingConfigurationForm()
    {
    	PublishingConfiguration publishingConfigurationOld = null;

        if (publishingConfiguration.getId() != null) {
            publishingConfigurationOld = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfiguration.getId());
        }

    	
        if (publishingConfigurationOld != null && publishingConfigurationOld.getLockedForEditing() == true && publishingConfiguration.getLockedForEditing() == true) {
            publishingConfigurationForm.recordError("This publishing configuration is locked, please unlock it first!");
        }
    
        try {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(publishingConfiguration.getPublishingStrategy());

            Constructor cons = clazz.getDeclaredConstructor(ObjectLocator.class);
        } catch (Exception e) {
            publishingConfigurationForm.recordError(StacktraceUtil.getStackTrace(e));
        }
    }

    public String getAbbreviatedTextData()
    {
        if (publishingSharedData.getTextData() != null) {
            return StringUtils.abbreviate(publishingSharedData.getTextData(), 100);
        }

        return "";
    }

    public boolean getTextDataIsAbbreviated()
    {
        return (publishingSharedData.getTextData() != null && publishingSharedData.getTextData().length() > 100);
    }
}
