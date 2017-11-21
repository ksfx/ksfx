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
import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.Observation;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.activity.ActivityInstanceParameter;
import ch.ksfx.util.DateFormatUtil;
import ch.ksfx.util.GenericSelectModel;
import ch.ksfx.web.services.activity.ActivityInstanceRunner;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.ioc.services.PropertyAccess;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ObservationViewGeneric
{
	private Integer seriesId;
	private String observationTimeString;
	private String sourceId;

    private Observation observation;
    
    @Inject
    private ObservationDAO observationDAO;

    @Inject
    private ActivityDAO activityDAO;

    @Inject
    private PropertyAccess propertyAccess;

    @Inject
    private ActivityInstanceDAO activityInstanceDAO;

    @Inject
    private ActivityInstanceRunner activityInstanceRunner;

    private Activity sendToActivity;

	private String currentComplexValueKey;

	private String currentMetaDataKey;

    @Persist
    private GenericSelectModel<Activity> allActivities;

	public void onActivate(Object[] objects)
	{
		this.seriesId = Integer.parseInt((String) objects[0]);
		this.observationTimeString = (String) objects[1];
		this.sourceId = (String) objects[2];

        observation = observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(seriesId, DateFormatUtil.parseISO8601TimeAndDateString(observationTimeString), sourceId);
        allActivities = new GenericSelectModel<Activity>(activityDAO.getAllActivities(),Activity.class,"name","id",propertyAccess);
    }
    
    public String getCurrentComplexValueValue()
    {
    	return observation.getComplexValue().get(currentComplexValueKey);
	}

    public String getCurrentMetaDataValue()
    {
    	return observation.getMetaData().get(currentMetaDataKey);
	}
	
	public Object[] getEditingContextParameters()
    {
            return new Object[]{observation.getTimeSeriesId(),
                    DateFormatUtil.formatToISO8601TimeAndDateString(observation.getObservationTime()),
                    observation.getSourceId()};
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

    public void onSuccessFromSendToActivityForm()
    {
        System.out.println(sendToActivity.toString());
        System.out.println(observation.toString());

        ActivityInstance activityInstance = new ActivityInstance();
        activityInstance.setActivity(sendToActivity);

        activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);

        ActivityInstanceParameter activityInstanceParameter = new ActivityInstanceParameter("time_series_id", observation.getTimeSeriesId().toString());
        activityInstanceParameter.setActivityInstance(activityInstance);
        activityInstanceDAO.saveOrUpdateActivityInstanceParameter(activityInstanceParameter);

        activityInstanceParameter = new ActivityInstanceParameter("source_id", observation.getSourceId().toString());
        activityInstanceParameter.setActivityInstance(activityInstance);
        activityInstanceDAO.saveOrUpdateActivityInstanceParameter(activityInstanceParameter);

        activityInstanceParameter = new ActivityInstanceParameter("observation_time", DateFormatUtil.formatToISO8601TimeAndDateString(observation.getObservationTime()));
        activityInstanceParameter.setActivityInstance(activityInstance);
        activityInstanceDAO.saveOrUpdateActivityInstanceParameter(activityInstanceParameter);

        activityInstanceRunner.runActivity(activityInstanceDAO.getActivityInstanceForId(activityInstance.getId()));
    }

    public Observation getObservation()
    {
        return observation;
    }

    public void setObservation(Observation observation)
    {
        this.observation = observation;
    }

    public GenericSelectModel<Activity> getAllActivities()
    {
        return allActivities;
    }

    public void setAllActivities(GenericSelectModel<Activity> allActivities)
    {
        this.allActivities = allActivities;
    }

    public String getCurrentMetaDataKey()
    {
        return currentMetaDataKey;
    }

    public void setCurrentMetaDataKey(String currentMetaDataKey)
    {
        this.currentMetaDataKey = currentMetaDataKey;
    }

    public String getCurrentComplexValueKey()
    {
        return currentComplexValueKey;
    }

    public void setCurrentComplexValueKey(String currentComplexValueKey)
    {
        this.currentComplexValueKey = currentComplexValueKey;
    }

    public Activity getSendToActivity()
    {
        return sendToActivity;
    }

    public void setSendToActivity(Activity sendToActivity)
    {
        this.sendToActivity = sendToActivity;
    }
}