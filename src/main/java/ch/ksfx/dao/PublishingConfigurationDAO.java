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

package ch.ksfx.dao;

import ch.ksfx.model.PublishingCategory;
import ch.ksfx.model.PublishingConfiguration;
import ch.ksfx.model.publishing.PublishingConfigurationCacheData;

import java.util.List;


public interface PublishingConfigurationDAO
{
    public PublishingConfiguration getPublishingConfigurationForId(Long publishingConfigurationId);
    public List<PublishingConfiguration> getAllPublishingConfigurations();
    public List<PublishingConfiguration> getPublishingConfigurationsForPublishingCategory(PublishingCategory publishingCategory);
    public void saveOrUpdatePublishingConfiguration(PublishingConfiguration publishingConfiguration);
    public void deletePublishingConfiguration(PublishingConfiguration publishingConfiguration);
    public PublishingCategory getPublishingCategoryForId(Long publishingCategoryId);
    public List<PublishingCategory> getAllPublishingCategories();
    public void saveOrUpdatePublishingCategory(PublishingCategory publishingCategory);
    public void deletePublishingCategory(PublishingCategory publishingCategory);
    public void appendConsole(Long publishingConfigurationId, String dataToAppend);
    public PublishingConfiguration getPublishingConfigurationForUri(String uri);
    
   	public void saveOrUpdatePublishingConfigurationCacheData(PublishingConfigurationCacheData publishingConfigurationCacheData);
	public PublishingConfigurationCacheData getPublishingConfigurationCacheDataForPublishingConfigurationAndUriParameter(PublishingConfiguration publishingConfiguration, String uriParameter);
	public void deletePublishingConfigurationCacheData(PublishingConfigurationCacheData publishingConfigurationCacheData);
}
