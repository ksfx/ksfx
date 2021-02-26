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
import ch.ksfx.dao.spidering.SpideringConfigurationDAO;
import ch.ksfx.model.spidering.ResourceConfiguration;
import ch.ksfx.model.spidering.SpideringConfiguration;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Repository;

import java.util.Iterator;
import java.util.List;

@Repository
public class EbeanSpideringConfigurationDAO implements SpideringConfigurationDAO
{
    @Override
    public void saveOrUpdate(SpideringConfiguration spideringConfiguration)
    {
        if (spideringConfiguration.getId() != null) {
            Ebean.update(spideringConfiguration);
        } else {
            Ebean.save(spideringConfiguration);
        }
    }

    @Override
    public List<SpideringConfiguration> getAllSpideringConfigurations()
    {
        return Ebean.find(SpideringConfiguration.class).findList();
    }

    @Override
    public Page<SpideringConfiguration> getSpideringConfigutationsForPageable(Pageable pageable)
    {
        ExpressionList expressionList = Ebean.find(SpideringConfiguration.class).where();
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

        Page<SpideringConfiguration> page = new PageImpl<SpideringConfiguration>(expressionList.findList(), pageable, Ebean.find(SpideringConfiguration.class).findCount());

        return page;
    }

    @Override
    public List<SpideringConfiguration> getScheduledSpideringConfigurations()
    {
        return Ebean.find(SpideringConfiguration.class).where().eq("cronScheduleEnabled",true).findList();
    }

    @Override
    public SpideringConfiguration getSpideringConfigurationForId(Long spideringConfigurationId)
    {
        return Ebean.find(SpideringConfiguration.class, spideringConfigurationId);
    }

    @Override
    public void deleteSpideringConfiguration(SpideringConfiguration spideringConfiguration)
    {
        ResourceConfigurationDAO resourceConfigurationDAO = new EbeanResourceConfigurationDAO();

        if (spideringConfiguration.getResourceConfigurations() != null) {
            for (ResourceConfiguration resourceConfiguration : spideringConfiguration.getResourceConfigurations()) {
                resourceConfigurationDAO.deleteResourceConfiguration(resourceConfiguration);
            }
        }

        Ebean.delete(spideringConfiguration);
    }
}
