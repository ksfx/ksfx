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

import ch.ksfx.model.PublishingConfiguration;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.note.*;
import ch.ksfx.model.spidering.ResourceLoaderPluginConfiguration;
import ch.ksfx.model.spidering.SpideringConfiguration;

import java.util.List;


public interface NoteDAO
{
	public void saveOrUpdateNote(Note note);
	public List<Note> getAllNotes();
	public List<Note> getNotesForNoteCategory(NoteCategory noteCategory);
	public List<Note> searchNotes(String title, NoteCategory noteCategory, TimeSeries timeSeries, Activity activity, PublishingConfiguration publishingConfiguration, SpideringConfiguration spideringConfiguration, ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration, boolean searchMainNotes);
	public Note getNoteForId(Long noteId);
	public void deleteNote(Note note);
	public void saveOrUpdateNoteCategory(NoteCategory noteCategory);
	public List<NoteCategory> getAllNoteCategories();
	public NoteCategory getNoteCategoryForId(Long noteCategoryId);
	public void deleteNoteCategory(NoteCategory noteCategory);
	public NoteFile getNoteFileForId(Long noteFileId);
	public void deleteNoteFile(NoteFile noteFile);
	public void saveOrUpdateNoteFile(NoteFile noteFile);
	
	public NoteTimeSeries getNoteTimeSeriesForId(Long noteTimeSeriesId);
	public void saveOrUpdateNoteTimeSeries(NoteTimeSeries noteTimeSeries);
	public void deleteNoteTimeSeries(NoteTimeSeries notTimeSeries);
	
	public NoteActivity getNoteActivityForId(Long noteActivityId);
	public void saveOrUpdateNoteActivity(NoteActivity noteActivity);
	public void deleteNoteActivity(NoteActivity noteActivity);
	
	public NoteSpideringConfiguration getNoteSpideringConfigurationForId(Long noteSpideringConfigurationId);
	public void saveOrUpdateNoteSpideringConfiguration(NoteSpideringConfiguration noteSpideringConfiguration);
	public void deleteNoteSpideringConfiguration(NoteSpideringConfiguration noteSpideringConfiguration);
	
	public NotePublishingConfiguration getNotePublishingConfigurationForId(Long notePublishingConfigurationId);
	public void saveOrUpdateNotePublishingConfiguration(NotePublishingConfiguration notePublishingConfiguration);
	public void deleteNotePublishingConfiguration(NotePublishingConfiguration notePublishingConfiguration);
	
	public NoteResourceLoaderPluginConfiguration getNoteResourceLoaderPluginConfigurationForId(Long noteResourceLoaderPluginConfigurationId);
	public void saveOrUpdateNoteResourceLoaderPluginConfiguration(NoteResourceLoaderPluginConfiguration noteResourceLoaderPluginConfiguration);
	public void deleteNoteResourceLoaderPluginConfiguration(NoteResourceLoaderPluginConfiguration noteResourceLoaderPluginConfiguration);
	
	public NoteNote getNoteNoteForId(Long noteNoteId);
	public void saveOrUpdateNoteNote(NoteNote noteNote);
	public void deleteNoteNote(NoteNote noteNote);
}