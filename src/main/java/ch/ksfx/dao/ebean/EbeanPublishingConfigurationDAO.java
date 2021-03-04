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

package ch.ksfx.dao.ebean;

import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.model.publishing.PublishingCategory;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.model.note.NotePublishingConfiguration;
import ch.ksfx.model.publishing.PublishingConfigurationCacheData;
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
public class EbeanPublishingConfigurationDAO implements PublishingConfigurationDAO
{
    @Override
    public PublishingConfiguration getPublishingConfigurationForId(Long publishingConfigurationId)
    {
        return Ebean.find(PublishingConfiguration.class, publishingConfigurationId);
    }

    @Override
    public List<PublishingConfiguration> getAllPublishingConfigurations()
    {
        return Ebean.find(PublishingConfiguration.class).findList();
    }

    @Override
    public List<PublishingConfiguration> getPublishingConfigurationsForPublishingCategory(PublishingCategory publishingCategory)
    {
        if (publishingCategory == null) {
            return Ebean.find(PublishingConfiguration.class).findList();
        } else {
            return Ebean.find(PublishingConfiguration.class).where().eq("publishingCategory",publishingCategory).findList();
        }
    }

    public Page<PublishingConfiguration> getPublishingConfigutationsForPageableAndPublishingCategory(Pageable pageable, PublishingCategory publishingCategory)
    {
        ExpressionList expressionList = Ebean.find(PublishingConfiguration.class).where();

        if (publishingCategory != null) {
            expressionList.eq("publishingCategory", publishingCategory);
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

        Page<PublishingConfiguration> page = new PageImpl<PublishingConfiguration>(expressionList.findList(), pageable, Ebean.find(PublishingConfiguration.class).findCount());

        return page;
    }

    @Override
    public void saveOrUpdatePublishingConfiguration(PublishingConfiguration publishingConfiguration)
    {
        if (publishingConfiguration.getId() != null) {
            Ebean.update(publishingConfiguration);
        } else {
            Ebean.save(publishingConfiguration);
        }
    }

    @Override
    public void deletePublishingConfiguration(PublishingConfiguration publishingConfiguration)
    {
        List<NotePublishingConfiguration> notePublishingConfigurations = Ebean.find(NotePublishingConfiguration.class).where().eq("publishingConfiguration",publishingConfiguration).findList();
        
        for (NotePublishingConfiguration notePublishingConfiguration : notePublishingConfigurations) {
            Ebean.delete(notePublishingConfiguration);   
        }
        
        Ebean.delete(publishingConfiguration);
    }

    @Override
    public PublishingCategory getPublishingCategoryForId(Long publishingCategoryId)
    {
        return Ebean.find(PublishingCategory.class, publishingCategoryId);
    }

    @Override
    public void saveOrUpdatePublishingCategory(PublishingCategory publishingCategory)
    {
        if (publishingCategory.getId() != null) {
            Ebean.update(publishingCategory);
        } else {
            Ebean.save(publishingCategory);
        }
    }

    @Override
    public List<PublishingCategory> getAllPublishingCategories()
    {
        return Ebean.find(PublishingCategory.class).findList();
    }

    @Override
    public void deletePublishingCategory(PublishingCategory publishingCategory)
    {
        Ebean.delete(publishingCategory);
    }
    
    @Override
    public void appendConsole(Long publishingConfigurationId, String dataToAppend)
    {
    	PublishingConfiguration publishingConfiguration = getPublishingConfigurationForId(publishingConfigurationId);
    	publishingConfiguration.setConsole(((publishingConfiguration.getConsole() == null)?"":publishingConfiguration.getConsole()) + dataToAppend);
    	saveOrUpdatePublishingConfiguration(publishingConfiguration);
    	
		//SqlUpdate update = Ebean.createSqlUpdate("UPDATE publishing_configuration SET console = CONCAT(console, :data) WHERE id = :id");
        //update.setParameter("data", dataToAppend);
        //update.setParameter("id", publishingConfigurationId);
       	//update.execute();	
    }
    
    @Override
    public PublishingConfiguration getPublishingConfigurationForUri(String uri)
    {
		return Ebean.find(PublishingConfiguration.class).where().eq("uri", uri).findUnique();
    }
    
    	
	@Override
	public void saveOrUpdatePublishingConfigurationCacheData(PublishingConfigurationCacheData publishingConfigurationCacheData)
	{
		if (publishingConfigurationCacheData.getId() != null) {
			Ebean.update(publishingConfigurationCacheData);
		} else {
			Ebean.save(publishingConfigurationCacheData);
		}
	}
	
	@Override
	public PublishingConfigurationCacheData getPublishingConfigurationCacheDataForPublishingConfigurationAndUriParameter(PublishingConfiguration publishingConfiguration, String uriParameter)
	{
		return Ebean.find(PublishingConfigurationCacheData.class).where().eq("publishingConfiguration", publishingConfiguration).eq("uriParameter", uriParameter).findUnique();
	}
	
	@Override
	public void deletePublishingConfigurationCacheData(PublishingConfigurationCacheData publishingConfigurationCacheData)
	{
		Ebean.delete(publishingConfigurationCacheData);
	}
}
