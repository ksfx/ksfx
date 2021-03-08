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

import ch.ksfx.dao.spidering.ResourceDAO;
import ch.ksfx.model.spidering.Resource;
import ch.ksfx.model.spidering.Result;
import ch.ksfx.model.spidering.Spidering;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import io.ebean.QueryIterator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Repository
public class EbeanResourceDAO implements ResourceDAO
{
	private Long megaByte = 1024l * 1024l;
	
    @Override
    public void saveOrUpdate(Resource resource)
    {
        if (resource.getId() != null) {
            Ebean.update(resource);
        } else {
            Ebean.save(resource);
        }
    }
    
    public Long getFreeMemory()
    {
        Runtime runtime = Runtime.getRuntime();

        return (runtime.freeMemory() / megaByte);
    }

    public Long getUsedMemory()
    {
        Runtime runtime = Runtime.getRuntime();

        return ((runtime.totalMemory() - runtime.freeMemory()) / megaByte);
    }

    public Long getTotalMemory()
    {
        Runtime runtime = Runtime.getRuntime();

        return (runtime.totalMemory() / megaByte);
    }

    public Long getMaxMemory()
    {
        Runtime runtime = Runtime.getRuntime();

        return (runtime.maxMemory() / megaByte);
    }

    @Override
    public List<Resource> getAllResources()
    {
        return Ebean.find(Resource.class).findList();
    }

    @Override
    public List<Resource> getResourcesForSpideringAndDepth(Spidering spidering, Integer depth)
    {
        return Ebean.find(Resource.class).where().eq("spidering.id", spidering.getId()).eq("depth", depth).findList();
    }

    @Override
    public QueryIterator<Resource> getResourcesForSpideringAndDepthIterator(Spidering spidering, Integer depth)
    {
        return Ebean.find(Resource.class).where().eq("spidering.id", spidering.getId()).eq("depth", depth).findIterate();
    }

    @Override
    public List getResourcesForSpideringAndDepthIds(Spidering spidering, Integer depth)
    {
        return Ebean.find(Resource.class).where().eq("spidering.id", spidering.getId()).eq("depth", depth).findIds();
    }

    @Override
    public List<Resource> getChildResources(Resource resource)
    {
        List<Resource> allResults = new ArrayList<Resource>();
        List<Resource> results = null;

        List<Long> relevantIds = new ArrayList<Long>();
        relevantIds.add(resource.getId());

        do {
            results = Ebean.find(Resource.class).where().in("previousResource.id", relevantIds).findList();

            if (results != null && !results.isEmpty()) {
                allResults.addAll(results);
            }

            relevantIds.clear();

            for (Resource r : results) {
                relevantIds.add(r.getId());
            }
        } while (!relevantIds.isEmpty());

        return allResults;
    }

    @Override
    public Page<Resource> getResourcesForPageableAndSpidering(Pageable pageable, Spidering spidering)
    {
        ExpressionList expressionList = Ebean.find(Resource.class).where();

        if (spidering != null) {
            expressionList.eq("spidering", spidering);
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

        Page<Resource> page = new PageImpl<Resource>(expressionList.findList(), pageable, expressionList.findCount());

        return page;
    }

    /*
    @Override
    public GridDataSource getResourceGridDataSourceForSpidering(Spidering spidering)
    {
        ExpressionList expressionList = Ebean.find(Resource.class).where().eq("spidering.id", spidering.getId());

        return new EbeanGridDataSource(expressionList, Resource.class);
    }
    */

    @Override
    public Resource getResourceForId(Long resourceId)
    {
        return Ebean.find(Resource.class, resourceId);
    }

    @Override
    public void deleteResource(Resource resource)
    {
        Ebean.delete(resource);
    }
}
