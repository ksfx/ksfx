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

package ch.ksfx.dao.ebean;

import ch.ksfx.dao.NoteDAO;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.note.*;
import ch.ksfx.model.spidering.ResourceLoaderPluginConfiguration;
import ch.ksfx.model.spidering.SpideringConfiguration;
import io.ebean.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Iterator;
import java.util.List;

@Repository
public class EbeanNoteDAO implements NoteDAO
{
	@Override
	public void saveOrUpdateNote(Note note)
	{
		if (note.getId() != null) {
			Ebean.update(note);
		} else {
			Ebean.save(note);
		}
	}
	
	@Override
	public List<Note> getAllNotes()
	{
		return Ebean.find(Note.class).findList();
	}
	
	@Override
	public List<Note> getNotesForNoteCategory(NoteCategory noteCategory)
	{
		if (noteCategory == null) {
			return Ebean.find(Note.class).findList();
		} else {
			return Ebean.find(Note.class).where().eq("noteCategory", noteCategory).findList();
		}
	}

	@Override
	public Page<Note> getNotesForPageableAndNoteCategory(Pageable pageable, NoteCategory noteCategory)
	{
		ExpressionList expressionList = Ebean.find(Note.class).where();

		if (noteCategory != null) {
			expressionList.eq("noteCategory", noteCategory);
		}

		expressionList.setFirstRow(new Long(pageable.getOffset()).intValue());
		expressionList.setMaxRows(pageable.getPageSize());

		if (!pageable.getSort().isUnsorted()) {
			Iterator<Sort.Order> orderIterator = pageable.getSort().iterator();
			while (orderIterator.hasNext()) {
				Sort.Order order = orderIterator.next();

				if (!order.getProperty().equals("UNSORTED")) {
					if (order.isAscending()) {
						expressionList.order().asc(order.getProperty());
					}

					if (order.isDescending()) {
						expressionList.order().desc(order.getProperty());
					}
				}
			}
		}

		Page<Note> page = new PageImpl<Note>(expressionList.findList(), pageable, expressionList.findCount());

		return page;
	}

	public List<NoteActivity> getNoteActivitiesForActivity(Activity activity)
	{
		return Ebean.find(NoteActivity.class).where().eq("activity", activity).findList();
	}
	
	public List<NoteTimeSeries> getNoteTimeSeriesForTimeSeries(TimeSeries timeSeries)
	{
		return Ebean.find(NoteTimeSeries.class).where().eq("timeSeries", timeSeries).findList();
	}
	
	public List<NotePublishingConfiguration> getNotePublishingConfigurationForPublishingConfiguration(PublishingConfiguration publishingConfiguration)
	{
		return Ebean.find(NotePublishingConfiguration.class).where().eq("publishingConfiguration", publishingConfiguration).findList();
	}
	
	public List<NoteSpideringConfiguration> getNoteSpideringConfigurationForSpideringConfiguration(SpideringConfiguration spideringConfiguration)
	{
		return Ebean.find(NoteSpideringConfiguration.class).where().eq("spideringConfiguration", spideringConfiguration).findList();
	}
	
