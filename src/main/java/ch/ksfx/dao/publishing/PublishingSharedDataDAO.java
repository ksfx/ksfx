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

import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.model.publishing.PublishingSharedData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface PublishingSharedDataDAO
{
	public void saveOrUpdatePublishingSharedData(PublishingSharedData publishingSharedData);
	public List<PublishingSharedData> getAllPublishingSharedDatas();
	public PublishingSharedData getPublishingSharedDataForId(Long publishingSharedDataId);
	public PublishingSharedData getPublishingSharedDataForDataKeyAndPublishingConfiguration(String dataKey, PublishingConfiguration publishingConfiguration);
	public void deletePublishingSharedData(PublishingSharedData publishingSharedData);
	public List<PublishingSharedData> getAllPublishingSharedDatasForPublishingConfiguration(PublishingConfiguration publishingConfiguration);
	public Page<PublishingSharedData> getPublishingSharedDataForPageableAndPublishingConfiguration(Pageable pageable, PublishingConfiguration publishingConfiguration);
}
 


