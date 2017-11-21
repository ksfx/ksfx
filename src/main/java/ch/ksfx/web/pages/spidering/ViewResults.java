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

import ch.ksfx.dao.spidering.ResultDAO;
import ch.ksfx.dao.spidering.SpideringConfigurationDAO;
import ch.ksfx.dao.spidering.SpideringDAO;
import ch.ksfx.model.spidering.ResourceConfiguration;
import ch.ksfx.model.spidering.Result;
import ch.ksfx.model.spidering.ResultUnit;
import ch.ksfx.model.spidering.SpideringConfiguration;
import ch.ksfx.web.services.spidering.ParsingRunner;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;


public class ViewResults
{
    @Inject
    private SpideringConfigurationDAO spideringConfigurationDAO;

    @Inject
    private SpideringDAO spideringDAO;

    @Inject
    private ParsingRunner parsingRunner;

    @Inject
    private ResultDAO resultDAO;

    @Property
    private SpideringConfiguration spideringConfiguration;

    @Property
    private Result result;
    
    @Property
    @Persist
    private boolean filterInvalidResults;

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long spideringConfigurationId)
    {
        spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
    }

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {

    }

    public GridDataSource getAllResultsForSpideringConfiguration()
    {
        return resultDAO.getResultGridDataSourceForSpideringConfiguration(spideringConfiguration, filterInvalidResults);
    }

    public String getFormattedData(Result result)
    {
        String data = "";
        data += "<table><tr>";
        for (ResultUnit resultUnit : result.getResultUnits()) {
            data += "<td valign='top'>" + StringEscapeUtils.escapeHtml(resultUnit.getValue()) + "</td>";
        }
        data += "</tr></table>";

        return data;
    }

    public Integer getResultUnitConfigurationCount()
    {
        Integer cnt = 0;

        for (ResourceConfiguration resourceConfiguration : spideringConfiguration.getResourceConfigurations()) {
            if (resourceConfiguration.getResultUnitConfigurations() != null) {
                cnt += resourceConfiguration.getResultUnitConfigurations().size();
            }
        }

        return cnt;
    }

    public void onActionFromClearResults()
    {
        resultDAO.deleteResultsForSpideringConfiguration(spideringConfiguration);
    }

    public Long onPassivate()
    {
        if (spideringConfiguration != null) {
            return spideringConfiguration.getId();
        }

        return null;
    }
}
