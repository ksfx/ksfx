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
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.springframework.security.access.annotation.Secured;

import java.util.List;


public class ResourceLoaderPluginConfigurationIndex
{
    @Inject
    private ResourceLoaderPluginConfigurationDAO resourceLoaderPluginConfigurationDAO;

    @Property
    private ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration;
    
    @Inject
    private PageRenderLinkSource pageRenderLinkSource;

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {

    }

    public List<ResourceLoaderPluginConfiguration> getAllResourceLoaderPluginConfigurations()
    {
        return resourceLoaderPluginConfigurationDAO.getAllResourceLoaderPluginConfigurations();
    }

    public void onActionFromDelete(Long resourceLoaderPluginId)
    {

    }
    
    public String getIframeInfoLink()
    {
    	return pageRenderLinkSource.createPageRenderLinkWithContext("admin/note/viewnotesforentityplain", getInfoContextParameters()).toURI();	
    }
    
    public Object[] getInfoContextParameters()
    {
        if (resourceLoaderPluginConfiguration != null) {
            return new Object[]{null,
                    null,
                    null,null,resourceLoaderPluginConfiguration.getId()};
        }

        return null;
    }
}
