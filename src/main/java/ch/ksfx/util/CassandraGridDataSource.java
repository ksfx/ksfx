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

import ch.ksfx.model.Observation;
import ch.ksfx.model.TimeSeries;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Ordering;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select.Where;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.grid.SortConstraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class CassandraGridDataSource implements GridDataSource
{
    private List preparedResults;
    private int startIndex;
    private Class clazz;
    private TimeSeries timeSeries;
    private Session session;


    public CassandraGridDataSource(TimeSeries timeSeries)
    {
        this.timeSeries = timeSeries;

        Cluster cluster = Cluster.builder().addContactPoint("localhost").build();

        session = cluster.connect("observation_store");
    }

    @Override
    public int getAvailableRows()
    {
    	if (timeSeries.getApproximateSize() != null) {
    		return timeSeries.getApproximateSize();
    	}
    	
    	return 100000; 
    }

    @Override
    public void prepare(int startIndex, int endIndex, List<SortConstraint> sortConstraints)
    {

        Integer page = startIndex / 25;

        System.out.println("Page: " + page);

        preparedResults = new ArrayList<Observation>();

        Long time = System.currentTimeMillis();

		Ordering ordering = null; 
		
		for (SortConstraint constraint : sortConstraints)
        {
            String propertyName = constraint.getPropertyModel().getPropertyName().replaceAll("([^_A-Z])([A-Z])", "$1_$2"); //camel case to underscore...

            switch (constraint.getColumnSort())
            {
                case ASCENDING:
                    ordering = QueryBuilder.asc(propertyName);
                    break;

                case DESCENDING:
                    ordering = QueryBuilder.desc(propertyName);
                    break;

                default:
            }
        }

        Where where = QueryBuilder.select().from("observation_store", "observation").where(QueryBuilder.eq("time_series_id", timeSeries.getId()));
        
        if (ordering != null) {
        	where.orderBy(ordering);	
        }
        
        where.setFetchSize(25);

        ResultSet results = session.execute(where);

        System.out.println("Query took " + ((System.currentTimeMillis() - time) / 1000));

        Iterator<Row> iterator = results.iterator();

        for (Integer i = 0; i < startIndex; i++) {
            iterator.next();

            if (i != 0 && (i % 25) == 0 && !results.isFullyFetched()) {
                results.fetchMoreResults();

                page--;
            }

            if (page == 0) {
                break;
            }
        }

        System.out.println("Start Index: " + startIndex + " End Index: " + endIndex);

        Integer size = 0;

        while (iterator.hasNext() && size < 25) {
            Row row = iterator.next();

            Observation o = new Observation();
            o.setTimeSeriesId(row.getInt("time_series_id"));
            o.setObservationTime(row.getTimestamp("observation_time"));
            o.setSourceId(row.getString("source_id"));
            o.setScalarValue(row.getString("scalar_value"));
            o.setComplexValue(row.getMap("complex_value", String.class, String.class));
            o.setMetaData(row.getMap("meta_data", String.class, String.class));
            preparedResults.add(o);

            size++;
        }
        
        while (preparedResults.size() < 25) {
        	preparedResults.add(new Observation());
        }

        this.startIndex = startIndex;
    }

    @Override
    public Object getRowValue(int index)
    {
        return preparedResults.get(index - startIndex);
    }

    @Override
    public Class getRowType()
    {
        return Observation.class;
    }
}
