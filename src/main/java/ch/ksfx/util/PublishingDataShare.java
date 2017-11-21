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

package ch.ksfx.util;

import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.ebean.EbeanPublishingConfigurationDAO;
import ch.ksfx.dao.ebean.publishing.EbeanPublishingSharedDataDAO;
import ch.ksfx.dao.publishing.PublishingSharedDataDAO;
import ch.ksfx.model.PublishingConfiguration;
import ch.ksfx.model.publishing.PublishingSharedData;

 
public class PublishingDataShare
{
	private static final ThreadLocal<Long> publicationConfigurationId = new ThreadLocal<Long>();
	
	public static void startShare(PublishingConfiguration publishingConfiguration)
	{
		publicationConfigurationId.set(publishingConfiguration.getId());
	}
	
	public static void addData(String dataKey, PublishingSharedData publishingSharedData)
	{
		PublishingSharedDataDAO publishingSharedDataDAO = new EbeanPublishingSharedDataDAO();
		PublishingConfigurationDAO publishingConfigurationDAO = new EbeanPublishingConfigurationDAO();
		
		PublishingSharedData existing = publishingSharedDataDAO.getPublishingSharedDataForDataKeyAndPublishingConfiguration(dataKey, publishingConfigurationDAO.getPublishingConfigurationForId(publicationConfigurationId.get()));
		if (existing != null) {
			publishingSharedDataDAO.deletePublishingSharedData(existing);
		}
		
		publishingSharedData.setDataKey(dataKey);
		publishingSharedData.setPublishingConfiguration(publishingConfigurationDAO.getPublishingConfigurationForId(publicationConfigurationId.get()));
		
		publishingSharedDataDAO.saveOrUpdatePublishingSharedData(publishingSharedData);
	}
	
	public static PublishingSharedData getData(String dataKey)
	{
		PublishingSharedDataDAO publishingSharedDataDAO = new EbeanPublishingSharedDataDAO();
		PublishingConfigurationDAO publishingConfigurationDAO = new EbeanPublishingConfigurationDAO();
		
		PublishingSharedData publishingSharedData = publishingSharedDataDAO.getPublishingSharedDataForDataKeyAndPublishingConfiguration(dataKey, publishingConfigurationDAO.getPublishingConfigurationForId(publicationConfigurationId.get()));
		
		return publishingSharedData;
	}
	
	public static void endShare()
	{
		publicationConfigurationId.remove();
	}
}

