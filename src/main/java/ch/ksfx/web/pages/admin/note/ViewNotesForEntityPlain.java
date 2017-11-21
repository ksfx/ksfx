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

package ch.ksfx.web.pages.admin.note;

import ch.ksfx.dao.NoteDAO;
import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.spidering.ResourceLoaderPluginConfigurationDAO;
import ch.ksfx.dao.spidering.SpideringConfigurationDAO;
import ch.ksfx.model.PublishingConfiguration;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.note.Note;
import ch.ksfx.model.spidering.ResourceLoaderPluginConfiguration;
import ch.ksfx.model.spidering.SpideringConfiguration;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.springframework.security.access.annotation.Secured;

import java.util.ArrayList;
import java.util.List;


@Secured({"ROLE_ADMIN"})
public class ViewNotesForEntityPlain
{
    @Property
    private Note note;

    @Inject
    private NoteDAO noteDAO;
	
	@Inject
	private TimeSeriesDAO timeSeriesDAO;
	
	@Inject
	private ActivityDAO activityDAO;
	
	@Inject
	private PublishingConfigurationDAO publishingConfigurationDAO;
	
	@Inject
	private SpideringConfigurationDAO spideringConfigurationDAO;
	
	@Inject
	private ResourceLoaderPluginConfigurationDAO resourceLoaderPluginConfigurationDAO;
	
	@Inject
	private PageRenderLinkSource pageRenderLinkSource;
	
	private Long timeSeriesId;
	private Long activityId;
	private Long publishingConfigurationId;
	private Long spideringConfigurationId;
	private Long resourceLoaderPluginConfigurationId;
	
	
	@Secured({"ROLE_ADMIN"})
	public void onActivate(Long timeSeriesId, Long activityId, Long publishingConfigurationId, Long spideringConfigurationId, Long resourceLoaderPluginConfigurationId)
	{
		this.timeSeriesId = timeSeriesId;
		this.activityId = activityId;
		this.publishingConfigurationId = publishingConfigurationId;
		this.spideringConfigurationId = spideringConfigurationId;
		this.resourceLoaderPluginConfigurationId = resourceLoaderPluginConfigurationId;
	}
	
	public Long[] onPassivate()
	{
		List<Long> p = new ArrayList<Long>();
		p.add(timeSeriesId);
		p.add(activityId);
		p.add(publishingConfigurationId);
		p.add(spideringConfigurationId);
		p.add(resourceLoaderPluginConfigurationId);
		
		return p.toArray(new Long[4]);
	}
	
	public String getCreateNewNoteLink()
    {
    	return pageRenderLinkSource.createPageRenderLinkWithContext("admin/note/managenote", getCreateNewNoteContextParameters()).toURI();	
    }
    
    public Object[] getCreateNewNoteContextParameters()
    {

            return new Object[]{timeSeriesId,
                    activityId,
                    publishingConfigurationId,spideringConfigurationId,resourceLoaderPluginConfigurationId};
    }
    
    public String getName()
    {
    	if (timeSeriesId != null) {
    		return timeSeriesDAO.getTimeSeriesForId(timeSeriesId).getName();
    	}
    	
    	if (activityId != null) {
    		return activityDAO.getActivityForId(activityId).getName();
    	}
    	
    	if (publishingConfigurationId != null) {
    		return publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId).getName();
    	}
    	
    	if (spideringConfigurationId != null) {
    		return spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId).getName();
    	}
    	
    	if (resourceLoaderPluginConfigurationId != null) {
    		return resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId).getName();
    	}
    	
    	return "";
    }

    public List<Note> getMainNotes()
    {
		TimeSeries timeSeries = null;
		
		if (timeSeriesId != null) {
			timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);
		}
		
		Activity activity = null;
		
		if (activityId != null) {
			activity = activityDAO.getActivityForId(activityId);
		}
		
		PublishingConfiguration publishingConfiguration = null;
		
		if (publishingConfigurationId != null) {
			publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
		}
		
		SpideringConfiguration spideringConfiguration = null;
		
		if (spideringConfigurationId != null) {
			spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
		}
		
		ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration = null;
		
		if (resourceLoaderPluginConfigurationId != null) {
			resourceLoaderPluginConfiguration = resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId);
		}
		
        return noteDAO.searchNotes(null, null, timeSeries, activity, publishingConfiguration, spideringConfiguration, resourceLoaderPluginConfiguration, true);
    }

    public List<Note> getNotes()
    {
		TimeSeries timeSeries = null;
		
		if (timeSeriesId != null) {
			timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);
		}
		
		Activity activity = null;
		
		if (activityId != null) {
			activity = activityDAO.getActivityForId(activityId);
		}
		
		PublishingConfiguration publishingConfiguration = null;
		
		if (publishingConfigurationId != null) {
			publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
		}
		
		SpideringConfiguration spideringConfiguration = null;
		
		if (spideringConfigurationId != null) {
			spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
		}
		
		ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration = null;
		
		if (resourceLoaderPluginConfigurationId != null) {
			resourceLoaderPluginConfiguration = resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId);
		}
		
        return noteDAO.searchNotes(null, null, timeSeries, activity, publishingConfiguration, spideringConfiguration, resourceLoaderPluginConfiguration, false);
    }
}

