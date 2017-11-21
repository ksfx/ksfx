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

package ch.ksfx.web.pages.activity;

import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.model.activity.ActivityCategory;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;

import java.util.List;


@Secured({"ROLE_ADMIN"})
@Import(library = "context:scripts/activity.js")
public class ActivityCategoryIndex
{
    @Inject
    private ActivityDAO activityDAO;

    @Property
    private ActivityCategory activityCategory;

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {

    }

    public List<ActivityCategory> getAllActivityCategories()
    {
        return activityDAO.getAllActivityCategories();
    }

    public void onActionFromDelete(Long activityCategoryId)
    {
        activityDAO.deleteActivityCategory(activityDAO.getActivityCategoryForId(activityCategoryId));
    }
}
