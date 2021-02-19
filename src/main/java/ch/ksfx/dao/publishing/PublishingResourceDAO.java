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

package ch.ksfx.dao.publishing;

import ch.ksfx.model.PublishingConfiguration;
import ch.ksfx.model.publishing.PublishingResource;
import ch.ksfx.model.publishing.PublishingResourceCacheData;

import java.util.List;


public interface PublishingResourceDAO
{
	public void saveOrUpdatePublishingResource(PublishingResource publishingResource);
	public List<PublishingResource> getAllPublishingResources();
	public PublishingResource getPublishingResourceForId(Long publishingResourceId);
	public PublishingResource getPublishingResourceForUri(String uri);
	public void deletePublishingResource(PublishingResource publishingResource);
	public List<PublishingResource> getAllPublishingResourcesForPublishingConfiguration(PublishingConfiguration publishingConfiguration);
	public PublishingResource getPublishingResourceForPublishingConfigurationAndUri(PublishingConfiguration publishingConfiguration, String uri);
	public void saveOrUpdatePublishingResourceCacheData(PublishingResourceCacheData publishingResourceCacheData);
	public PublishingResourceCacheData getPublishingResourceCacheDataForPublishingResourceAndUriParameter(PublishingResource publishingResource, String uriParameter);
	public void deletePublishingResourceCacheData(PublishingResourceCacheData publishingResourceCacheData);
}
 


