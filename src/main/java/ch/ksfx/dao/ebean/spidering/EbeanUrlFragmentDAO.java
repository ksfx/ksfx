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

import ch.ksfx.dao.spidering.UrlFragmentDAO;
import ch.ksfx.model.spidering.UrlFragment;
import ch.ksfx.model.spidering.UrlFragmentFinder;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanUrlFragmentDAO implements UrlFragmentDAO
{
    @Override
    public UrlFragment getUrlFragmentForId(Long urlFragmentId)
    {
        return Ebean.find(UrlFragment.class, urlFragmentId);
    }

    @Override
    public void saveOrUpdate(UrlFragment urlFragment)
    {
        if (urlFragment.getId() != null) {
            Ebean.update(urlFragment);
        } else {
            Ebean.save(urlFragment);
        }
    }

    @Override
    public void delete(UrlFragment urlFragment)
    {
        Ebean.delete(urlFragment);
    }

    @Override
    public List<UrlFragmentFinder> getAllUrlFragmentFinders()
    {
        return Ebean.find(UrlFragmentFinder.class).findList();
    }
}
