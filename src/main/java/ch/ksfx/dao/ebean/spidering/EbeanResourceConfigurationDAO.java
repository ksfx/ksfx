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

import ch.ksfx.dao.spidering.ResourceConfigurationDAO;
import ch.ksfx.model.spidering.ResourceConfiguration;
import ch.ksfx.model.spidering.ResultUnitConfiguration;
import ch.ksfx.model.spidering.UrlFragment;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanResourceConfigurationDAO implements ResourceConfigurationDAO
{
    @Override
    public void saveOrUpdate(ResourceConfiguration resourceConfiguration)
    {
        if (resourceConfiguration.getId() != null) {
            Ebean.update(resourceConfiguration);
        } else {
            Ebean.save(resourceConfiguration);
        }
    }

    @Override
    public List<ResourceConfiguration> getAllResourceConfigurations()
    {
        return Ebean.find(ResourceConfiguration.class).findList();
    }

    @Override
    public ResourceConfiguration getResourceConfigurationForId(Long resourceConfigurationId)
    {
        return Ebean.find(ResourceConfiguration.class, resourceConfigurationId);
    }

    @Override
    public void deleteResourceConfiguration(ResourceConfiguration resourceConfiguration)
    {
        if (resourceConfiguration.getUrlFragments() != null) {
            for (UrlFragment uf : resourceConfiguration.getUrlFragments()) {
                Ebean.delete(uf);
            }
        }

        if (resourceConfiguration.getResultUnitConfigurations() != null) {
            for (ResultUnitConfiguration ru : resourceConfiguration.getResultUnitConfigurations()) {
                Ebean.delete(ru);
            }
        }

        Ebean.delete(resourceConfiguration);
    }
}
