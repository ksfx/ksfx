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

package ch.ksfx.model;

import javax.persistence.*;

/**
 * Created by Kejo on 20.04.2015.
 */

@Entity
@Table(name = "time_series")
public class TimeSeries
{
    private Long id;
    private String name;
    private String locator;
    private String source;
    private String sourceId;
    private Integer approximateSize;
    private TimeSeriesType timeSeriesType;
    private boolean indexable;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
    
    public String getLocator()
    {
    	return locator;	
    }
    
    public void setLocator(String locator)
    {
    	this.locator = locator;
    }
    
    public String getSource()
    {
    	return source;	
    }
    
    public void setSource(String source)
    {
    	this.source = source;
    }
    
    public String getSourceId()
    {
    	return sourceId;	
    }
    
    public void setSourceId(String sourceId)
    {
    	this.sourceId = sourceId;
    }
    
    public Integer getApproximateSize()
    {
    	return approximateSize;	
    }
    
    public void setApproximateSize(Integer approximateSize)
    {
    	this.approximateSize = approximateSize;
    }

    @ManyToOne
    @JoinColumn(name = "time_series_type")
    public TimeSeriesType getTimeSeriesType()
    {
        return timeSeriesType;
    }

    public void setTimeSeriesType(TimeSeriesType timeSeriesType)
    {
        this.timeSeriesType = timeSeriesType;
    }
    
    public boolean getIndexable()
    {
    	return indexable;
    }
    
    public void setIndexable(boolean indexable)
    {
    	this.indexable = indexable;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimeSeries that = (TimeSeries) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }
}
