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

package ch.ksfx.web.services;

import ch.ksfx.web.services.sitemap.Page;
import ch.ksfx.web.services.sitemap.PageProvider;
import ch.ksfx.web.services.sitemap.Sitemap;
import ch.ksfx.web.services.sitemap.StaticPageProvider;
import ch.ksfx.web.services.sitemap.impl.SitemapImpl;
import org.apache.tapestry5.ioc.OrderedConfiguration;

import java.util.List;


public class SitemapModule
{
    public static Sitemap buildSitemap(List<PageProvider> pageProviders)
    {
        return new SitemapImpl("KSFX - Data Analysis Platform", pageProviders);
    }

    public static void contributeSitemap(OrderedConfiguration<PageProvider> configuration,
            PageProvider pageProvider)
    {
        configuration.add("main", pageProvider);
    }

    public static PageProvider build()
    {
        StaticPageProvider pageProvider = new StaticPageProvider();

        Page dashboard = new Page("index", "Dashboard");
        //dashboard.addChildPage(new Page("legacyindex", "OLD Dashboard"));

        pageProvider.addFirstLevelPage(dashboard);
        //pageProvider.addFirstLevelPage(new Page("simulator", "Simulator"));

        Page activityPage = new Page("activity/activityIndex", "Activities");
        activityPage.addChildPage(new Page("activity/ViewUnapprovedActivityInstances", "Activities Waiting for Approval"));
        activityPage.addChildPage(new Page("activity/activityCategoryIndex", "Activity Categories"));
        //activityPage.addChildPage(new Page("signalTrading", "Signal Trading"));
        pageProvider.addFirstLevelPage(activityPage);

        Page publishingPage = new Page("publishing/Index", "Reports");
        publishingPage.addChildPage(new Page("publishing/publishingCategoryIndex", "Report Categories"));
        pageProvider.addFirstLevelPage(publishingPage);

        Page spideringPage = new Page("spidering/spideringConfigurationIndex", "Information Retrieval");
        spideringPage.addChildPage(new Page("spidering/resultUnitTypeIndex", "Result Units"));
        spideringPage.addChildPage(new Page("spidering/resultUnitModifierConfigurationIndex", "Result Unit Modifier Configurations"));
        spideringPage.addChildPage(new Page("spidering/resultVerifierConfigurationIndex", "Result Verifier Configurations"));
        spideringPage.addChildPage(new Page("spidering/resourceLoaderPluginConfigurationIndex", "Resource Loader Plugin Configurations"));

        Page adminPage = new Page("admin", "Admin");
        adminPage.addChildPage(new Page("admin/timeseries/timeSeriesIndex", "Time Series"));
        adminPage.addChildPage(new Page("admin/category/categoryIndex", "Categories"));
        adminPage.addChildPage(new Page("admin/datastore/dataStoreIndex", "Generic Data Store"));
        adminPage.addChildPage(new Page("admin/note/noteIndex", "Notes"));
        adminPage.addChildPage(new Page("admin/systemlog/systemLogIndex", "System Logs"));
        adminPage.addChildPage(new Page("admin/scheduler/schedulerIndex", "Scheduler"));
        adminPage.addChildPage(new Page("admin/user/manageuser", "Change Admin Password"));
        adminPage.addChildPage(new Page("admin/loginpageinformation/manageloginpageinformation", "Login Page Information"));

        pageProvider.addFirstLevelPage(spideringPage);
        pageProvider.addFirstLevelPage(adminPage);

        return pageProvider;
    }
}
