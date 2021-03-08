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

import ch.ksfx.model.spidering.Resource;
import ch.ksfx.model.spidering.Result;
import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.model.spidering.SpideringConfiguration;
import io.ebean.QueryIterator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface ResourceDAO
{
    public void saveOrUpdate(Resource resource);
    public List<Resource> getAllResources();
    public List<Resource> getResourcesForSpideringAndDepth(Spidering spidering, Integer depth);
    public QueryIterator<Resource> getResourcesForSpideringAndDepthIterator(Spidering spidering, Integer depth);
    public List getResourcesForSpideringAndDepthIds(Spidering spidering, Integer depth);
    public List<Resource> getChildResources(Resource resource);
//    public GridDataSource getResourceGridDataSourceForSpidering(Spidering spidering);
    public Page<Resource> getResourcesForPageableAndSpidering(Pageable pageable, Spidering spidering);
    public Resource getResourceForId(Long resourceId);
    public void deleteResource(Resource resource);
}
