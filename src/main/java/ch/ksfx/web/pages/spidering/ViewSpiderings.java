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

import ch.ksfx.dao.spidering.SpideringConfigurationDAO;
import ch.ksfx.dao.spidering.SpideringDAO;
import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.model.spidering.SpideringConfiguration;
import ch.ksfx.web.services.spidering.ParsingRunner;
import ch.ksfx.web.services.spidering.SpideringRunner;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;


public class ViewSpiderings
{
    @Inject
    private SpideringConfigurationDAO spideringConfigurationDAO;

    @Inject
    private SpideringDAO spideringDAO;

    @Inject
    private ParsingRunner parsingRunner;

    @Inject
    private SpideringRunner spideringRunner;

    @Property
    private SpideringConfiguration spideringConfiguration;

    @Property
    private Spidering spidering;

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long spideringConfigurationId)
    {
        spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
    }

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {

    }

    public boolean getParsingRunning()
    {
        return parsingRunner.isParsingRunning(spidering);
    }

    public boolean getSpideringRunning()
    {
        return spideringRunner.isSpideringRunning(spidering);
    }

    public GridDataSource getAllSpideringsForConfiguration()
    {
        return spideringDAO.getSpideringGridDataSourceForSpideringConfiguration(spideringConfiguration);
    }

    public void onActionFromDelete(Long spideringId)
    {
        spideringDAO.delete(spideringDAO.getSpideringForId(spideringId));
    }

    public Integer getResourcesCount(Long spideringId)
    {
        return spideringDAO.calculateResourcesCount(spideringDAO.getSpideringForId(spideringId));
    }

    public void onActionFromRunParsing(Long spideringId)
    {
        parsingRunner.runParsing(spideringDAO.getSpideringForId(spideringId));
    }

    public void onActionFromTerminateParsing(Long spideringId)
    {
        parsingRunner.terminateParsing(spideringDAO.getSpideringForId(spideringId));
    }

    public void onActionFromTerminateSpidering(Long spideringId)
    {
        spideringRunner.terminateSpidering(spideringDAO.getSpideringForId(spideringId));
    }

    public Long onPassivate()
    {
        if (spideringConfiguration != null) {
            return spideringConfiguration.getId();
        }

        return null;
    }
}
