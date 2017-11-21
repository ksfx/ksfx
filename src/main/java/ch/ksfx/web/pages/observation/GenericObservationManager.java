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

package ch.ksfx.web.pages.observation;

import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.model.Observation;
import ch.ksfx.util.DateFormatUtil;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericObservationManager
{
	private Integer seriesId;
	private String observationTimeString;
	private String sourceId;
    
    @Property
    @Persist
    private Integer numberOfAddedComplexValueFragments;
    
    @Property
    @Persist    
    private Integer numberOfAddedMetaDataFragments;
    
    @Persist
    private List<String> complexValueKeys;
    
    @Persist
    private List<String> complexValueValues;
    
    @Persist
    private List<String> metaDataKeys;
    
    @Persist
    private List<String> metaDataValues;
    
    @Persist
    private String sourceIdBefore; 
    
    @Property
    private Observation observation;
    
    @Inject
    private ObservationDAO observationDAO;
    
    private String complexValueKey;

    @Property
    private String complexValueKeyLoop;
    
    @Property
    private String metaDataKeyLoop;

	public void onActivate(Object[] objects)
	{
        complexValueKeys = new ArrayList<String>();
        complexValueValues = new ArrayList<String>();
        metaDataKeys = new ArrayList<String>();
        metaDataValues = new ArrayList<String>();

		this.seriesId = Integer.parseInt((String) objects[0]);
		this.observationTimeString = (String) objects[1];
		this.sourceId = (String) objects[2];
		
		if (!sourceId.equals(sourceIdBefore)) {
			numberOfAddedComplexValueFragments = 1;	
			numberOfAddedMetaDataFragments = 1;
		}
		
		sourceIdBefore = sourceId;

        System.out.println("Series Id " + seriesId);
        System.out.println("Date " + DateFormatUtil.parseISO8601TimeAndDateString(observationTimeString));
        System.out.println("Source Id " + sourceId);

        observation = observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(seriesId, DateFormatUtil.parseISO8601TimeAndDateString(observationTimeString), sourceId);
    }
    
    public void onActionFromAddMetaDataFragment()
    {
        System.out.println("Adding meta data fragment");

        observation.addMetaDataFragment("META_DATA_" + numberOfAddedMetaDataFragments, "");
		observationDAO.saveObservation(observation);

        numberOfAddedMetaDataFragments++;
    }
    
    public void onActionFromDeleteMetaDataFragment(String fragmentKey)
    {
    	observation.getMetaData().remove(fragmentKey);
    	observationDAO.saveObservation(observation);
    }
    
    public String getMetaDataKey()
	{
		return metaDataKeyLoop;
	}
	
	public void setMetaDataKey(String metaDataKey)
	{
		metaDataKeys.add(metaDataKey);
	}
	
	public String getMetaDataValue()
	{
		return observation.getMetaData().get(metaDataKeyLoop);
	}
	
	public void setMetaDataValue(String metaDataValue)
	{
		metaDataValues.add(metaDataValue);
	}
	
	public void onActionFromAddComplexValueFragment()
    {
        observation.addComplexValueFragment("COMPLEX_VAL_" + numberOfAddedComplexValueFragments, "");
		observationDAO.saveObservation(observation);

        numberOfAddedComplexValueFragments++;
    }
    
    public void onActionFromDeleteComplexValueFragment(String fragmentKey)
    {
    	observation.getComplexValue().remove(fragmentKey);
    	observationDAO.saveObservation(observation);
    }
    
    public String getComplexValueKey()
	{
		return complexValueKeyLoop;
	}
	
	public void setComplexValueKey(String complexValueKey)
	{
		complexValueKeys.add(complexValueKey);
	}
	
	public String getComplexValueValue()
	{
		return observation.getComplexValue().get(complexValueKeyLoop);
	}
	
	public void setComplexValueValue(String complexValueValue)
	{
		complexValueValues.add(complexValueValue);
	}
	
	public void onSuccess()
	{
        Map<String, String> complexValue = new HashMap<String,String>();

		for (Integer i = 0; i < complexValueKeys.size(); i++) {
			System.out.println(complexValueKeys.get(i) + ": " + complexValueValues.get(i));
		    complexValue.put(complexValueKeys.get(i), complexValueValues.get(i));
        }

        observation.setComplexValue(complexValue);
        
        Map<String, String> metaData = new HashMap<String,String>();

		for (Integer i = 0; i < metaDataKeys.size(); i++) {
			System.out.println(metaDataKeys.get(i) + ": " + metaDataValues.get(i));
		    metaData.put(metaDataKeys.get(i), metaDataValues.get(i));
        }

        observation.setMetaData(metaData);

        observationDAO.saveObservation(observation);
	}
	
	public Object onPassivate()
	{
		List<Object> p = new ArrayList<Object>();
		
		if (seriesId != null) {
			p.add(seriesId);
		}
		
		if (observationTimeString != null) {
			p.add(observationTimeString);
		}
		
		if (sourceId != null) {
			p.add(sourceId);
		}
		
		return p;
	}
}