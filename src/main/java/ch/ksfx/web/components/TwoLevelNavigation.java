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

package ch.ksfx.web.components;

import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.web.services.sitemap.NavigationHelper;
import ch.ksfx.web.services.sitemap.Page;
import ch.ksfx.web.services.sitemap.Sitemap;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;

import java.util.List;


public class TwoLevelNavigation
{
    @Property
    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    private String id;

    @Property
    private Page page;

    @Property
    private Page activeFirstLevelPage;

    @Inject
    private Sitemap sitemap;

    @Inject
    private ComponentResources resources;

    @Inject
    private TimeSeriesDAO timeSeriesDAO;

    void setupRender()
    {
        for (Page page : getFirstLevelPages()) {
            if (isActivePage(page)) {
                this.activeFirstLevelPage = page;
            }
        }
    }

    public List<Page> getFirstLevelPages()
    {
        return sitemap.getFirstLevelPages();
    }

    public boolean isActivePage(Page page)
    {
        return NavigationHelper.isActivePage(page, resources.getPageName());
    }

    public String getCssClass()
    {
        if (isActivePage(page)) {
            return "active";
        }
        return null;
    }

    public Long getFirstTimeSeriesId()
    {
        TimeSeries ts = timeSeriesDAO.getFirstTimeSeriesInDatabase();

        if (ts != null) {
            return ts.getId();
        }

        return null;
    }
}
