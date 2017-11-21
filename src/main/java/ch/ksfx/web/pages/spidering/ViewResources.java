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

package ch.ksfx.web.pages.spidering;

import ch.ksfx.dao.spidering.ResourceDAO;
import ch.ksfx.dao.spidering.ResultDAO;
import ch.ksfx.dao.spidering.SpideringDAO;
import ch.ksfx.model.spidering.Resource;
import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.web.services.spidering.ParsingRunner;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.util.TextStreamResponse;
import org.springframework.security.access.annotation.Secured;

/**
 * Created by Kejo on 26.10.2014.
 */
public class ViewResources
{
    @Inject
    private SpideringDAO spideringDAO;

    @Inject
    private ResourceDAO resourceDAO;

    @Inject
    private ParsingRunner parsingRunner;

    @Inject
    private ResultDAO resultDAO;

    @Property
    private Spidering spidering;

    @Property
    private Resource resource;

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long spideringId)
    {
        spidering = spideringDAO.getSpideringForId(spideringId);
    }

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {

    }

    public StreamResponse onActionFromViewResource(Long resourceId)
    {
        Resource resource = resourceDAO.getResourceForId(resourceId);

        return new TextStreamResponse(resource.getMimeType(), "UTF-8",resource.getContent());
    }

    public GridDataSource getAllResourcesForSpidering()
    {
        return resourceDAO.getResourceGridDataSourceForSpidering(spidering);
    }

    public Long onPassivate()
    {
        if (spidering != null) {
            return spidering.getId();
        }

        return null;
    }
}
