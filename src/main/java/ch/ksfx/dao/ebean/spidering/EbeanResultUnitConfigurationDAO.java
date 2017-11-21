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

import ch.ksfx.dao.spidering.ResultUnitConfigurationDAO;
import ch.ksfx.model.spidering.ResultUnitConfiguration;
import ch.ksfx.model.spidering.ResultUnitFinder;
import ch.ksfx.model.spidering.ResultUnitType;
import io.ebean.Ebean;

import java.util.List;


public class EbeanResultUnitConfigurationDAO implements ResultUnitConfigurationDAO
{
    @Override
    public ResultUnitConfiguration getResultUnitConfigurationForId(Long resultUnitConfigurationId)
    {
        return Ebean.find(ResultUnitConfiguration.class, resultUnitConfigurationId);
    }

    @Override
    public void saveOrUpdate(ResultUnitConfiguration resultUnitConfiguration)
    {
        if (resultUnitConfiguration.getId() != null) {
            Ebean.update(resultUnitConfiguration);
        } else {
            Ebean.save(resultUnitConfiguration);
        }
    }

    @Override
    public void delete(ResultUnitConfiguration resultUnitConfiguration)
    {
        Ebean.delete(resultUnitConfiguration);
    }

    @Override
    public List<ResultUnitFinder> getAllResultUnitFinders()
    {
        return Ebean.find(ResultUnitFinder.class).findList();
    }

    @Override
    public List<ResultUnitType> getAllResultUnitTypes()
    {
        return Ebean.find(ResultUnitType.class).findList();
    }
}
