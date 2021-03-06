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
import ch.ksfx.model.note.NoteCategory;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;

import java.util.List;


@Secured({"ROLE_ADMIN"})
public class NoteCategoryIndex
{
    @Property
    private NoteCategory noteCategory;

    @Inject
    private NoteDAO noteDAO;
	
	@Secured({"ROLE_ADMIN"})
	public void onActivate()
	{
		
	}

    public List<NoteCategory> getAllNoteCategories()
    {
        return noteDAO.getAllNoteCategories();
    }

    public void onActionFromDelete(Long noteCategoryId)
    {
        NoteCategory noteCategory = noteDAO.getNoteCategoryForId(noteCategoryId);
        noteDAO.deleteNoteCategory(noteCategory);
    }
}