	@Override
	public List<Note> searchNotes(String title, NoteCategory noteCategory, TimeSeries timeSeries, Activity activity, PublishingConfiguration publishingConfiguration, SpideringConfiguration spideringConfiguration, ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration, boolean searchMainNotes)
	{
		String sql = "SELECT DISTINCT(n.id), n.title, n.content, n.created, n.last_updated, n.note_category FROM note n LEFT JOIN note_time_series nts ON nts.note = n.id LEFT JOIN note_activity na ON na.note = n.id LEFT JOIN note_resource_loader_plugin_configuration nrlpc ON nrlpc.note = n.id" +
					" LEFT JOIN note_publishing_configuration npc ON npc.note = n.id LEFT JOIN note_spidering_configuration nsc ON nsc.note = n.id WHERE 1";
		
		if (noteCategory != null) {
			sql += " AND n.note_category = " + noteCategory.getId();
		}
		
		if (timeSeries != null) {
			sql += " AND nts.time_series = " + timeSeries.getId();
			
			if (searchMainNotes) {
				sql += " AND nts.main_note = b'1'";
			}
		}
		
		if (activity != null) {
			sql += " AND na.activity = " + activity.getId();
			
			if (searchMainNotes) {
				sql += " AND na.main_note = b'1'";
			}
		}
		
		if (publishingConfiguration != null) {
			sql += " AND npc.publishing_configuration = " + publishingConfiguration.getId();
			
			if (searchMainNotes) {
				sql += " AND npc.main_note = b'1'";
			}
		}
		
		if (spideringConfiguration != null) {
			sql += " AND nsc.spidering_configuration = " + spideringConfiguration.getId();
			
			if (searchMainNotes) {
				sql += " AND nsc.main_note = b'1'";
			}
		}
		
		if (resourceLoaderPluginConfiguration != null) {
			sql += " AND nrlpc.resource_loader_plugin_configuration = " + resourceLoaderPluginConfiguration.getId();
			
			if (searchMainNotes) {
				sql += " AND nrlpc.main_note = b'1'";
			}
		}
		
		System.out.println(sql);
		
		RawSql rawSql = RawSqlBuilder
							.parse(sql)
							.columnMappingIgnore("n.note_category")
							//.columnMappingIgnore("nts.time_series")
							//.columnMappingIgnore("na.activity")
							//.columnMappingIgnore("npc.publishing_configuration")
							//.columnMappingIgnore("nsc.spidering_configuration")
							//.columnMappingIgnore("nrlpc.resource_loader_plugin_configuration")
							.columnMapping("DISTINCT(n.id)","id")
							.columnMapping("n.title","title")
							.columnMapping("n.content","content")
							.columnMapping("n.created","created")
							.columnMapping("n.last_updated","lastUpdated")
							.create();
						
		Query<Note> query = Ebean.find(Note.class);
		query.setRawSql(rawSql);
		
		return query.findList();	
	}
	
	@Override
	public Note getNoteForId(Long noteId)
	{
		return Ebean.find(Note.class, noteId);
	}
	
	@Override
	public void deleteNote(Note note)
	{
		for (NoteFile noteFile : note.getNoteFiles()) {
			Ebean.delete(noteFile);
		}
		
		for (NoteActivity noteActivity : note.getNoteActivities()) {
			Ebean.delete(noteActivity);
		}
		
		for (NoteTimeSeries noteTimeSeries : note.getNoteTimeSeries()) {
			Ebean.delete(noteTimeSeries);
		}
		
		for (NotePublishingConfiguration notePublishingConfiguration : note.getNotePublishingConfigurations()) {
			Ebean.delete(notePublishingConfiguration);	
		}
		
		for (NoteSpideringConfiguration noteSpideringConfiguration : note.getNoteSpideringConfigurations()) {
			Ebean.delete(noteSpideringConfiguration);	
		}
		
		Ebean.delete(note);
	}
	
	@Override
	public void saveOrUpdateNoteCategory(NoteCategory noteCategory)
	{
		if (noteCategory.getId() != null) {
			Ebean.update(noteCategory);
		} else {
			Ebean.save(noteCategory);
		}
	}
	
	@Override
	public List<NoteCategory> getAllNoteCategories()
	{
		return Ebean.find(NoteCategory.class).findList();
	}
	
	@Override
	public NoteCategory getNoteCategoryForId(Long noteCategoryId)
	{
		return Ebean.find(NoteCategory.class, noteCategoryId);
	}
	
	@Override
	public void deleteNoteCategory(NoteCategory noteCategory)
	{
		Ebean.delete(noteCategory);
	}
	
	@Override
	public NoteFile getNoteFileForId(Long noteFileId)
	{
		return Ebean.find(NoteFile.class, noteFileId);
	}

	@Override
	public Page<NoteFile> getNoteFilesForPageableAndNote(Pageable pageable, Note note)
	{
		ExpressionList expressionList = Ebean.find(NoteFile.class).where();

		if (note != null) {
			expressionList.eq("note", note);
		}

		expressionList.setFirstRow(new Long(pageable.getOffset()).intValue());
		expressionList.setMaxRows(pageable.getPageSize());

		if (!pageable.getSort().isUnsorted()) {
			Iterator<Sort.Order> orderIterator = pageable.getSort().iterator();
			while (orderIterator.hasNext()) {
				Sort.Order order = orderIterator.next();

				if (!order.getProperty().equals("UNSORTED")) {
					if (order.isAscending()) {
						expressionList.order().asc(order.getProperty());
					}

					if (order.isDescending()) {
						expressionList.order().desc(order.getProperty());
					}
				}
			}
		}

		Page<NoteFile> page = new PageImpl<NoteFile>(expressionList.findList(), pageable, expressionList.findCount());

		return page;
	}

	@Override
	public void deleteNoteFile(NoteFile noteFile)
	{
		Ebean.delete(noteFile);
	}
	
