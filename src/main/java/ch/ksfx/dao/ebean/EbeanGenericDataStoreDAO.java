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

import ch.ksfx.dao.GenericDataStoreDAO;
import ch.ksfx.model.GenericDataStore;
import ch.ksfx.model.activity.ActivityInstance;
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
public class EbeanGenericDataStoreDAO implements GenericDataStoreDAO
{
    @Override
    public List<GenericDataStore> getAllGenericDataStores()
    {
        return Ebean.find(GenericDataStore.class).findList();
    }

    @Override
    public Page<GenericDataStore> getGenericDataStoresForPageable(Pageable pageable)
    {
        ExpressionList expressionList = Ebean.find(GenericDataStore.class).where();

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

        Page<GenericDataStore> page = new PageImpl<GenericDataStore>(expressionList.findList(), pageable, expressionList.findCount());

        return page;
    }

    @Override
    public void saveOrUpdateGenericDataStore(GenericDataStore genericDataStore)
    {
        if (genericDataStore.getId() != null) {
            Ebean.update(genericDataStore);
        } else {
            Ebean.save(genericDataStore);
        }
    }

    @Override
    public void deleteGenericDataStore(GenericDataStore genericDataStore)
    {
        Ebean.delete(genericDataStore);
    }

    @Override
    public GenericDataStore getGenericDataStoreForId(Long genericDataStoreId)
    {
        return Ebean.find(GenericDataStore.class, genericDataStoreId);
    }

    @Override
    public GenericDataStore getGenericDataStoreForKey(String dataKey)
    {
        return Ebean.find(GenericDataStore.class).where().eq("dataKey",dataKey).findUnique();
    }
}
