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

package ch.ksfx.services.lucene;

import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.services.configurationdatabase.LiquibaseProvider;
import ch.ksfx.services.systemlogger.SystemLogger;
import ch.ksfx.services.SystemEnvironment;
import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IndexService
{
	private SystemEnvironment systemEnvironment;
	private ObservationDAO observationDAO;
	private TimeSeriesDAO timeSeriesDAO;
	private SystemLogger systemLogger;
	private List<Integer> indexableTimeSeriesIds;
	
	private AsynchronousIndexer asynchronousIndexer;
    private Logger logger = LoggerFactory.getLogger(IndexService.class);


	public IndexService(SpringLiquibase springLiquibase, SystemEnvironment systemEnvironment, @Lazy ObservationDAO observationDAO, SystemLogger systemLogger, TimeSeriesDAO timeSeriesDAO)
	{
        this.systemEnvironment = systemEnvironment;
        this.observationDAO = observationDAO;
        this.systemLogger = systemLogger;
        this.timeSeriesDAO = timeSeriesDAO;

		refreshIndexableTimeSeriesIds();

		asynchronousIndexer = new AsynchronousIndexer(observationDAO, systemEnvironment, systemLogger);
		asynchronousIndexer.start();
	}
	
	public void refreshIndexableTimeSeriesIds()
	{
		indexableTimeSeriesIds = new ArrayList<Integer>();
		
		for (TimeSeries ts : timeSeriesDAO.getIndexableTimeSeries()) {
			if (ts.getIndexable()) {
				indexableTimeSeriesIds.add(ts.getId().intValue());
			}
		}
	}
	
	public void index(IndexEvent indexEvent)
	{
        try {
        	if (indexableTimeSeriesIds.contains(indexEvent.getSeriesId())) {
            	asynchronousIndexer.getQueuedIndexEvents().add(indexEvent);
        	}
        } catch (Exception e) {
            logger.error("Error in IndexService", e);
        }			
	}

    public Integer getCurrentQueueSize()
    {
        return asynchronousIndexer.getQueuedIndexEvents().size();
    }
}