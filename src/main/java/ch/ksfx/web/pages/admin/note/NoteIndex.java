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
import ch.ksfx.model.note.Note;
import ch.ksfx.model.note.NoteCategory;
import ch.ksfx.util.GenericSelectModel;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.PropertyAccess;
import org.springframework.security.access.annotation.Secured;

import java.util.List;


@Secured({"ROLE_ADMIN"})
public class NoteIndex
{
    @Property
    private Note note;

    @Inject
    private NoteDAO noteDAO;
    
    @Inject
    private PropertyAccess propertyAccess;
    
    @Property
    @Persist
    private NoteCategory noteCategory;
    
    @Property
    private GenericSelectModel<NoteCategory> allNoteCategories;
	
	@Secured({"ROLE_ADMIN"})
	public void onActivate()
	{
        allNoteCategories = new GenericSelectModel<NoteCategory>(noteDAO.getAllNoteCategories(),NoteCategory.class,"name","id",propertyAccess);
	}

    public List<Note> getAllNotes()
    {
        return noteDAO.getNotesForNoteCategory(noteCategory);
    }

    public void onActionFromDelete(Long noteId)
    {
        Note note = noteDAO.getNoteForId(noteId);
        noteDAO.deleteNote(note);
    }
}

