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

package ch.ksfx.web.services.configurationdatabase.impl;

import ch.ksfx.configuration.SetupTest;
import ch.ksfx.web.services.configurationdatabase.ConfigurationDatabaseProvider;
import ch.ksfx.web.services.systemenvironment.SystemEnvironment;
import io.ebean.Ebean;
import io.ebean.EbeanServerFactory;
import io.ebean.SqlUpdate;
import io.ebean.annotation.Sql;
import io.ebean.annotation.TxIsolation;
import io.ebean.config.ServerConfig;
import io.ebean.config.UnderscoreNamingConvention;
import org.avaje.datasource.DataSourceConfig;


public class MockDatabaseProvider implements ConfigurationDatabaseProvider
{
    private SystemEnvironment systemEnvironment;

    public MockDatabaseProvider(SystemEnvironment systemEnvironment)
    {
        this.systemEnvironment = systemEnvironment;
    }

    public void provideConfigurationDatabase()
    {
        systemEnvironment.getMainConfiguration();

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setName("default");
        serverConfig.setDdlGenerate(false);
        serverConfig.setDdlRun(false);
        serverConfig.setNamingConvention(new UnderscoreNamingConvention());

        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setDriver("com.mysql.jdbc.Driver");
        dataSourceConfig.setUsername(SetupTest.testDatabaseUser);
        dataSourceConfig.setPassword(SetupTest.testDatabasePassword);
        dataSourceConfig.setUrl(SetupTest.testDatabaseConnectionString);
        dataSourceConfig.setMinConnections(1);
        dataSourceConfig.setMaxConnections(2000);
        dataSourceConfig.setHeartbeatSql("Select 1");
        dataSourceConfig.setIsolationLevel(TxIsolation.READ_COMMITED.getLevel());

        serverConfig.setDataSourceConfig(dataSourceConfig);
        serverConfig.setDefaultServer(true);
        serverConfig.setRegister(true);

        EbeanServerFactory.create(serverConfig);

        if (SetupTest.testDatabaseRecreate) {
            SqlUpdate drop = Ebean.createSqlUpdate("DROP DATABASE IF EXISTS " + SetupTest.testDatabaseName);
            Ebean.execute(drop);

            SqlUpdate create = Ebean.createSqlUpdate("CREATE DATABASE " + SetupTest.testDatabaseName);
            Ebean.execute(create);

            SqlUpdate use = Ebean.createSqlUpdate("USE " + SetupTest.testDatabaseName);
            Ebean.execute(use);
        }
    }
}
