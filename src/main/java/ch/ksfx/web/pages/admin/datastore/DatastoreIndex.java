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

package ch.ksfx.web.pages.admin.datastore;

import ch.ksfx.dao.GenericDataStoreDAO;
import ch.ksfx.model.GenericDataStore;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


@Secured({"ROLE_ADMIN"})
public class DatastoreIndex
{
    @Property
    private GenericDataStore genericDataStore;

    @Property
    private String nonPersisentStoreKey;

    @Property
    @Persist
    private boolean showNonPersistentData;

    @Inject
    private GenericDataStoreDAO genericDataStoreDAO;

    public List<GenericDataStore> getAllPersistentGenericDataStoreDatas()
    {
        return genericDataStoreDAO.getAllGenericDataStores();
    }

    public void onActionFromDeletePersistentGenericDataStore(Long genericDataStoreId)
    {
        GenericDataStore genericDataStore = genericDataStoreDAO.getGenericDataStoreForId(genericDataStoreId);
        genericDataStoreDAO.deleteGenericDataStore(genericDataStore);
    }

    public List<String> getAllNonPersistentGenericDataStoreDatas()
    {
        return new ArrayList<String>(GenericDataStore.nonPersistentStore.keySet());
    }

    public void onActionFromDeleteNonPersistentGenericDataStore(String nonPeristentStoreKey)
    {
        GenericDataStore.nonPersistentStore.remove(nonPeristentStoreKey);
    }

    public String getNonPersistentContent(String nonPersisentStoreKey)
    {
        if (GenericDataStore.nonPersistentStore.get(nonPersisentStoreKey) == null) {
            return "";
        }

        return GenericDataStore.nonPersistentStore.get(nonPersisentStoreKey).toString();
    }

    public void onActionFromShowPersistentData()
    {
        showNonPersistentData = false;
    }

    public void onActionFromShowNonPersistentData()
    {
        showNonPersistentData = true;
    }

    public void onActionFromClearPersistentData()
    {
        for (GenericDataStore gds : genericDataStoreDAO.getAllGenericDataStores()) {
            genericDataStoreDAO.deleteGenericDataStore(gds);
        }
    }

    public void onActionFromClearNonPersistentData()
    {
        GenericDataStore.nonPersistentStore = new HashMap<String, Object>();
    }

    public String getPersistentTabClass()
    {
        if (!showNonPersistentData) {
            return "active";
        }

        return "";
    }

    public String getNonPersistentTabClass()
    {
        if (showNonPersistentData) {
            return "active";
        }

        return "";
    }
}
