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
import ch.ksfx.model.note.*;
import ch.ksfx.model.spidering.ResourceLoaderPluginConfiguration;
import ch.ksfx.model.spidering.SpideringConfiguration;
import ch.ksfx.util.GenericSelectModel;
import ch.ksfx.util.GenericStreamResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.PropertyAccess;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.upload.services.UploadedFile;
import org.springframework.security.access.annotation.Secured;

import java.util.Date;


@Secured({"ROLE_ADMIN"})
public class ManageNote
{
    @Property
    private Note note;
    
    @Property
    private NoteTimeSeries noteTimeSeries;
    
    @Property
    private NoteActivity noteActivity;
    
    @Property
    private NotePublishingConfiguration notePublishingConfiguration;
    
    @Property
    private NoteSpideringConfiguration noteSpideringConfiguration;
    
    @Property
    private NoteResourceLoaderPluginConfiguration noteResourceLoaderPluginConfiguration;
    
    @Property
    private NoteFile noteFile;
	
	@Property
	private NoteNote noteNote;
	
	@Property
	private String timeSeriesId;

	@Property
	private String activityId;
	
	@Property
	private String publishingConfigurationId;
	
	@Property
	private String resourceLoaderPluginConfigurationId;
	
	@Property
	private String relatedNoteId;
	
	@Property
	private boolean activityMainNote;
	
	@Property
	private boolean timeSeriesMainNote;
	
	@Property
	private boolean spideringConfigurationMainNote;
	
	@Property
	private boolean publishingConfigurationMainNote;
	
	@Property
	private boolean resourceLoaderPluginConfigurationMainNote;
	
	@Property
	private String spideringConfigurationId;
	
	@Property
	private String noteFileComment;
	
    @Inject
    private NoteDAO noteDAO;
	
	@Inject
	private TimeSeriesDAO timeSeriesDAO;
	
	@Inject
	private ActivityDAO activityDAO;
	
	@Inject
	private PageRenderLinkSource pageRenderLinkSource;
	
	@Inject
	private PublishingConfigurationDAO publishingConfigurationDAO;
	
	@Inject
	private SpideringConfigurationDAO spideringConfigurationDAO;
	
	@Inject
	private ResourceLoaderPluginConfigurationDAO resourceLoaderPluginConfigurationDAO;
	
	@Property
	private GenericSelectModel<NoteCategory> allNoteCategories;
	
	@Inject
	private PropertyAccess propertyAccess;
	
	@Property
	private UploadedFile uploadedFile;

	@Secured({"ROLE_ADMIN"})
	public void onActivate(Long noteId)
	{
		note = noteDAO.getNoteForId(noteId);
	}
	
	@Secured({"ROLE_ADMIN"})
	public Link onActivate(Long timeSeriesId, Long activityId, Long publishingConfigurationId, Long spideringConfigurationId, Long resourceLoaderPluginConfigurationId)
	{
		Note note = new Note();
		note.setCreated(new Date());
		note.setLastUpdated(new Date());
		noteDAO.saveOrUpdateNote(note);
		
		if (activityId != null) {
			Activity activity = activityDAO.getActivityForId(activityId);
			note.setTitle("Note for Activity: " + activity.getName());
			
			NoteActivity noteActivity = new NoteActivity();
			noteActivity.setNote(note);
			noteActivity.setActivity(activity);
		
			noteDAO.saveOrUpdateNoteActivity(noteActivity);
		
			note.setLastUpdated(new Date());
			noteDAO.saveOrUpdateNote(note);
		}
		
		if (timeSeriesId != null) {
			TimeSeries timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);
			note.setTitle("Note for Time Series: " + timeSeries.getName());
			
			NoteTimeSeries noteTimeSeries = new NoteTimeSeries();
			noteTimeSeries.setNote(note);
			noteTimeSeries.setTimeSeries(timeSeries);
		
			noteDAO.saveOrUpdateNoteTimeSeries(noteTimeSeries);
		
			note.setLastUpdated(new Date());
			noteDAO.saveOrUpdateNote(note);
		}
		
		if (spideringConfigurationId != null) {
			SpideringConfiguration spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
			note.setTitle("Note for Spidering Configuration: " + spideringConfiguration.getName());
			
			NoteSpideringConfiguration noteSpideringConfiguration = new NoteSpideringConfiguration();
			noteSpideringConfiguration.setNote(note);
			noteSpideringConfiguration.setSpideringConfiguration(spideringConfiguration);
		
			noteDAO.saveOrUpdateNoteSpideringConfiguration(noteSpideringConfiguration);
		
			note.setLastUpdated(new Date());
			noteDAO.saveOrUpdateNote(note);
		}
		
