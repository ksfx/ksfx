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

package ch.ksfx.dao.ebean.spidering;

import ch.ksfx.dao.spidering.SpideringPostActivityDAO;
import ch.ksfx.model.spidering.SpideringPostActivity;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanSpideringPostActivityDAO implements SpideringPostActivityDAO
{
    @Override
    public void saveOrUpdate(SpideringPostActivity spideringPostActivity)
    {
        if (spideringPostActivity.getId() != null) {
            Ebean.update(spideringPostActivity);
        } else {
            Ebean.save(spideringPostActivity);
        }
    }

    @Override
    public List<SpideringPostActivity> getAllSpideringPostActivities()
    {
        return Ebean.find(SpideringPostActivity.class).findList();
    }

    @Override
    public SpideringPostActivity getSpideringPostActivityForId(Long spideringPostActivityId)
    {
        return Ebean.find(SpideringPostActivity.class, spideringPostActivityId);
    }

    @Override
    public void deleteSpideringPostActivity(SpideringPostActivity spideringPostActivity)
    {
        Ebean.delete(spideringPostActivity);
    }
}
