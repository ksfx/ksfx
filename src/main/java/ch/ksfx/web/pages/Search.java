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

package ch.ksfx.web.pages;

import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.Observation;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.util.DateFormatUtil;
import ch.ksfx.util.LuceneObservationGridDataSource;
import ch.ksfx.web.services.systemenvironment.SystemEnvironment;
import org.apache.commons.lang.time.DateUtils;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.springframework.security.access.annotation.Secured;

import java.text.SimpleDateFormat;
import java.util.*;


@Secured({"ROLE_ADMIN"})
@Import(library = "context:scripts/viewtimeseries.js")
public class Search
{
    @Inject
    private TimeSeriesDAO timeSeriesDAO;
    
    @Inject
    private ObservationDAO observationDAO;
    
    @Inject
    private SystemEnvironment systemEnvironment;
    
    @Inject
    private ComponentResources componentResources;
    
    @Inject
    private PageRenderLinkSource linkSource;

    @Property
    private TimeSeries timeSeries;
    
    @Property
    private Observation observation;
 
    @Property
    @Persist
    private boolean searchRunning;
 
    @Property
    @Persist
    private Integer numberOfAddedComplexValueFragments;
    
    @Property
    @Persist    
    private Integer numberOfAddedMetaDataFragments;
    
    @Property
    @Persist
    private String allQuery;
    
    @Property
    @Persist
    private String scalarValueQuery;
    
    @Property
    @Persist
    private Date dateFrom;
    
    @Persist
    private Date dateTo;
    
    @Property
    @Persist
    private String seriesId;
    
    @Property
    @Persist
    private Map<String, String> complexValueQuery;

    @Property
    @Persist
    private Map<String, String> metaDataQuery;
    
    @Persist
    private List<String> complexValueKeys;
    
    @Persist
    private List<String> complexValueValues;
    
    @Persist
    private List<String> metaDataKeys;
    
    @Persist
    private List<String> metaDataValues;
    
    @Property
    private String complexValueKeyLoop;
    
    @Property
    private String metaDataKeyLoop;
    
    @Property
    @Persist
    private String activeObservationId;

    @Property
    @Persist
    private Date activeObservationObservationTime;
    
    @Property
    @Persist
    private Integer activeObservationTimeSeriesId;
    
    @Secured({"ROLE_ADMIN"})
    public void onActivate(String seriesId)
    {
        onActivate(seriesId, null, null);
    } 
    
    @Secured({"ROLE_ADMIN"})
    public void onActivate(String seriesId, String dateFromString, String dateToString)
    {
        SimpleDateFormat sdfOrderable = new SimpleDateFormat("yyyy-MM-dd");
        
        this.seriesId = seriesId;
        
        if (dateFromString != null) {
        	try {
            	dateFrom = sdfOrderable.parse(dateFromString);
        	} catch (Exception e) {
        		throw new RuntimeException(e);
        	}
        }
        
        if (dateToString != null) {
        	try {
            	setDateTo(sdfOrderable.parse(dateToString));
        	} catch (Exception e) {
        		throw new RuntimeException(e);
        	}
        }
        
        searchRunning = true;
    }
    
    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {
    	if (numberOfAddedComplexValueFragments == null) {
    		numberOfAddedComplexValueFragments = 1;
    	}
    	
    	if (numberOfAddedMetaDataFragments == null) {
    		numberOfAddedMetaDataFragments = 1;
    	}
    	
    	if (complexValueQuery == null) {
    		complexValueQuery = new HashMap<String, String>();
    	}
    	
    	if (metaDataQuery == null) {
    		metaDataQuery = new HashMap<String, String>();
    	}
    	
    	complexValueKeys = new ArrayList<String>();
        complexValueValues = new ArrayList<String>();
        metaDataKeys = new ArrayList<String>();
        metaDataValues = new ArrayList<String>();
    }
    
    public Date getDateTo()
    {
    	return dateTo;
    }
    
    public void setDateTo(Date dateTo)
    {
    	if (dateTo == null) {
    		return;
    	}
    	//search till end of day
    	dateTo = DateUtils.addHours(dateTo, 23);
    	dateTo = DateUtils.addMinutes(dateTo, 59);
    	dateTo = DateUtils.addSeconds(dateTo, 59);
    	
    	this.dateTo = dateTo;
    }
    
