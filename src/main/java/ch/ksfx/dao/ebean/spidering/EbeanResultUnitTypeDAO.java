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

import ch.ksfx.dao.spidering.ResultUnitTypeDAO;
import ch.ksfx.model.spidering.ResultUnitType;
import io.ebean.Ebean;

import java.util.List;


public class EbeanResultUnitTypeDAO implements ResultUnitTypeDAO
{
    @Override
    public void saveOrUpdate(ResultUnitType resultUnitType)
    {
        if (resultUnitType.getId() != null) {
            Ebean.update(resultUnitType);
        } else {
            Ebean.save(resultUnitType);
        }
    }

    @Override
    public List<ResultUnitType> getAllResultUnitTypes()
    {
        return Ebean.find(ResultUnitType.class).findList();
    }

    @Override
    public ResultUnitType getResultUnitTypeForId(Long resultUnitTypeId)
    {
        return Ebean.find(ResultUnitType.class, resultUnitTypeId);
    }

    @Override
    public void deleteResultUnitType(ResultUnitType resultUnitType)
    {
        Ebean.delete(resultUnitType);
    }
}
