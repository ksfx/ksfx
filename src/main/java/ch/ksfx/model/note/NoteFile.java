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

@Entity
@Table(name = "note_file")
public class NoteFile
{
	private Long id;
	private byte[] fileContent;
	private String fileName;
	private String contentType;
	private String comment;
	private Note note;
	
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
	
	@Lob
	public byte[] getFileContent()
	{
		return fileContent;
	}
	
	public void setFileContent(byte[] fileContent)
	{
		this.fileContent = fileContent;
	}
	
	public String getFileName()
	{
		return fileName;
	}
	
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}
	
	public String getContentType()
	{
		return contentType;
	}
	
	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}
	
	public String getComment()
	{
		return comment;
	}
	
	public void setComment(String comment)
	{
		this.comment = comment;
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
}


