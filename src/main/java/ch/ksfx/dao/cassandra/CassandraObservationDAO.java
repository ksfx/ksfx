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

package ch.ksfx.dao.cassandra;

import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.Observation;
import ch.ksfx.model.TimeSeries;
//import ch.ksfx.web.services.lucene.DeleteSeriesObservationsEvent;
//import ch.ksfx.web.services.lucene.IndexEvent;
//import ch.ksfx.web.services.lucene.IndexService;
//import ch.ksfx.web.services.systemenvironment.SystemEnvironment;

//import com.datastax.driver.core.*;
//import com.datastax.driver.core.querybuilder.Ordering;
//import com.datastax.driver.core.querybuilder.QueryBuilder;
//import com.datastax.driver.core.querybuilder.Select;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.core.metadata.schema.ClusteringOrder;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.*;

@Repository
public class CassandraObservationDAO implements ObservationDAO
{
//    private SystemEnvironment systemEnvironment;
//    private IndexService indexService;

//    CqlSession simpleCluster = CqlSession.builder().addContactPoint("localhost").withSocketOptions(new SocketOptions().setConnectTimeoutMillis(20000).setReadTimeoutMillis(30000)).build();
//    CqlSession simpleSession = simpleCluster.connect("observation_store");

    CqlSession simpleSession = CqlSession.builder()
            .withConfigLoader(DriverConfigLoader.programmaticBuilder()
                    .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofMillis(100000))
                    .withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, Duration.ofMillis(100000))
                    .build())
            .withKeyspace("observation_store")
            .build();

    PreparedStatement insertObservationStatement = null;

    private TimeSeriesDAO timeSeriesDAO;

    public CassandraObservationDAO(TimeSeriesDAO timeSeriesDAO/*SystemEnvironment systemEnvironment, IndexService indexService*/)
    {
//		this.systemEnvironment = systemEnvironment;
//		this.indexService = indexService;
        this.timeSeriesDAO = timeSeriesDAO;

        insertObservationStatement = simpleSession.prepare("INSERT INTO observation (time_series_id, observation_time, source_id, scalar_value, complex_value, meta_data) VALUES (?,?,?,?,?,?);");
    }

    @Override
    public void saveObservation(Observation observation)
    {
        try {
            BoundStatement bs = insertObservationStatement.bind(observation.getTimeSeriesId(), observation.getObservationTime(), observation.getSourceId(), observation.getScalarValue(), observation.getComplexValue(), observation.getMetaData());
            ResultSet rs = simpleSession.execute(bs);
        
 //       	indexService.index(new IndexEvent(observation.getTimeSeriesId(), observation.getObservationTime(), observation.getSourceId(), false));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
        }
    }

    @Override
    public Observation getFirstObservationForTimeSeriesId(Integer timeSeriesId)
    {
        try {
            Select select = QueryBuilder.selectFrom("observation_store", "observation").all().whereColumn("time_series_id").isEqualTo(QueryBuilder.bindMarker());
            select = select.orderBy("observation_time", ClusteringOrder.ASC);
            select = select.limit(1);

            PreparedStatement statement = simpleSession.prepare(select.build());
            ResultSet results = simpleSession.execute(statement.bind(timeSeriesId));

            List<Row> rows = results.all();

            if (rows != null && rows.size() > 0) {
                Observation o = new Observation();
                o.setTimeSeriesId(rows.get(0).getInt("time_series_id"));
                //o.setObservationTime(new Date(rows.get(0).getDate("observation_time").getMillisSinceEpoch()));
                o.setObservationTime(Date.from(rows.get(0).getInstant("observation_time")));
                o.setSourceId(rows.get(0).getString("source_id"));
                o.setScalarValue(rows.get(0).getString("scalar_value"));
                o.setComplexValue(rows.get(0).getMap("complex_value", String.class, String.class));
                o.setMetaData(rows.get(0).getMap("meta_data", String.class, String.class));

                return o;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
        }

        return null;
    }

    /*
    @Override
    public Observation getLastObservationForTimeSeriesId(Integer timeSeriesId)
    {
        try {
            Select select = QueryBuilder.selectFrom("observation_store", "observation").all().whereColumn("time_series_id").isEqualTo(QueryBuilder.bindMarker()).limit(1);
            PreparedStatement statement = simpleSession.prepare(select.build());

            ResultSet resultSet = simpleSession.execute(statement.bind(timeSeriesId));

            List<Row> rows = resultSet.all();

            if (rows != null && rows.size() > 0) {
                Observation o = new Observation();
                o.setTimeSeriesId(rows.get(0).getInt("time_series_id"));
                //o.setObservationTime(new Date(rows.get(0).getDate("observation_time").getMillisSinceEpoch()));
                o.setObservationTime(Date.from(rows.get(0).getInstant("observation_time")));
                o.setSourceId(rows.get(0).getString("source_id"));
                o.setScalarValue(rows.get(0).getString("scalar_value"));
                o.setComplexValue(rows.get(0).getMap("complex_value", String.class, String.class));
                o.setMetaData(rows.get(0).getMap("meta_data", String.class, String.class));

                return o;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
        }

        return null;
    }
     */

    @Override
    public Observation getObservationForTimeSeriesIdObservationTimeAndSourceId(Integer timeSeriesId, Date observationTime, String sourceId)
    {
        try {

            System.out.println("Time Series ID -> " + timeSeriesId);
            System.out.println("Source ID -> " + sourceId);
            System.out.println("Observation Time -> " + observationTime);

            Select select = QueryBuilder.selectFrom("observation_store", "observation").all().whereColumn("time_series_id").isEqualTo(QueryBuilder.bindMarker()).whereColumn("source_id").isEqualTo(QueryBuilder.bindMarker()).whereColumn("observation_time").isEqualTo(QueryBuilder.bindMarker());
            PreparedStatement statement = simpleSession.prepare(select.build());
            ResultSet results = simpleSession.execute(statement.bind(timeSeriesId,sourceId,observationTime.toInstant()));

            List<Row> rows = results.all();

            //TODO Exception when more than one result!?
            if (rows.size() > 0) {
                Observation o = new Observation();
                o.setTimeSeriesId(rows.get(0).getInt("time_series_id"));
                //o.setObservationTime(new Date(rows.get(0).getDate("observation_time").getMillisSinceEpoch()));
                o.setObservationTime(Date.from(rows.get(0).getInstant("observation_time")));
                o.setSourceId(rows.get(0).getString("source_id"));
                o.setScalarValue(rows.get(0).getString("scalar_value"));
                o.setComplexValue(new HashMap<String, String>(rows.get(0).getMap("complex_value", String.class, String.class)));
                o.setMetaData(new HashMap<String, String>(rows.get(0).getMap("meta_data", String.class, String.class)));

                return o;
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
        }

        return null;
    }

    /*
    public List<Observation> getObservations() 
    {
        try {
            List<Observation> observations = new ArrayList<Observation>();

            Statement select = QueryBuilder.select().all().from("observation_store", "observation");

            ResultSet results = simpleSession.execute(select);

            for (Row row : results) {
                Observation o = new Observation();
                o.setTimeSeriesId(row.getInt("time_series_id"));
                //o.setObservationTime(new Date(row.getDate("observation_time").getMillisSinceEpoch()));
                o.setObservationTime(row.getTimestamp("observation_time"));
                o.setSourceId(row.getString("source_id"));
                o.setScalarValue(row.getString("scalar_value"));
                o.setComplexValue(row.getMap("complex_value", String.class, String.class));
                o.setMetaData(row.getMap("meta_data", String.class, String.class));
                observations.add(o);
            }

            return observations;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
        }
    }
     */


    public List<Observation> queryObservations(Integer timeSeriesId, Date startDate, Date endDate)
    {
        return queryObservations(timeSeriesId, startDate, endDate, null);
    }

    public List<Observation> queryObservations(Integer timeSeriesId, Date startDate, Date endDate, Integer limit)
    {
        try {
            List<Observation> observations = new ArrayList<Observation>();

            List<Object> boundValues = new ArrayList<Object>();

            Select select = QueryBuilder.selectFrom("observation_store", "observation").all().whereColumn("time_series_id").isEqualTo(QueryBuilder.bindMarker());
            boundValues.add(timeSeriesId);

            //Select.Where select = QueryBuilder.select().all().from("observation_store", "observation").where(QueryBuilder.eq("time_series_id", timeSeriesId));

            if (startDate != null) {
                select = select.whereColumn("observation_time").isGreaterThanOrEqualTo(QueryBuilder.bindMarker());
                boundValues.add(startDate.toInstant());
//                select.and(QueryBuilder.gte("observation_time",startDate));
            }

            if (endDate != null) {
                select = select.whereColumn("observation_time").isLessThanOrEqualTo(QueryBuilder.bindMarker());
                boundValues.add(endDate.toInstant());
//                select.and(QueryBuilder.lte("observation_time", endDate));
            }
            
            if (limit != null) {
            	select.limit(limit);
            }

            PreparedStatement statement = simpleSession.prepare(select.build());
            ResultSet results = simpleSession.execute(statement.bind(boundValues.toArray()));

            for (Row row : results) {

                Observation o = new Observation();
                o.setTimeSeriesId(row.getInt("time_series_id"));
                //o.setObservationTime(new Date(row.getDate("observation_time").getMillisSinceEpoch()));
                o.setObservationTime(Date.from(row.getInstant("observation_time")));
                o.setSourceId(row.getString("source_id"));
                o.setScalarValue(row.getString("scalar_value"));
                o.setComplexValue(row.getMap("complex_value", String.class, String.class));
                o.setMetaData(row.getMap("meta_data", String.class, String.class));
                observations.add(o);
            }

            return observations;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
        }
    }

    /*
    public List<Observation> queryObservationsSparse(Integer timeSeriesId, Date startDate, Date endDate)
    {
        try {
        	
        	if (startDate.getTime() == 0l) {
        		System.out.println("[GRAPH] ==> Chart for whole series requested (" + timeSeriesId + ")");
        		Observation o = getFirstObservationForTimeSeriesId(timeSeriesId);
        		
        		if (o != null) {
        			startDate = o.getObservationTime();
        			System.out.println("[GRAPH] ==> Fixed startdate to " + startDate + "(" + timeSeriesId + ")");
        		}
        	}
        	
        	System.out.println("[GRAPH] ==> Calculating chart from: " + startDate + " to enddate " + endDate + "(" + timeSeriesId + ")");
        	
            List<Observation> observations = new ArrayList<Observation>();
            
            Integer obsPerPeriod = null;
            Integer stepWidth = 1;

            // > 30 Days
        	if ((endDate.getTime() - startDate.getTime()) >= 2592000000l) {
        		obsPerPeriod = 1;
        		stepWidth = 5;
        		
        		System.out.println("[GRAPH] ==> Using Autosparsing " + "(" + timeSeriesId + ")");
        	}

            // > 10 Years
            if ((endDate.getTime() - startDate.getTime()) >= 307584000000l) {
                obsPerPeriod = 1;
                stepWidth = 180;

                System.out.println("[GRAPH] ==> Using Autosparsing "+ "(" + timeSeriesId + ")");
            }

            // > 100 Years
            if ((endDate.getTime() - startDate.getTime()) >= 3075840000000l) {
                obsPerPeriod = 1;
                stepWidth = 365;

                System.out.println("[GRAPH] ==> Using Autosparsing " + "(" + timeSeriesId + ")");
            }

        	Date tmpStart = startDate;
        	Date tmpEnd = DateUtils.addDays(startDate, stepWidth);
        	
        	if (tmpEnd.getTime() > endDate.getTime()) {
        		tmpEnd = endDate;
        	}
        	
        	Integer icnt = 0;
        	
        	while(tmpEnd.getTime() <= endDate.getTime()) {
                System.out.println("[GRAPH] Querying observations with: " + tmpStart + " - " + tmpEnd + "(" + timeSeriesId + ")");
                List<Observation> obs = queryObservations(timeSeriesId, tmpStart, tmpEnd, obsPerPeriod);
            	//Collections.reverse(obs);
            	
            	observations.addAll(obs);

            	tmpEnd = DateUtils.addDays(tmpEnd, stepWidth);
            	tmpStart = DateUtils.addDays(tmpStart, stepWidth);
            	
            	icnt++;
            	System.out.println("[GRAPH] ==> Now at sparsing iter " + icnt + "(" + timeSeriesId + ")");
        	}
        	
        	System.out.println("[GRAPH] ==> Final size " + observations.size() + "(" + timeSeriesId + ")");


     

            return observations;
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
        }
        
        return null;
    }
     */

    /*
    public void deleteObservation(Observation observation)
    {
        try {
            Statement delete = QueryBuilder.delete().from("observation").where(QueryBuilder.eq("time_series_id", observation.getTimeSeriesId())).and(QueryBuilder.eq("observation_time", observation.getObservationTime())).and(QueryBuilder.eq("source_id", observation.getSourceId()));

            simpleSession.execute(delete);
            
//            indexService.index(new IndexEvent(observation.getTimeSeriesId(), observation.getObservationTime(), observation.getSourceId(), true));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
        }
    }
     */

    /*
    public void deleteAllObservationsForTimeSeries(TimeSeries timeSeries)
    {
        try {
            Statement delete = QueryBuilder.delete().from("observation").where(QueryBuilder.eq("time_series_id", timeSeries.getId()));
            simpleSession.execute(delete);
            
//            indexService.index(new DeleteSeriesObservationsEvent(timeSeries.getId().intValue()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
        }
    }
     */

    @Override
    public Page<Observation> getObservationsForPageableAndTimeSSeriesId(Pageable pageable, Integer timeSeriesId)
    {
        TimeSeries timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId.longValue());

        List<Observation> preparedResults = new ArrayList<Observation>();

        Long time = System.currentTimeMillis();

        Select select = QueryBuilder.selectFrom("observation_store", "observation").all().whereColumn("time_series_id").isEqualTo(QueryBuilder.bindMarker());

        boolean hasOrder = false;

        if (!pageable.getSort().isUnsorted()) {
            Iterator<Sort.Order> orderIterator = pageable.getSort().iterator();
            while (orderIterator.hasNext()) {
                Sort.Order order = orderIterator.next();

                if (!order.getProperty().equals("UNSORTED")) {
                    if (order.isAscending()) {
                        select = select.orderBy(order.getProperty(), ClusteringOrder.ASC);
                        hasOrder = true;
                    }

                    if (order.isDescending()) {
                        select = select.orderBy(order.getProperty(), ClusteringOrder.DESC);
                        hasOrder = true;
                    }
                }
            }
        }

        if (hasOrder == false) {
            select = select.orderBy("observation_time", ClusteringOrder.DESC);
        }

        PreparedStatement statement = simpleSession.prepare(select.build());
        ResultSet results = simpleSession.execute(statement.bind(timeSeriesId));

        System.out.println("Query took " + ((System.currentTimeMillis() - time) / 1000));

        Iterator<Row> iterator = results.iterator();

        for (Integer i = 0; i < pageable.getOffset(); i++) {
            iterator.next();

//            if (i != 0 && (i % pageable.getPageSize()) == 0 && !results.isFullyFetched()) {
//                results.fetchMoreResults();
//           }

            if (!iterator.hasNext()) {
                break;
            }
        }

        //results.getAvailableWithoutFetching()

        System.out.println("Start Index: " + pageable.getOffset() + " End Index: " + (pageable.getOffset() + pageable.getPageSize()));

        Integer size = 0;

        while (iterator.hasNext() && size < pageable.getPageSize()) {
            Row row = iterator.next();

            Observation o = new Observation();
            o.setTimeSeriesId(row.getInt("time_series_id"));
            o.setObservationTime(Date.from(row.getInstant("observation_time")));
            o.setSourceId(row.getString("source_id"));
            o.setScalarValue(row.getString("scalar_value"));
            o.setComplexValue(row.getMap("complex_value", String.class, String.class));
            o.setMetaData(row.getMap("meta_data", String.class, String.class));
            preparedResults.add(o);

            size++;
        }

        while (preparedResults.size() < pageable.getPageSize()) {
            preparedResults.add(new Observation());
        }

        Integer availableRows = timeSeries.getApproximateSize();

        if (availableRows == null) {
            availableRows = 100000;
        }

        Page<Observation> page = new PageImpl<Observation>(preparedResults, pageable, availableRows);

        return page;
    }
}
