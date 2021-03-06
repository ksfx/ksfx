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

import ch.ksfx.model.Observation;
import ch.ksfx.model.TimeSeries;

import java.util.Date;
import java.util.List;

public interface ObservationDAO
{
	public Observation getFirstObservationForTimeSeriesId(Integer timeSeriesId);
    public Observation getLastObservationForTimeSeriesId(Integer timeSeriesId);
    public void saveObservation(Observation observation);
    public Observation getObservationForTimeSeriesIdObservationTimeAndSourceId(Integer timeSeriesId, Date observationTime, String sourceId);
    public List<Observation> getObservations();
    public List<Observation> queryObservations(Integer timeSeriesId, Date startDate, Date endDate);
    public void deleteObservation(Observation observation);
    public void deleteAllObservationsForTimeSeries(TimeSeries timeSeries);
    public List<Observation> queryObservationsSparse(Integer timeSeriesId, Date startDate, Date endDate);
    public List<Observation> queryObservations(Integer timeSeriesId, Date startDate, Date endDate, Integer limit);
}
