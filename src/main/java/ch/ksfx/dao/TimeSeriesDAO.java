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

package ch.ksfx.dao;

import ch.ksfx.model.AssetPricingTimeRange;
import ch.ksfx.model.ImportableField;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.model.TimeSeriesType;
import java.util.List;


public interface TimeSeriesDAO
{
    public TimeSeries getFirstTimeSeriesInDatabase();
    public TimeSeries getTimeSeriesForId(Long timeSeriesId);
    public TimeSeries getTimeSeriesForName(String timeSeriesName);
    public void saveOrUpdate(TimeSeries timeSeries);
    public void delete(TimeSeries timeSeries);
    public List<TimeSeries> getAllTimeSeries();
    public List<TimeSeriesType> getAllTimeSeriesTypes();
    public TimeSeriesType getTimeSeriesTypeForId(Long timeSeriesTypeId);
    public List<TimeSeries> searchTimeSeries(String timeSeriesName, Integer limit);
//    public GridDataSource getTimeSeriesGridDataSource();
    public List<TimeSeries> getIndexableTimeSeries();
    public List<AssetPricingTimeRange> getAllAssetPricingTimeRanges();
    public AssetPricingTimeRange getAssetPricingTimeRangeForName(String intraday);
    public List<ImportableField> getAllImportableFields();
}