	@Override
	public void saveOrUpdateNoteFile(NoteFile noteFile)
	{
		if (noteFile.getId() != null) {
			Ebean.update(noteFile);
		} else {
			Ebean.save(noteFile);
		}
	}
	
	@Override
	public NoteTimeSeries getNoteTimeSeriesForId(Long noteTimeSeriesId)
	{
		return Ebean.find(NoteTimeSeries.class, noteTimeSeriesId);
	}
	
	@Override
	public void saveOrUpdateNoteTimeSeries(NoteTimeSeries noteTimeSeries)
	{
		if (noteTimeSeries.getId() != null) {
			Ebean.update(noteTimeSeries);
		} else {
			Ebean.save(noteTimeSeries);
		}
	}
	
	@Override
	public void deleteNoteTimeSeries(NoteTimeSeries noteTimeSeries)
	{
		Ebean.delete(noteTimeSeries);
	}
	
	@Override
	public NoteActivity getNoteActivityForId(Long noteActivityId)
	{
		return Ebean.find(NoteActivity.class, noteActivityId);
	}
	
	@Override
	public void saveOrUpdateNoteActivity(NoteActivity noteActivity)
	{
		if (noteActivity.getId() != null) {
			Ebean.update(noteActivity);
		} else {
			Ebean.save(noteActivity);
		}
	}
	
	@Override
	public void deleteNoteActivity(NoteActivity noteActivity)
	{
		Ebean.delete(noteActivity);
	}
	
	@Override
	public NoteSpideringConfiguration getNoteSpideringConfigurationForId(Long noteSpideringConfigurationId)
	{
		return Ebean.find(NoteSpideringConfiguration.class, noteSpideringConfigurationId);
	}
	
	@Override
	public void saveOrUpdateNoteSpideringConfiguration(NoteSpideringConfiguration noteSpideringConfiguration)
	{
		if (noteSpideringConfiguration.getId() != null) {
			Ebean.update(noteSpideringConfiguration);
		} else {
			Ebean.save(noteSpideringConfiguration);
		}
	}
	
	@Override
	public void deleteNoteSpideringConfiguration(NoteSpideringConfiguration noteSpideringConfiguration)
	{
		Ebean.delete(noteSpideringConfiguration);
	}
	
	@Override
	public NotePublishingConfiguration getNotePublishingConfigurationForId(Long notePublishingConfigurationId)
	{
		return Ebean.find(NotePublishingConfiguration.class, notePublishingConfigurationId);
	}
	
	@Override
	public void saveOrUpdateNotePublishingConfiguration(NotePublishingConfiguration notePublishingConfiguration)
	{
		if (notePublishingConfiguration.getId() != null) {
			Ebean.update(notePublishingConfiguration);
		} else {
			Ebean.save(notePublishingConfiguration);
		}
	}
	
	@Override
	public void deleteNotePublishingConfiguration(NotePublishingConfiguration notePublishingConfiguration)
	{
		Ebean.delete(notePublishingConfiguration);
	}
	
	@Override
	public NoteResourceLoaderPluginConfiguration getNoteResourceLoaderPluginConfigurationForId(Long noteResourceLoaderPluginConfigurationId)
	{
		return Ebean.find(NoteResourceLoaderPluginConfiguration.class, noteResourceLoaderPluginConfigurationId);
	}
	
	@Override
	public void saveOrUpdateNoteResourceLoaderPluginConfiguration(NoteResourceLoaderPluginConfiguration noteResourceLoaderPluginConfiguration)
	{
		if (noteResourceLoaderPluginConfiguration.getId() != null) {
			Ebean.update(noteResourceLoaderPluginConfiguration);
		} else {
			Ebean.save(noteResourceLoaderPluginConfiguration);
		}
	}
	
	@Override
	public void deleteNoteResourceLoaderPluginConfiguration(NoteResourceLoaderPluginConfiguration noteResourceLoaderPluginConfiguration)
	{
		Ebean.delete(noteResourceLoaderPluginConfiguration);
	}
	
	@Override
	public NoteNote getNoteNoteForId(Long noteNoteId)
	{
		return Ebean.find(NoteNote.class, noteNoteId);
	}
	
	@Override
	public void saveOrUpdateNoteNote(NoteNote noteNote)
	{
		if (noteNote.getId() != null) {
			Ebean.update(noteNote);
		} else {
			Ebean.save(noteNote);
		}
	}
	
	@Override
	public void deleteNoteNote(NoteNote noteNote)
	{
		Ebean.delete(noteNote);
	}
}