		if (publishingConfigurationId != null) {
			PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
			note.setTitle("Note for Publishing Configuration: " + publishingConfiguration.getName());
			
			NotePublishingConfiguration notePublishingConfiguration = new NotePublishingConfiguration();
			notePublishingConfiguration.setNote(note);
			notePublishingConfiguration.setPublishingConfiguration(publishingConfiguration);
		
			noteDAO.saveOrUpdateNotePublishingConfiguration(notePublishingConfiguration);
		
			note.setLastUpdated(new Date());
			noteDAO.saveOrUpdateNote(note);
		}
		
		if (resourceLoaderPluginConfigurationId != null) {
			ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration = resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId);
			note.setTitle("Note for Resource Loader: " + resourceLoaderPluginConfiguration.getName());
			
			NoteResourceLoaderPluginConfiguration noteResourceLoaderPluginConfiguration = new NoteResourceLoaderPluginConfiguration();
			noteResourceLoaderPluginConfiguration.setNote(note);
			noteResourceLoaderPluginConfiguration.setResourceLoaderPluginConfiguration(resourceLoaderPluginConfiguration);
		
			noteDAO.saveOrUpdateNoteResourceLoaderPluginConfiguration(noteResourceLoaderPluginConfiguration);
		
			note.setLastUpdated(new Date());
			noteDAO.saveOrUpdateNote(note);
		}
		
		//System.out.println("========> NOTEID " + note.getId());
		
		return pageRenderLinkSource.createPageRenderLinkWithContext(ManageNote.class, note.getId());
	}
	
	@Secured({"ROLE_ADMIN"})
	public void onActivate()
	{
		allNoteCategories = new GenericSelectModel<NoteCategory>(noteDAO.getAllNoteCategories(), NoteCategory.class, "name", "id", propertyAccess);
		
		if (note == null) {
			note = new Note();
		}
	}
	
	public Long onPassivate()
	{
		if (note != null) {
			return note.getId();
		}
		
		return null;
	}
	
	public void onSuccessFromNoteForm()
	{
		if (note.getCreated() == null) {
			note.setCreated(new Date());	
		}
		
		note.setLastUpdated(new Date());
		
		noteDAO.saveOrUpdateNote(note);
		
		//return NoteIndex.class;
	}
	
	public void onSuccessFromNoteUploadForm() throws Exception
	{
		byte[] bytes = IOUtils.toByteArray(uploadedFile.getStream());
		
		NoteFile noteFile = new NoteFile();
		noteFile.setFileContent(bytes);
		noteFile.setFileName(uploadedFile.getFileName());
		noteFile.setContentType(uploadedFile.getContentType());
		noteFile.setNote(note);
		noteFile.setComment(noteFileComment);
		
		note.setLastUpdated(new Date());
		
		noteDAO.saveOrUpdateNoteFile(noteFile);
		noteDAO.saveOrUpdateNote(note);
	}
	
	public String getAbbreviatedNoteFileComment()
	{
		if (noteFile.getComment() != null) {
			return StringUtils.abbreviate(noteFile.getComment(), 100);	
		}
		
		return "";
	}
	
	public boolean getCommentIsAbbreviated()
	{
		return (noteFile.getComment() != null && noteFile.getComment().length() > 100);
	}
	
	public void onSuccessFromNoteActivityForm()
	{
		NoteActivity noteActivity = new NoteActivity();
		noteActivity.setNote(note);
		noteActivity.setActivity(activityDAO.getActivityForId(Long.parseLong(activityId)));
		noteActivity.setMainNote(activityMainNote);
		
		noteDAO.saveOrUpdateNoteActivity(noteActivity);
		
		note.setLastUpdated(new Date());
		noteDAO.saveOrUpdateNote(note);
	}
	
	public void onSuccessFromNoteTimeSeriesForm()
	{
		NoteTimeSeries noteTimeSeries = new NoteTimeSeries();
		noteTimeSeries.setNote(note);
		noteTimeSeries.setTimeSeries(timeSeriesDAO.getTimeSeriesForId(Long.parseLong(timeSeriesId)));
		noteTimeSeries.setMainNote(timeSeriesMainNote);
		
		noteDAO.saveOrUpdateNoteTimeSeries(noteTimeSeries);
		
		note.setLastUpdated(new Date());
		noteDAO.saveOrUpdateNote(note);
	}
	
	public void onSuccessFromNoteSpideringConfigurationForm()
	{
		NoteSpideringConfiguration noteSpideringConfiguration = new NoteSpideringConfiguration();
		noteSpideringConfiguration.setNote(note);
		noteSpideringConfiguration.setSpideringConfiguration(spideringConfigurationDAO.getSpideringConfigurationForId(Long.parseLong(spideringConfigurationId)));
		noteSpideringConfiguration.setMainNote(spideringConfigurationMainNote);
		
		noteDAO.saveOrUpdateNoteSpideringConfiguration(noteSpideringConfiguration);
		
		note.setLastUpdated(new Date());
		noteDAO.saveOrUpdateNote(note);
	}
	
	public void onSuccessFromNotePublishingConfigurationForm()
	{
		NotePublishingConfiguration notePublishingConfiguration = new NotePublishingConfiguration();
		notePublishingConfiguration.setNote(note);
		notePublishingConfiguration.setPublishingConfiguration(publishingConfigurationDAO.getPublishingConfigurationForId(Long.parseLong(publishingConfigurationId)));
		notePublishingConfiguration.setMainNote(publishingConfigurationMainNote);	
		
		noteDAO.saveOrUpdateNotePublishingConfiguration(notePublishingConfiguration);
		
		note.setLastUpdated(new Date());
		noteDAO.saveOrUpdateNote(note);
	}
	
	public void onSuccessFromNoteResourceLoaderPluginConfigurationForm()
	{
		NoteResourceLoaderPluginConfiguration noteResourceLoaderPluginConfiguration = new NoteResourceLoaderPluginConfiguration();
		noteResourceLoaderPluginConfiguration.setNote(note);
		noteResourceLoaderPluginConfiguration.setResourceLoaderPluginConfiguration(resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(Long.parseLong(resourceLoaderPluginConfigurationId)));
		noteResourceLoaderPluginConfiguration.setMainNote(resourceLoaderPluginConfigurationMainNote);	
		
		noteDAO.saveOrUpdateNoteResourceLoaderPluginConfiguration(noteResourceLoaderPluginConfiguration);
		
		note.setLastUpdated(new Date());
		noteDAO.saveOrUpdateNote(note);
	}
	
	public void onSuccessFromNoteNoteForm()
	{
		NoteNote noteNote = new NoteNote();
		noteNote.setNote(note);
		noteNote.setRelatedNote(noteDAO.getNoteForId(Long.parseLong(relatedNoteId)));
		
		noteDAO.saveOrUpdateNoteNote(noteNote);
		
		note.setLastUpdated(new Date());
		noteDAO.saveOrUpdateNote(note);
	}
	
	public StreamResponse onActionFromDownloadNoteFile(Long noteFileId)
	{
		NoteFile noteFile = noteDAO.getNoteFileForId(noteFileId);
		GenericStreamResponse genericStreamResponse = new GenericStreamResponse(noteFile.getFileContent(), noteFile.getFileName(), noteFile.getContentType(), true);
		
		return genericStreamResponse; 
	}
	
	public void onActionFromDeleteNoteFile(Long noteFileId)
	{
		note.setLastUpdated(new Date());
		noteDAO.deleteNoteFile(noteDAO.getNoteFileForId(noteFileId));
		noteDAO.saveOrUpdateNote(note);
	}
	
	public void onActionFromDeleteNoteNote(Long noteNoteId)
	{
		note.setLastUpdated(new Date());
		noteDAO.deleteNoteNote(noteDAO.getNoteNoteForId(noteNoteId));
		noteDAO.saveOrUpdateNote(note);
	}
	
	public void onActionFromDeleteNoteTimeSeries(Long noteTimeSeriesId)
	{
		note.setLastUpdated(new Date());
		noteDAO.deleteNoteTimeSeries(noteDAO.getNoteTimeSeriesForId(noteTimeSeriesId));
		noteDAO.saveOrUpdateNote(note);
	}
	
	public void onActionFromDeleteNoteActivity(Long noteActivityId)
	{
		note.setLastUpdated(new Date());
		noteDAO.deleteNoteActivity(noteDAO.getNoteActivityForId(noteActivityId));
		noteDAO.saveOrUpdateNote(note);
	}
	
	public void onActionFromDeleteNotePublishingConfiguration(Long notePublishingConfigurationId)
	{
		note.setLastUpdated(new Date());
		noteDAO.deleteNotePublishingConfiguration(noteDAO.getNotePublishingConfigurationForId(notePublishingConfigurationId));
		noteDAO.saveOrUpdateNote(note);
	}
	
	public void onActionFromDeleteNoteSpideringConfiguration(Long noteSpideringConfigurationId)
	{
		note.setLastUpdated(new Date());
		noteDAO.deleteNoteSpideringConfiguration(noteDAO.getNoteSpideringConfigurationForId(noteSpideringConfigurationId));
		noteDAO.saveOrUpdateNote(note);
	}
	
	public void onActionFromDeleteNoteResourceLoaderPluginConfiguration(Long noteResourceLoaderPluginConfigurationId)
	{
		note.setLastUpdated(new Date());
		noteDAO.deleteNoteResourceLoaderPluginConfiguration(noteDAO.getNoteResourceLoaderPluginConfigurationForId(noteResourceLoaderPluginConfigurationId));
		noteDAO.saveOrUpdateNote(note);
	}
	
	public void onActionFromToggleMainNoteTimeSeries(Long noteTimeSeriesId)
	{
		note.setLastUpdated(new Date());
		NoteTimeSeries noteTimeSeries = noteDAO.getNoteTimeSeriesForId(noteTimeSeriesId);
		noteTimeSeries.setMainNote(!noteTimeSeries.getMainNote());
		
		noteDAO.saveOrUpdateNoteTimeSeries(noteTimeSeries);
		noteDAO.saveOrUpdateNote(note);		
	}
	
	public String getMainNoteTimeSeriesClass()
	{
		if (noteTimeSeries.getMainNote()) {
			return "glyphicon glyphicon-star";
		}
		
		return "glyphicon glyphicon-star-empty";
	}
	
	public void onActionFromToggleMainNoteActivity(Long noteActivityId)
	{
		note.setLastUpdated(new Date());
		NoteActivity noteActivity = noteDAO.getNoteActivityForId(noteActivityId);
		noteActivity.setMainNote(!noteActivity.getMainNote());
		
		noteDAO.saveOrUpdateNoteActivity(noteActivity);
		noteDAO.saveOrUpdateNote(note);		
	}
	
	public String getMainNoteActivityClass()
	{
		if (noteActivity.getMainNote()) {
			return "glyphicon glyphicon-star";
		}
		
		return "glyphicon glyphicon-star-empty";
	}
	
	public void onActionFromToggleMainNoteSpideringConfiguration(Long noteSpideringConfigurationId)
	{
		note.setLastUpdated(new Date());
		NoteSpideringConfiguration noteSpideringConfiguration = noteDAO.getNoteSpideringConfigurationForId(noteSpideringConfigurationId);
		noteSpideringConfiguration.setMainNote(!noteSpideringConfiguration.getMainNote());
		
		noteDAO.saveOrUpdateNoteSpideringConfiguration(noteSpideringConfiguration);
		noteDAO.saveOrUpdateNote(note);		
	}
	
	public String getMainNoteSpideringConfigurationClass()
	{
		if (noteSpideringConfiguration.getMainNote()) {
			return "glyphicon glyphicon-star";
		}
		
		return "glyphicon glyphicon-star-empty";
	}
	
	public void onActionFromToggleMainNotePublishingConfiguration(Long notePublishingConfigurationId)
	{
		note.setLastUpdated(new Date());
		NotePublishingConfiguration notePublishingConfiguration = noteDAO.getNotePublishingConfigurationForId(notePublishingConfigurationId);
		notePublishingConfiguration.setMainNote(!notePublishingConfiguration.getMainNote());
		
		noteDAO.saveOrUpdateNotePublishingConfiguration(notePublishingConfiguration);
		noteDAO.saveOrUpdateNote(note);		
	}
	
	public String getMainNotePublishingConfigurationClass()
	{
		if (notePublishingConfiguration.getMainNote()) {
			return "glyphicon glyphicon-star";
		}
		
		return "glyphicon glyphicon-star-empty";
	}
	
	public void onActionFromToggleMainNoteResourceLoaderPluginConfiguration(Long noteResourceLoaderPluginConfigurationId)
	{
		note.setLastUpdated(new Date());
		NoteResourceLoaderPluginConfiguration noteResourceLoaderPluginConfiguration = noteDAO.getNoteResourceLoaderPluginConfigurationForId(noteResourceLoaderPluginConfigurationId);
		noteResourceLoaderPluginConfiguration.setMainNote(!noteResourceLoaderPluginConfiguration.getMainNote());
		
		noteDAO.saveOrUpdateNoteResourceLoaderPluginConfiguration(noteResourceLoaderPluginConfiguration);
		noteDAO.saveOrUpdateNote(note);		
	}
	
	public String getMainNoteResourceLoaderPluginConfigurationClass()
	{
		if (noteResourceLoaderPluginConfiguration.getMainNote()) {
			return "glyphicon glyphicon-star";
		}
		
		return "glyphicon glyphicon-star-empty";
	}
}