    public List<TimeSeries> getAllTimeSeries()
    {
        return timeSeriesDAO.getAllTimeSeries();
    }
    
    public GridDataSource getObservationGridDataSource()
    {
        return new LuceneObservationGridDataSource(systemEnvironment, observationDAO, allQuery, scalarValueQuery, complexValueQuery, metaDataQuery, dateFrom, dateTo, seriesId);
    }
    
    public void onActionFromCancelSearch()
    {
    	allQuery = null;
    	scalarValueQuery = null;
    	dateFrom = null;
    	dateTo = null;
    	seriesId = null;
    	complexValueQuery = new HashMap<String, String>();
    	metaDataQuery = new HashMap<String, String>();
    	numberOfAddedComplexValueFragments = 1;
    	numberOfAddedMetaDataFragments = 1;
    	
    	searchRunning = false;
    }
    
    public void onActionFromAddMetaDataFragment()
    {
        metaDataQuery.put("META_DATA_" + numberOfAddedMetaDataFragments, "");

        numberOfAddedMetaDataFragments++;
    }
    
    public void onActionFromDeleteMetaDataFragment(String fragmentKey)
    {
    	metaDataQuery.remove(fragmentKey);
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
		return metaDataQuery.get(metaDataKeyLoop);
	}
	
	public void setMetaDataValue(String metaDataValue)
	{
		metaDataValues.add(metaDataValue);
	}
	
	public void onActionFromAddComplexValueFragment()
    {
        complexValueQuery.put("COMPLEX_VAL_" + numberOfAddedComplexValueFragments, "");

        numberOfAddedComplexValueFragments++;
    }
    
    public void onActionFromDeleteComplexValueFragment(String fragmentKey)
    {
    	complexValueQuery.remove(fragmentKey);
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
		return complexValueQuery.get(complexValueKeyLoop);
	}
	
	public void setComplexValueValue(String complexValueValue)
	{
		complexValueValues.add(complexValueValue);
	}
	
	public void onActionFromOpenNews(Integer timeSeriesId, String observationSourceId, String observationTime)
    {
        System.out.println("Obs ID: " + observationSourceId);

		activeObservationTimeSeriesId = timeSeriesId;
        activeObservationId = observationSourceId;
        activeObservationObservationTime = new Date(Long.parseLong(observationTime));
    }
    
    public Observation getActiveObservation()
    {
        if (activeObservationId == null) {
            return null;
        }

        return observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(activeObservationTimeSeriesId,activeObservationObservationTime,activeObservationId);
    }
    
    public boolean getIsObservationActiveObservation()
    {
        return observation.getSourceId().equals(activeObservationId);
    }
    
   	public boolean getHasActiveObservation()
    {
        return activeObservationId != null;
    }
    
    public String getIframeLink()
    {
        if (activeObservationTimeSeriesId != null) {
            TimeSeries ts = timeSeriesDAO.getTimeSeriesForId(activeObservationTimeSeriesId.longValue());
            return linkSource.createPageRenderLinkWithContext("observation/" + ts.getTimeSeriesType().getObservationView(), getEditingContextParameters()).toURI();
        }

        return null;
    }
    
    public void onActionFromCloseParameterWindow()
    {
        activeObservationTimeSeriesId = null;
        activeObservationId = null;
        activeObservationObservationTime = null;
    }

    public void onActionFromCancelParameterWindow()
    {
        onActionFromCloseParameterWindow();
    }
    
    public void onSuccess()
    {
        for (Integer i = 0; i < complexValueKeys.size(); i++) {
            System.out.println(complexValueKeys.get(i) + ": " + complexValueValues.get(i));
            complexValueQuery.put(complexValueKeys.get(i), complexValueValues.get(i));
        }

        for (Integer i = 0; i < metaDataKeys.size(); i++) {
            System.out.println(metaDataKeys.get(i) + ": " + metaDataValues.get(i));
            metaDataQuery.put(metaDataKeys.get(i), metaDataValues.get(i));
        }

    	searchRunning = true;	
    }
    
    public Object[] getEditingContextParameters()
    {
        if (observation != null) {
            return new Object[]{activeObservationTimeSeriesId,
                    DateFormatUtil.formatToISO8601TimeAndDateString(activeObservationObservationTime),
                    activeObservationId};
        }

        return null;
    }
}