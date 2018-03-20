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

import ch.ksfx.dao.*;
import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.dao.cassandra.CassandraObservationDAO;
import ch.ksfx.dao.ebean.*;
import ch.ksfx.dao.ebean.activity.EbeanActivityDAO;
import ch.ksfx.dao.ebean.activity.EbeanActivityInstanceDAO;
import ch.ksfx.dao.ebean.publishing.EbeanPublishingResourceDAO;
import ch.ksfx.dao.ebean.publishing.EbeanPublishingSharedDataDAO;
import ch.ksfx.dao.ebean.spidering.*;
import ch.ksfx.dao.publishing.PublishingResourceDAO;
import ch.ksfx.dao.publishing.PublishingSharedDataDAO;
import ch.ksfx.dao.spidering.*;
import ch.ksfx.util.mail.MailSender;
import ch.ksfx.web.services.activity.ActivityInstanceRunner;
import ch.ksfx.web.services.chart.ObservationChartGenerator;
import ch.ksfx.web.services.configurationdatabase.ConfigurationDatabaseProvider;
import ch.ksfx.web.services.configurationdatabase.impl.MockDatabaseProvider;
import ch.ksfx.web.services.dbmigration.DbMigration;
import ch.ksfx.web.services.dbmigration.impl.DbMigrationImpl;
import ch.ksfx.web.services.logger.SystemLogger;
import ch.ksfx.web.services.lucene.IndexService;
import ch.ksfx.web.services.scheduler.SchedulerService;
import ch.ksfx.web.services.scheduler.impl.SchedulerServiceImpl;
import ch.ksfx.web.services.seriesbrowser.SeriesBrowser;
import ch.ksfx.web.services.spidering.ParsingRunner;
import ch.ksfx.web.services.spidering.SpideringRunner;
import ch.ksfx.web.services.systemenvironment.SystemEnvironment;
import ch.ksfx.web.services.systemenvironment.impl.MockSystemEnvironmentImpl;
import ch.ksfx.web.services.version.Version;
import ch.ksfx.web.services.version.impl.VersionImpl;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ScopeConstants;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.ImportModule;
import org.apache.tapestry5.ioc.annotations.Startup;
import org.apache.tapestry5.services.*;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@ImportModule({TestSecurityModule.class, SitemapModule.class})
public class TestAppModule
{
    @Startup
    public static void initKsfx(SystemEnvironment systemEnvironment, Logger logger, ConfigurationDatabaseProvider configurationDatabaseProvider, SchedulerService schedulerService, DbMigration dbMigration)
    {
        logger.info("Starting KSFX with home directory: " + systemEnvironment.getApplicationHomeDirectoryPath());

        try {
            configurationDatabaseProvider.provideConfigurationDatabase();
            dbMigration.updateAllChangelogs();
            schedulerService.initializeScheduler();
        } catch (Exception e) {
            logger.error("Error while starting basic services",e);
        }
    }

