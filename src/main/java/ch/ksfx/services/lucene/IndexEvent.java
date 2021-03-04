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

import java.util.Date;

public class IndexEvent
{
	private Integer seriesId;
	private Date observationTime;
	private String sourceId;
	private boolean deleteEvent;
	
	public IndexEvent(Integer seriesId, Date observationTime, String sourceId, boolean deleteEvent)
	{
		this.seriesId = seriesId;
		this.observationTime = observationTime;
		this.sourceId = sourceId;
		this.deleteEvent = deleteEvent;
	}
	
	public Integer getSeriesId()
	{
		return seriesId;
	}
	
	public Date getObservationTime()
	{
		return observationTime;
	}
	
	public String getSourceId()
	{
		return sourceId;
	}
	
	public boolean getDeleteEvent()
	{
		return deleteEvent;
	}
}