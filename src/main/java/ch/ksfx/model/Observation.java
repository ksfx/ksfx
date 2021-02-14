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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Observation
{
    private Integer timeSeriesId;
    private String sourceId;
    private Date observationTime;
    private String scalarValue;
    private Map<String, String> complexValue = new HashMap<String, String>();
    private Map<String, String> metaData = new HashMap<String, String>();

    public Integer getTimeSeriesId()
    {
        return timeSeriesId;
    }

    public void setTimeSeriesId(Integer timeSeriesId)
    {
        this.timeSeriesId = timeSeriesId;
    }

    public String getSourceId()
    {
        return sourceId;
    }

    public void setSourceId(String sourceId)
    {
        this.sourceId = sourceId;
    }

    public Date getObservationTime()
    {
        return observationTime;
    }

    public void setObservationTime(Date observationTime)
    {
        this.observationTime = observationTime;
    }

    public String getScalarValue()
    {
        return scalarValue;
    }

    public void setScalarValue(String scalarValue)
    {
        this.scalarValue = scalarValue;
    }

    public Map<String, String> getComplexValue()
    {
        return complexValue;
    }

    public void setComplexValue(Map<String, String> complexValue)
    {
        this.complexValue = complexValue;
    }

    public Map<String, String> getMetaData()
    {
        return metaData;
    }

    public void setMetaData(Map<String, String> metaData)
    {
        this.metaData = metaData;
    }

    public void addComplexValueFragment(String key, String value)
    {
        complexValue.put(key, value);
    }

    public String getComplexValueFragmentForName(String name)
    {
        if (complexValue.containsKey(name)) {
            return complexValue.get(name);
        }

        return null;
    }

    public void addMetaDataFragment(String key, String value)
    {
        metaData.put(key, value);
    }
    
    public String getMetaDataFragmentForName(String name)
    {
        if (metaData.containsKey(name)) {
            return metaData.get(name);
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Observation)) return false;

        Observation that = (Observation) o;

        if (!observationTime.equals(that.observationTime)) return false;
        if (!sourceId.equals(that.sourceId)) return false;
        if (!timeSeriesId.equals(that.timeSeriesId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = timeSeriesId.hashCode();
        result = 31 * result + sourceId.hashCode();
        result = 31 * result + observationTime.hashCode();
        return result;
    }
}
