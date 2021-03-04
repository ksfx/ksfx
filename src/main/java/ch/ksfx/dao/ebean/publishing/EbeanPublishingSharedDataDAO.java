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

import ch.ksfx.dao.publishing.PublishingSharedDataDAO;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.model.publishing.PublishingSharedData;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Iterator;
import java.util.List;

@Repository
public class EbeanPublishingSharedDataDAO implements PublishingSharedDataDAO
{
	@Override
	public void saveOrUpdatePublishingSharedData(PublishingSharedData publishingSharedData)
	{
		if (publishingSharedData.getId() != null) {
			Ebean.update(publishingSharedData);
		} else {
			Ebean.save(publishingSharedData);
		}
	}
	
	@Override
	public List<PublishingSharedData> getAllPublishingSharedDatas()
	{
		return Ebean.find(PublishingSharedData.class).findList();
	}
	
	@Override
	public PublishingSharedData getPublishingSharedDataForId(Long publishingSharedDataId)
	{
		return Ebean.find(PublishingSharedData.class, publishingSharedDataId);
	}
	
	@Override
	public PublishingSharedData getPublishingSharedDataForDataKeyAndPublishingConfiguration(String dataKey, PublishingConfiguration publishingConfiguration)
	{
		return Ebean.find(PublishingSharedData.class).where().eq("dataKey", dataKey).eq("publishingConfiguration", publishingConfiguration).findUnique();
	}
	
	@Override
	public void deletePublishingSharedData(PublishingSharedData publishingSharedData)
	{
		Ebean.delete(publishingSharedData);
	}
	
	@Override
	public List<PublishingSharedData> getAllPublishingSharedDatasForPublishingConfiguration(PublishingConfiguration publishingConfiguration)
	{
		return Ebean.find(PublishingSharedData.class).where().eq("publishingConfiguration", publishingConfiguration).findList();
	}

	@Override
	public Page<PublishingSharedData> getPublishingSharedDataForPageableAndPublishingConfiguration(Pageable pageable, PublishingConfiguration publishingConfiguration)
	{
		ExpressionList expressionList = Ebean.find(PublishingSharedData.class).where();

		if (publishingConfiguration != null) {
			expressionList.eq("publishingConfiguration", publishingConfiguration);
		}

		expressionList.setFirstRow(new Long(pageable.getOffset()).intValue());
		expressionList.setMaxRows(pageable.getPageSize());

		if (!pageable.getSort().isUnsorted()) {
			Iterator<Sort.Order> orderIterator = pageable.getSort().iterator();
			while (orderIterator.hasNext()) {
				Sort.Order order = orderIterator.next();

				if (!order.getProperty().equals("UNSORTED")) {
					if (order.isAscending()) {
						expressionList.order().asc(order.getProperty());
					}

					if (order.isDescending()) {
						expressionList.order().desc(order.getProperty());
					}
				}
			}
		}

		Page<PublishingSharedData> page = new PageImpl<PublishingSharedData>(expressionList.findList(), pageable, expressionList.findCount());

		return page;
	}
}
 


