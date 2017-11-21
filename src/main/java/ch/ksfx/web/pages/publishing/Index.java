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
import ch.ksfx.model.PublishingCategory;
import ch.ksfx.model.PublishingConfiguration;
import ch.ksfx.model.publishing.PublishingConfigurationCacheData;
import ch.ksfx.model.publishing.PublishingResource;
import ch.ksfx.model.publishing.PublishingResourceCacheData;
import ch.ksfx.util.GenericSelectModel;
import ch.ksfx.web.services.ObjectLocatorService;
import ch.ksfx.web.services.logger.SystemLogger;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.PropertyAccess;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.springframework.security.access.annotation.Secured;

import java.util.List;


@Secured({"ROLE_ADMIN"})
public class Index
{
	@Inject
	private PageRenderLinkSource pageRenderLinkSource;
	
    @Inject
    private PublishingConfigurationDAO publishingConfigurationDAO;
    
    @Inject
    private PublishingResourceDAO publishingResourceDAO;

    @Inject
    private ObjectLocatorService objectLocatorService;

    @Inject
    private SystemLogger systemLogger;

    @Inject
    private PropertyAccess propertyAccess;

    @Property
    private PublishingConfiguration publishingConfiguration;

    @Property
    @Persist
    private PublishingCategory publishingCategory;

    @Property
    private GenericSelectModel<PublishingCategory> allPublishingCategories;

    public void onActivate()
    {
        allPublishingCategories = new GenericSelectModel<PublishingCategory>(publishingConfigurationDAO.getAllPublishingCategories(),PublishingCategory.class,"name","id",propertyAccess);
    }

    public List<PublishingConfiguration> getAllPublishingConfigurations()
    {
        return publishingConfigurationDAO.getPublishingConfigurationsForPublishingCategory(publishingCategory);
    }

    public void onActionFromDeletePublishingConfiguration(Long publishingConfigurationId)
    {
        PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
        publishingConfigurationDAO.deletePublishingConfiguration(publishingConfiguration);
    }
    
    public void onActionFromPurgePublishingCache(Long publishingConfigurationId)
    {
        PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
    
        if (!publishingConfiguration.getLockedForCacheUpdate()) {
            publishingConfiguration.setConsole("");
            publishingConfigurationDAO.saveOrUpdatePublishingConfiguration(publishingConfiguration);

            for (PublishingConfigurationCacheData pccd : publishingConfiguration.getPublishingConfigurationCacheDatas()) {
                publishingConfigurationDAO.deletePublishingConfigurationCacheData(pccd);
            }
            
            for (PublishingResource publishingResourse : publishingResourceDAO.getAllPublishingResourcesForPublishingConfiguration(publishingConfiguration)) {
                for (PublishingResourceCacheData prcd : publishingResourse.getPublishingResourceCacheDatas()) {
                    publishingResourceDAO.deletePublishingResourceCacheData(prcd);
                }
            }
        }
    }
    
    public String getIframeInfoLink()
    {
    	return pageRenderLinkSource.createPageRenderLinkWithContext("admin/note/viewnotesforentityplain", getInfoContextParameters()).toURI();	
    }
    
    public Object[] getInfoContextParameters()
    {
        if (publishingConfiguration != null) {
            return new Object[]{null,
                    null,
                    publishingConfiguration.getId(),null,null};
        }

        return null;
    }
    
    public String getIframeConsoleLink()
    {
    	return pageRenderLinkSource.createPageRenderLinkWithContext("ViewConsoleForEntityPlain", getConsoleContextParameters()).toURI();	
    }
    
    public Object[] getConsoleContextParameters()
    {
        if (publishingConfiguration != null) {
            return new Object[]{publishingConfiguration.getId(),
                    "publishingConfiguration"};
        }

        return null;
    }

    /*
    public StreamResponse onActionFromGeneratePublication(Long publishingConfigurationId)
    {
        try {
            PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);

            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(publishingConfiguration.getPublishingStrategy());
            Constructor cons = clazz.getDeclaredConstructor(ObjectLocator.class);

            PublishingStrategy publishingStrategy = (PublishingStrategy) cons.newInstance(objectLocatorService.getObjectLocator());

            return publishingStrategy.getPublishingData();
        } catch (Exception e) {
            systemLogger.logMessage("PUBLICATION","Error while creating publication PublicationViewer",e);
            throw new RuntimeException(e);
        }
    }
    */
}
