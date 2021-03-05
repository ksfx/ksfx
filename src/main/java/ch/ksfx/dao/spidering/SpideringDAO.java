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

package ch.ksfx.dao.spidering;

import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.model.spidering.SpideringConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;


public interface SpideringDAO
{
    public Spidering getSpideringForId(Long spideringId);
    public void saveOrUpdate(Spidering spidering);
    public void delete(Spidering spiderings);
    public List<Spidering> getAllSpiderings();
    public List<Spidering> getSpideringsOlderThan(Date date);
    public List<Spidering> getSpideringsOlderThanForSpideringConfiguration(Date date, SpideringConfiguration spideringConfiguration);
    //public GridDataSource getSpideringGridDataSourceForSpideringConfiguration(SpideringConfiguration spideringConfiguration);
    public List<Spidering> getSpideringsForSpideringConfiguration(SpideringConfiguration spideringConfiguration);
    public Page<Spidering> getSpideringsForPageableAndSpideringConfiguration(Pageable pageable, SpideringConfiguration spideringConfiguration);
    public Integer calculateResourcesCount(Spidering spidering);
}
