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

import ch.ksfx.dao.spidering.ResultVerifierConfigurationDAO;
import ch.ksfx.model.spidering.ResultVerifierConfiguration;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanResultVerifierConfigurationDAO implements ResultVerifierConfigurationDAO
{
    @Override
    public void saveOrUpdate(ResultVerifierConfiguration resultVerifierConfiguration)
    {
        if (resultVerifierConfiguration.getId() != null) {
            Ebean.update(resultVerifierConfiguration);
        } else {
            Ebean.save(resultVerifierConfiguration);
        }
    }

    @Override
    public List<ResultVerifierConfiguration> getAllResultVerifierConfigurations()
    {
        return Ebean.find(ResultVerifierConfiguration.class).findList();
    }

    @Override
    public ResultVerifierConfiguration getResultVerifierConfigurationForId(Long resultVerifierConfigurationId)
    {
        return Ebean.find(ResultVerifierConfiguration.class, resultVerifierConfigurationId);
    }

    @Override
    public void deleteResultVerifierConfiguration(ResultVerifierConfiguration resultVerifierConfiguration)
    {
        Ebean.delete(resultVerifierConfiguration);
    }
}
