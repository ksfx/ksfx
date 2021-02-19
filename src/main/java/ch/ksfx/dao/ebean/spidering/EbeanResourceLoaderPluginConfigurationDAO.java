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

import ch.ksfx.dao.spidering.ResourceLoaderPluginConfigurationDAO;
import ch.ksfx.model.spidering.ResourceLoaderPluginConfiguration;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanResourceLoaderPluginConfigurationDAO implements ResourceLoaderPluginConfigurationDAO
{
    @Override
    public void saveOrUpdate(ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration)
    {
        if (resourceLoaderPluginConfiguration.getId() != null) {
            Ebean.update(resourceLoaderPluginConfiguration);
        } else {
            Ebean.save(resourceLoaderPluginConfiguration);
        }
    }

    @Override
    public List<ResourceLoaderPluginConfiguration> getAllResourceLoaderPluginConfigurations()
    {
        return Ebean.find(ResourceLoaderPluginConfiguration.class).findList();
    }

    @Override
    public ResourceLoaderPluginConfiguration getResourceLoaderPluginConfigurationForId(Long resourceLoaderPluginConfigurationId)
    {
        return Ebean.find(ResourceLoaderPluginConfiguration.class, resourceLoaderPluginConfigurationId);
    }

    @Override
    public void deleteResourceLoaderPluginConfiguration(ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration)
    {
        Ebean.delete(resourceLoaderPluginConfiguration);
    }
}
