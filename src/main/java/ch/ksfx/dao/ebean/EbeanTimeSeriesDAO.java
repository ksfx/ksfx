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

import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.AssetPricingTimeRange;
import ch.ksfx.model.ImportableField;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.model.TimeSeriesType;
import ch.ksfx.model.note.NoteTimeSeries;
import ch.ksfx.util.EbeanGridDataSource;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import io.ebean.RawSqlBuilder;
import org.apache.tapestry5.grid.GridDataSource;

import java.util.List;


public class EbeanTimeSeriesDAO implements TimeSeriesDAO
{
    @Override
    public TimeSeries getFirstTimeSeriesInDatabase()
    {
        return Ebean.find(TimeSeries.class).setMaxRows(1).orderBy("id ASC").findUnique();
    }

    @Override
    public TimeSeries getTimeSeriesForId(Long timeSeriesId)
    {
        return Ebean.find(TimeSeries.class, timeSeriesId);
    }

	@Override
    public TimeSeries getTimeSeriesForName(String timeSeriesName)
    {
        return Ebean.find(TimeSeries.class).where().eq("name", timeSeriesName).findUnique();
    }
    
    @Override
    public List<TimeSeries> getIndexableTimeSeries()
    {
    	return Ebean.find(TimeSeries.class).where().eq("indexable", true).findList();
    }
    
    @Override
    public List<TimeSeries> searchTimeSeries(String timeSeriesName, Integer limit)
    {
    	//String sql = "SELECT id, ask, bid, pricing_time, asset, rownum " +
        //                "FROM ( " +
        //                    "SELECT " +
        //                        "@row := @row +1 AS rownum, `id`, `ask`, `bid`, `pricing_time`, `asset` " +
        //                        "FROM ( " +
        //                            "SELECT @row :=0) r, asset_price WHERE asset = " + asset.getId() + " AND pricing_time BETWEEN '" + sqlStartDate.toString() + "' AND '" + sqlEndDate.toString() + "' ORDER BY `pricing_time` ASC " +
        //                    ") ranked " +
        //                "WHERE rownum % " + granularity + " = 1";

        //logger.info("SIMULATION_TRACE " + sql);

        //return Ebean.find(AssetPrice.class).setRawSql(RawSqlBuilder.parse(sql).columnMappingIgnore("rownum").columnMapping("pricing_time", "pricingTime").columnMapping("asset", "asset.id").create()).findList();

	    //private Long id;
	    //private String name;
	    //private String locator;
	    //private String source;
	    //private String sourceId;
	    //private Integer approximateSize;
	    //private TimeSeriesType timeSeriesType;
	    //private boolean indexable;    	

		String sql = "SELECT id, name, locator, source, source_id, approximate_size, time_series_type, indexable FROM time_series WHERE MATCH(name) AGAINST('" + timeSeriesName + "' IN BOOLEAN MODE) LIMIT " + limit + ";";

    	return Ebean.find(TimeSeries.class).setRawSql(RawSqlBuilder.parse(sql).columnMapping("source_id", "sourceId").columnMapping("approximate_size", "approximateSize").columnMapping("time_series_type", "timeSeriesType.id").create()).findList();
    	
        //return Ebean.find(TimeSeries.class).where().like("name", "%" + timeSeriesName + "%").findList();
    }

    @Override
    public void saveOrUpdate(TimeSeries timeSeries)
    {
        if (timeSeries.getId() != null) {
            Ebean.update(timeSeries);
        } else {
            Ebean.save(timeSeries);
        }
    }

    @Override
    public void delete(TimeSeries timeSeries)
    {
        List<NoteTimeSeries> noteTimeSerieses = Ebean.find(NoteTimeSeries.class).where().eq("timeSeries",timeSeries).findList();
        
        for (NoteTimeSeries noteTimeSeries : noteTimeSerieses) {
            Ebean.delete(noteTimeSeries);   
        }
    
        Ebean.delete(timeSeries);
    }

    @Override
    public List<TimeSeries> getAllTimeSeries()
    {
        return Ebean.find(TimeSeries.class).findList();
    }

    @Override
    public TimeSeriesType getTimeSeriesTypeForId(Long timeSeriesTypeId)
    {
        return Ebean.find(TimeSeriesType.class, timeSeriesTypeId);
    }

    @Override
    public List<TimeSeriesType> getAllTimeSeriesTypes()
    {
        return Ebean.find(TimeSeriesType.class).findList();
    }
    
    @Override
    public GridDataSource getTimeSeriesGridDataSource()
    {
        ExpressionList expressionList = Ebean.find(TimeSeries.class).where();

        return new EbeanGridDataSource(expressionList, TimeSeries.class);
    }    
    
    @Override
    public List<AssetPricingTimeRange> getAllAssetPricingTimeRanges()
    {
        return Ebean.find(AssetPricingTimeRange.class).findList();
    }

    @Override
    public List<ImportableField> getAllImportableFields()
    {
        return Ebean.find(ImportableField.class).findList();
    }
    
    @Override
    public AssetPricingTimeRange getAssetPricingTimeRangeForName(String name)
    {
        return Ebean.find(AssetPricingTimeRange.class).where().eq("name", name).findUnique();
    }
}
