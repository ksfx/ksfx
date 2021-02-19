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
import ch.ksfx.model.Observation;
import ch.ksfx.model.TimeSeries;
//import ch.ksfx.web.services.lucene.DeleteSeriesObservationsEvent;
//import ch.ksfx.web.services.lucene.IndexEvent;
//import ch.ksfx.web.services.lucene.IndexService;
//import ch.ksfx.web.services.systemenvironment.SystemEnvironment;
import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.apache.commons.lang.time.DateUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class CassandraObservationDAO implements ObservationDAO
{
//    private SystemEnvironment systemEnvironment;
//    private IndexService indexService;

	Cluster simpleCluster = Cluster.builder().addContactPoint("localhost").withSocketOptions(new SocketOptions().setConnectTimeoutMillis(20000).setReadTimeoutMillis(30000)).build();
    Session simpleSession = simpleCluster.connect("observation_store");

    PreparedStatement insertObservationStatement = null;

    public CassandraObservationDAO(/*SystemEnvironment systemEnvironment, IndexService indexService*/)
    {
//		this.systemEnvironment = systemEnvironment;
//		this.indexService = indexService;

        insertObservationStatement = simpleSession.prepare("INSERT INTO observation (time_series_id, observation_time, source_id, scalar_value, complex_value, meta_data) VALUES (?,?,?,?,?,?);");
    }

    @Override
    public void saveObservation(Observation observation)
    {

        try {
            BoundStatement bs = new BoundStatement(insertObservationStatement);
            ResultSet rs = simpleSession.execute(bs.bind(observation.getTimeSeriesId(), observation.getObservationTime(), observation.getSourceId(), observation.getScalarValue(), observation.getComplexValue(), observation.getMetaData()));
        
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
            Statement select = QueryBuilder.select().from("observation_store", "observation").where(QueryBuilder.eq("time_series_id", timeSeriesId)).orderBy(QueryBuilder.asc("observation_time")).limit(1);
            
            ResultSet resultSet = simpleSession.execute(select);

            List<Row> rows = resultSet.all();

            if (rows != null && rows.size() > 0) {
                Observation o = new Observation();
                o.setTimeSeriesId(rows.get(0).getInt("time_series_id"));
                //o.setObservationTime(new Date(rows.get(0).getDate("observation_time").getMillisSinceEpoch()));
                o.setObservationTime(rows.get(0).getTimestamp("observation_time"));
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

    @Override
    public Observation getLastObservationForTimeSeriesId(Integer timeSeriesId)
    {
        try {
            Statement select = QueryBuilder.select().from("observation_store", "observation").where(QueryBuilder.eq("time_series_id", timeSeriesId)).limit(1);
            
            ResultSet resultSet = simpleSession.execute(select);

            List<Row> rows = resultSet.all();

            if (rows != null && rows.size() > 0) {
                Observation o = new Observation();
                o.setTimeSeriesId(rows.get(0).getInt("time_series_id"));
                //o.setObservationTime(new Date(rows.get(0).getDate("observation_time").getMillisSinceEpoch()));
                o.setObservationTime(rows.get(0).getTimestamp("observation_time"));
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

    @Override
    public Observation getObservationForTimeSeriesIdObservationTimeAndSourceId(Integer timeSeriesId, Date observationTime, String sourceId)
    {
        try {
            Statement select = QueryBuilder.select().all().from("observation_store", "observation").where(QueryBuilder.eq("source_id", sourceId)).and(QueryBuilder.eq("time_series_id", timeSeriesId)).and(QueryBuilder.eq("observation_time", observationTime));

            ResultSet results = simpleSession.execute(select);

            List<Row> rows = results.all();

            //TODO Exception when more than one result!?
            if (rows.size() > 0) {
                Observation o = new Observation();
                o.setTimeSeriesId(rows.get(0).getInt("time_series_id"));
                //o.setObservationTime(new Date(rows.get(0).getDate("observation_time").getMillisSinceEpoch()));
                o.setObservationTime(rows.get(0).getTimestamp("observation_time"));
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
    
    public List<Observation> queryObservations(Integer timeSeriesId, Date startDate, Date endDate)
    {
        return queryObservations(timeSeriesId, startDate, endDate, null);
    }

    public List<Observation> queryObservations(Integer timeSeriesId, Date startDate, Date endDate, Integer limit)
    {
        try {
            List<Observation> observations = new ArrayList<Observation>();

            Select.Where select = QueryBuilder.select().all().from("observation_store", "observation").where(QueryBuilder.eq("time_series_id", timeSeriesId));

            if (startDate != null) {
                select.and(QueryBuilder.gte("observation_time",startDate));
            }

            if (endDate != null) {
                select.and(QueryBuilder.lte("observation_time", endDate));
            }
            
            if (limit != null) {
            	select.limit(limit);
            }

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
}