    public static void bind(ServiceBinder binder)
    {
        binder.bind(ConfigurationDatabaseProvider.class, MockDatabaseProvider.class);
        binder.bind(DbMigration.class, DbMigrationImpl.class);
        binder.bind(SystemEnvironment.class, MockSystemEnvironmentImpl.class);
        binder.bind(Version.class, VersionImpl.class);
        binder.bind(SchedulerService.class, SchedulerServiceImpl.class).scope(ScopeConstants.DEFAULT);
        //binder.bind(SchedulerService.class).scope(ScopeConstants.DEFAULT);
        binder.bind(MailSender.class);
        binder.bind(SystemLogger.class);

        binder.bind(ActivityDAO.class, EbeanActivityDAO.class);
        binder.bind(ActivityInstanceDAO.class, EbeanActivityInstanceDAO.class);
        //binder.bind(AssetDAO.class, EbeanAssetDAO.class);
        //binder.bind(SimulationOptimizationParameterDAO.class, EbeanSimulationOptimizationParameterDAO.class);
        //binder.bind(TradeDAO.class, EbeanTradeDAO.class);
        binder.bind(CategoryDAO.class, EbeanCategoryDAO.class);
        binder.bind(PublishingResourceDAO.class, EbeanPublishingResourceDAO.class);
        binder.bind(PublishingSharedDataDAO.class, EbeanPublishingSharedDataDAO.class);
        //binder.bind(DecisionDAO.class, EbeanDecisionDAO.class);
        //binder.bind(DecisionStrategyTemplateDAO.class, EbeanDecisionStrategyTemplateDAO.class);
        //binder.bind(DecisionStrategyDAO.class, EbeanDecisionStrategyDAO.class);
        binder.bind(GenericDataStoreDAO.class, EbeanGenericDataStoreDAO.class);
        //binder.bind(CodeExecuterDAO.class, EbeanCodeExecuterDAO.class);
        binder.bind(LogMessageDAO.class, EbeanLogMessageDAO.class);
        //binder.bind(MarketOrderDAO.class, EbeanMarketOrderDAO.class);
        binder.bind(NoteDAO.class, EbeanNoteDAO.class);
        binder.bind(ResourceLoaderPluginConfigurationDAO.class, EbeanResourceLoaderPluginConfigurationDAO.class);
        binder.bind(ResultUnitTypeDAO.class, EbeanResultUnitTypeDAO.class);
        binder.bind(ResultUnitModifierConfigurationDAO.class, EbeanResultUnitModifierConfigurationDAO.class);
        binder.bind(ResultUnitConfigurationModifiersDAO.class, EbeanResultUnitConfigurationModifiersDAO.class);
        binder.bind(ResultUnitDAO.class, EbeanResultUnitDAO.class);
        binder.bind(SpideringConfigurationDAO.class, EbeanSpideringConfigurationDAO.class);
        binder.bind(SpideringPostActivityDAO.class, EbeanSpideringPostActivityDAO.class);
        //binder.bind(InfrastructureDAO.class, EbeanInfrastructureDAO.class);
        binder.bind(SeriesBrowser.class).scope(ScopeConstants.DEFAULT);
        binder.bind(ObjectLocatorService.class);
        binder.bind(IndexService.class);
        binder.bind(ObservationDAO.class, CassandraObservationDAO.class);
        binder.bind(ResourceConfigurationDAO.class, EbeanResourceConfigurationDAO.class);
        //binder.bind(SimulationDAO.class, EbeanSimulationDAO.class);
        binder.bind(UrlFragmentDAO.class, EbeanUrlFragmentDAO.class);
        binder.bind(PagingUrlFragmentDAO.class, EbeanPagingUrlFragmentDAO.class);
        binder.bind(ResultUnitConfigurationDAO.class, EbeanResultUnitConfigurationDAO.class);
        binder.bind(SpideringDAO.class, EbeanSpideringDAO.class);
        binder.bind(ResourceDAO.class, EbeanResourceDAO.class);
        binder.bind(ResultDAO.class, EbeanResultDAO.class);
        binder.bind(ResultVerifierConfigurationDAO.class, EbeanResultVerifierConfigurationDAO.class);
        //binder.bind(SimulationRawDataDAO.class, EbeanSimulationRawDataDAO.class);
        //binder.bind(SimulatedDecisionDAO.class, EbeanSimulatedDecisionDAO.class);
        //binder.bind(SimulatedMarketOrderDAO.class, EbeanSimulatedMarketOrderDAO.class);
        //binder.bind(SimulatedTradeDAO.class, EbeanSimulatedTradeDAO.class);
        //binder.bind(SimulatedAssetDecisionStrategyTemplateDAO.class, EbeanSimulatedAssetDecisionStrategyTemplateDAO.class);
        binder.bind(ActivityInstanceRunner.class);
        //binder.bind(SimulationRunner.class);
        binder.bind(TimeSeriesDAO.class, EbeanTimeSeriesDAO.class);
        binder.bind(SpideringRunner.class);
        binder.bind(ParsingRunner.class);
        //binder.bind(OptimizationRunner.class);
        //binder.bind(TradeSignalDAO.class, EbeanTradeSignalDAO.class);
        //binder.bind(ChartGenerator.class);
        binder.bind(ObservationChartGenerator.class);
        binder.bind(PublishingConfigurationDAO.class, EbeanPublishingConfigurationDAO.class);
    }

    //Mock a Servlet request...
    public static void contributeRequestHandler(OrderedConfiguration<RequestFilter> config, final RequestGlobals requestGlobals)
    {
        RequestFilter filter = new RequestFilter()
        {
            public boolean service(Request request, Response response, RequestHandler handler) throws IOException {
                requestGlobals.storeServletRequestResponse(Mockito.mock(HttpServletRequest.class), Mockito.mock(HttpServletResponse.class));
                return handler.service(request, response);
            }
        };
        config.add("EnsureNonNullHttpRequestAndResponse", filter, "before:*");
    }

    public static void contributeApplicationDefaults(
            MappedConfiguration<String, String> configuration)
    {
        configuration.add(SymbolConstants.SUPPORTED_LOCALES, "en_GB");
        configuration.add(SymbolConstants.PRODUCTION_MODE, "false");
        configuration.add("tapestry.default-cookie-max-age", "31536000");
    }
}
