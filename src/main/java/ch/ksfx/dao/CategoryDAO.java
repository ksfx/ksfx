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

import ch.ksfx.model.Category;

import java.util.List;


public interface CategoryDAO
{
	public void saveOrUpdateCategory(Category category);
	public List<Category> getAllCategories();
	public Category getCategoryForId(Long categoryId);
	public Category getCategoryForLocator(String locator);
	public void deleteCategory(Category category);
}
 

