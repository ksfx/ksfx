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

import ch.ksfx.dao.spidering.PagingUrlFragmentDAO;
import ch.ksfx.model.spidering.PagingUrlFragment;
import ch.ksfx.model.spidering.UrlFragmentFinder;
import io.ebean.Ebean;

import java.util.List;


public class EbeanPagingUrlFragmentDAO implements PagingUrlFragmentDAO
{
    @Override
    public PagingUrlFragment getPagingUrlFragmentForId(Long pagingUrlFragmentId)
    {
        return Ebean.find(PagingUrlFragment.class, pagingUrlFragmentId);
    }

    @Override
    public void saveOrUpdate(PagingUrlFragment pagingUrlFragment)
    {
        if (pagingUrlFragment.getId() != null) {
            Ebean.update(pagingUrlFragment);
        } else {
            Ebean.save(pagingUrlFragment);
        }
    }

    @Override
    public void delete(PagingUrlFragment pagingUrlFragment)
    {
        Ebean.delete(pagingUrlFragment);
    }

    @Override
    public List<UrlFragmentFinder> getAllUrlFragmentFinders()
    {
        return Ebean.find(UrlFragmentFinder.class).findList();
    }
}
