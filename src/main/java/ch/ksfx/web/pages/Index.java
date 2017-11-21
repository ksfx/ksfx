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

package ch.ksfx.web.pages;

import ch.ksfx.dao.GenericDataStoreDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.GenericDataStore;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.model.user.User;
import ch.ksfx.web.services.lucene.IndexService;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import java.util.List;


@Secured({"ROLE_ADMIN"})
public class Index
{
    @Inject
    private TimeSeriesDAO timeSeriesDAO;

	@Inject
	private GenericDataStoreDAO genericDataStoreDAO;

    @Inject
    private IndexService indexService;

    @Property
    private TimeSeries timeSeries;
    
    private Long megaByte = 1024l * 1024l;

    public void onActivate()
    {
    }

    public List<TimeSeries> getAllTimeSeries()
    {
        return timeSeriesDAO.getAllTimeSeries();
    }

    public void onActionFromPerformGc()
    {
        System.gc();
    }
    
    public String getGenericDataStoreInformation(String key)
    {
    	GenericDataStore genericDataStore = genericDataStoreDAO.getGenericDataStoreForKey(key);
    	
    	if (genericDataStore == null) {
    		return "";
    	}
    	
    	return genericDataStore.getDataValue();
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

    public Integer getIndexQueueSize()
    {
        return indexService.getCurrentQueueSize();
    }

    public Long getFirstTimeSeriesId()
    {
        TimeSeries ts = timeSeriesDAO.getFirstTimeSeriesInDatabase();

        if (ts != null) {
            return ts.getId();
        }

        return null;
    }
}
