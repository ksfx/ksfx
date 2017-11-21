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

import ch.ksfx.dao.CategoryDAO;
import ch.ksfx.model.Category;
import io.ebean.Ebean;

import java.util.List;


public class EbeanCategoryDAO implements CategoryDAO
{
	@Override
	public void saveOrUpdateCategory(Category category)
	{
		if (category.getId() != null) {
			Ebean.update(category);
		} else {
			Ebean.save(category);
		}
	}
	
	@Override
	public List<Category> getAllCategories()
	{
		return Ebean.find(Category.class).findList();
	}
	
	@Override
	public Category getCategoryForId(Long categoryId)
	{
		return Ebean.find(Category.class, categoryId);
	}
	
	@Override
	public Category getCategoryForLocator(String locator)
	{
		return Ebean.find(Category.class).where().eq("locator", locator).findUnique();
	}
	
	@Override
	public void deleteCategory(Category category)
	{
		Ebean.delete(category);
	}
}
