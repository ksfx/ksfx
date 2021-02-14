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

package ch.ksfx.model.note;

import javax.persistence.*;
import java.util.Date;
import java.util.List;


@Entity
@Table(name = "note")
public class Note
{
	private Long id;
	private String title;
	private NoteCategory noteCategory;
	private String content;
	private Date created;
	private Date lastUpdated;
	private List<NoteTimeSeries> noteTimeSeries;
	private List<NoteActivity> noteActivities;
	private List<NotePublishingConfiguration> notePublishingConfigurations;
	private List<NoteSpideringConfiguration> noteSpideringConfigurations;
	private List<NoteResourceLoaderPluginConfiguration> noteResourceLoaderPluginConfigurations;
	private List<NoteFile> noteFiles;
	private List<NoteNote> noteNotes;
	
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
	
	public String getTitle()
	{
		return title;
	}
	
	public void setTitle(String title)
	{
		this.title = title;
	}
	
	@ManyToOne
	@JoinColumn(name = "note_category")
	public NoteCategory getNoteCategory()
	{
		return noteCategory;
	}
	
	public void setNoteCategory(NoteCategory noteCategory)
	{
		this.noteCategory = noteCategory;
	}
	
	@Lob
	public String getContent()
	{
		return content;
	}
	
	public void setContent(String content)
	{
		this.content = content;
	}
	
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreated()
	{
		return created;
	}
	
	public void setCreated(Date created)
	{
		this.created = created;
	}
	
	@Temporal(TemporalType.TIMESTAMP)
	public Date getLastUpdated()
	{
		return lastUpdated;
	}
	
	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}
	
	@OneToMany(mappedBy = "note")
	public List<NoteTimeSeries> getNoteTimeSeries()
	{
		return noteTimeSeries;
	}
	
	public void setNoteTimeSeries(List<NoteTimeSeries> noteTimeSeries)
	{
		this.noteTimeSeries = noteTimeSeries;
	}
	
	@OneToMany(mappedBy = "note")
	public List<NoteActivity> getNoteActivities()
	{
		return noteActivities;
	}
	
	public void setNoteActivities(List<NoteActivity> noteActivities)
	{
		this.noteActivities = noteActivities;
	}
	
	@OneToMany(mappedBy = "note")
	public List<NotePublishingConfiguration> getNotePublishingConfigurations()
	{
		return notePublishingConfigurations;
	}
	
	public void setNotePublishingConfiguration(List<NotePublishingConfiguration> notePublishingConfigurations)
	{
		this.notePublishingConfigurations = notePublishingConfigurations;
	}
	
	@OneToMany(mappedBy = "note")
	public List<NoteSpideringConfiguration> getNoteSpideringConfigurations()
	{
		return noteSpideringConfigurations;
	}
	
	public void setNoteSpideringConfiguration(List<NoteSpideringConfiguration> noteSpideringConfigurations)
	{
		this.noteSpideringConfigurations = noteSpideringConfigurations;
	}
	
	@OneToMany(mappedBy = "note")
	public List<NoteResourceLoaderPluginConfiguration> getNoteResourceLoaderPluginConfigurations()
	{
		return noteResourceLoaderPluginConfigurations;
	}
	
	public void setNoteResourceLoaderPluginConfigurations(List<NoteResourceLoaderPluginConfiguration> noteResourceLoaderPluginConfigurations)
	{
		this.noteResourceLoaderPluginConfigurations = noteResourceLoaderPluginConfigurations;
	}
	
	@OneToMany(mappedBy = "note")
	public List<NoteFile> getNoteFiles()
	{
		return noteFiles;
	}
	
	public void setNoteFiles(List<NoteFile> noteFiles)
	{
		this.noteFiles = noteFiles;
	}
	
	@OneToMany(mappedBy = "note")
	public List<NoteNote> getNoteNotes()
	{
		return noteNotes;
	}
	
	public void setNoteNotes(List<NoteNote> noteNotes)
	{
		this.noteNotes = noteNotes;
	}
}