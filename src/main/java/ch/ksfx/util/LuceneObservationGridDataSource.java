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

package ch.ksfx.util;

import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.model.Observation;
import ch.ksfx.web.services.lucene.ObservationSearch;
import ch.ksfx.web.services.systemenvironment.SystemEnvironment;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.grid.SortConstraint;

import java.util.Date;
import java.util.List;
import java.util.Map;


public class LuceneObservationGridDataSource implements GridDataSource
{
    private int startIndex;
    private List preparedResults;
    
    private ObservationSearch observationSearch;

    public LuceneObservationGridDataSource(SystemEnvironment systemEnvironment, ObservationDAO observationDAO, String allQuery, String scalarValueQuery, Map<String, String> complexValueQuery, Map<String, String> metaDataQuery, Date dateFrom, Date dateTo, String seriesId)
    {
		observationSearch = new ObservationSearch(systemEnvironment, observationDAO);
		observationSearch.prepare(allQuery, scalarValueQuery, complexValueQuery, metaDataQuery, dateFrom, dateTo, seriesId);
    }

    @Override
    public int getAvailableRows()
    {
        System.out.println("-=-=-= Total hits " + observationSearch.getTotalHits());
        return observationSearch.getTotalHits();
    }

    @Override
    public void prepare(int startIndex, int endIndex, List<SortConstraint> sortConstraints)
    {
        this.startIndex = startIndex;
    	preparedResults = observationSearch.getObservations(startIndex, endIndex);
    	
    	//http://stackoverflow.com/questions/963781/how-to-achieve-pagination-in-lucene
    	


/*
        for (SortConstraint constraint : sortConstraints)
        {

            String propertyName = constraint.getPropertyModel().getPropertyName();

            switch (constraint.getColumnSort())
            {

                case ASCENDING:

                    expressionList.order().asc(propertyName);
                    break;

                case DESCENDING:
                    expressionList.order().desc(propertyName);
                    break;

                default:
            }
        }
*/
    }

    @Override
    public Object getRowValue(int index)
    {
        System.out.println("Resultsize: " + preparedResults.size() + " Index: " + index + "Startindex: " + startIndex);
        return preparedResults.get(index - (startIndex));
    }

    @Override
    public Class getRowType()
    {
        return Observation.class;
    }
}
