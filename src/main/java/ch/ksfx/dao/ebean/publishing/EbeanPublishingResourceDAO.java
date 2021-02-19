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

package ch.ksfx.dao.ebean.publishing;

import ch.ksfx.dao.publishing.PublishingResourceDAO;
import ch.ksfx.model.PublishingConfiguration;
import ch.ksfx.model.publishing.PublishingResource;
import ch.ksfx.model.publishing.PublishingResourceCacheData;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanPublishingResourceDAO implements PublishingResourceDAO
{
	@Override
	public void saveOrUpdatePublishingResource(PublishingResource publishingResource)
	{
		if (publishingResource.getId() != null) {
			Ebean.update(publishingResource);
		} else {
			Ebean.save(publishingResource);
		}
	}
	
	@Override
	public List<PublishingResource> getAllPublishingResources()
	{
		return Ebean.find(PublishingResource.class).findList();
	}
	
	@Override
	public PublishingResource getPublishingResourceForId(Long publishingResourceId)
	{
		return Ebean.find(PublishingResource.class, publishingResourceId);
	}
	
	@Override
	public PublishingResource getPublishingResourceForUri(String uri)
	{
		return Ebean.find(PublishingResource.class).where().eq("uri", uri).findUnique();
	}
	
	@Override
	public void deletePublishingResource(PublishingResource publishingResource)
	{
		Ebean.delete(publishingResource);
	}
	
	@Override
	public List<PublishingResource> getAllPublishingResourcesForPublishingConfiguration(PublishingConfiguration publishingConfiguration)
	{
		return Ebean.find(PublishingResource.class).where().eq("publishingConfiguration", publishingConfiguration).findList();
	}
	
	@Override
	public PublishingResource getPublishingResourceForPublishingConfigurationAndUri(PublishingConfiguration publishingConfiguration, String uri)
	{
		return Ebean.find(PublishingResource.class).where().eq("uri", uri).eq("publishingConfiguration", publishingConfiguration).findUnique();
	}
	
	@Override
	public void saveOrUpdatePublishingResourceCacheData(PublishingResourceCacheData publishingResourceCacheData)
	{
		if (publishingResourceCacheData.getId() != null) {
			Ebean.update(publishingResourceCacheData);
		} else {
			Ebean.save(publishingResourceCacheData);
		}
	}
	
	@Override
	public PublishingResourceCacheData getPublishingResourceCacheDataForPublishingResourceAndUriParameter(PublishingResource publishingResource, String uriParameter)
	{
		return Ebean.find(PublishingResourceCacheData.class).where().eq("publishingResource", publishingResource).eq("uriParameter", uriParameter).findUnique();
	}
	
	@Override
	public void deletePublishingResourceCacheData(PublishingResourceCacheData publishingResourceCacheData)
	{
		Ebean.delete(publishingResourceCacheData);
	}
}
 


