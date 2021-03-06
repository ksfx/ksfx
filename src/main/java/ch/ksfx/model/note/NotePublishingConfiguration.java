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

import ch.ksfx.model.PublishingConfiguration;

import javax.persistence.*;


@Entity
@Table(name = "note_publishing_configuration")
public class NotePublishingConfiguration
{
	private Long id;
	private Note note;
	private PublishingConfiguration publishingConfiguration;
	private boolean mainNote;
	
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
	
	@ManyToOne
	@JoinColumn(name = "note")
	public Note getNote()
	{
		return note;
	}
	
	public void setNote(Note note)
	{
		this.note = note;
	}
	
	@ManyToOne
	@JoinColumn(name = "publishing_configuration")
	public PublishingConfiguration getPublishingConfiguration()
	{
		return publishingConfiguration;
	}
	
	public void setPublishingConfiguration(PublishingConfiguration publishingConfiguration)
	{
		this.publishingConfiguration = publishingConfiguration;
	}
	
	public boolean getMainNote()
	{
		return mainNote;
	}
	
	public void setMainNote(boolean mainNote)
	{
		this.mainNote = mainNote;
	}
}

