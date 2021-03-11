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

import ch.ksfx.dao.spidering.SpideringDAO;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.spidering.Resource;
import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.model.spidering.SpideringConfiguration;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import io.ebean.SqlUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Repository
public class EbeanSpideringDAO implements SpideringDAO
{
    @Override
    public Spidering getSpideringForId(Long spideringId)
    {
        return Ebean.find(Spidering.class, spideringId);
    }

    @Override
    public void saveOrUpdate(Spidering spidering)
    {
        if (spidering.getId() != null) {
            Ebean.update(spidering);
        } else {
            Ebean.save(spidering);
        }
    }

    @Override
    public void delete(Spidering spidering)
    {
        //TODO on delete cascade result unit deletion.
        SqlUpdate update = Ebean.createSqlUpdate("DELETE FROM resource WHERE spidering = :spidering");
        update.setParameter("spidering", spidering.getId());
        update.execute();

        SqlUpdate update2 = Ebean.createSqlUpdate("UPDATE result SET spidering = NULL WHERE spidering = :spidering");
        update2.setParameter("spidering", spidering.getId());
        update2.execute();

        Ebean.delete(spidering);
    }

    @Override
    public List<Spidering> getAllSpiderings()
    {
        return Ebean.find(Spidering.class).findList();
    }

    @Override
    public List<Spidering> getSpideringsOlderThan(Date date)
    {
        return Ebean.find(Spidering.class).where().le("finished",date).findList();
    }
    
    @Override
    public List<Spidering> getSpideringsOlderThanForSpideringConfiguration(Date date, SpideringConfiguration spideringConfiguration)
    {
    	return Ebean.find(Spidering.class).where().le("finished",date).eq("spideringConfiguration.id", spideringConfiguration.getId()).findList();	
    }

    /*
    @Override
    public GridDataSource getSpideringGridDataSourceForSpideringConfiguration(SpideringConfiguration spideringConfiguration)
    {
        ExpressionList expressionList = Ebean.find(Spidering.class).where().eq("spideringConfiguration.id", spideringConfiguration.getId());

        return new EbeanGridDataSource(expressionList, Spidering.class);
    }
    */

    @Override
    public List<Spidering> getSpideringsForSpideringConfiguration(SpideringConfiguration spideringConfiguration)
    {
        return Ebean.find(Spidering.class).where().eq("spideringConfiguration.id", spideringConfiguration.getId()).findList();
    }

    public Page<Spidering> getSpideringsForPageableAndSpideringConfiguration(Pageable pageable, SpideringConfiguration spideringConfiguration)
    {
        ExpressionList expressionList = Ebean.find(Spidering.class).where();

        if (spideringConfiguration != null) {
            expressionList.eq("spideringConfiguration", spideringConfiguration);
        }

        expressionList.setFirstRow(new Long(pageable.getOffset()).intValue());
        expressionList.setMaxRows(pageable.getPageSize());

        boolean hasOrder = false;

        if (!pageable.getSort().isUnsorted()) {
            Iterator<Sort.Order> orderIterator = pageable.getSort().iterator();
            while (orderIterator.hasNext()) {
                Sort.Order order = orderIterator.next();

                if (!order.getProperty().equals("UNSORTED")) {
                    if (order.isAscending()) {
                        expressionList.order().asc(order.getProperty());
                        hasOrder = true;
                    }

                    if (order.isDescending()) {
                        expressionList.order().desc(order.getProperty());
                        hasOrder = true;
                    }
                }
            }
        }

        if (hasOrder == false) {
            expressionList.order().desc("id");
        }

        Page<Spidering> page = new PageImpl<Spidering>(expressionList.findList(), pageable, expressionList.findCount());

        return page;
    }

    @Override
    public Integer calculateResourcesCount(Spidering spidering)
    {
        return Ebean.find(Resource.class).where().eq("spidering.id", spidering.getId()).findCount();
    }
}
