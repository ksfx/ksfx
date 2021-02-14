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

package ch.ksfx.services.configurationdatabase;

import ch.ksfx.services.SystemEnvironment;
import ch.ksfx.services.configurationdatabase.EbeanSimpleDataSource;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import io.ebean.EbeanServerFactory;
import io.ebean.annotation.TxIsolation;
import io.ebean.config.ServerConfig;
import io.ebean.config.UnderscoreNamingConvention;
import liquibase.Liquibase;
import liquibase.integration.spring.SpringLiquibase;
import org.avaje.datasource.DataSourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationDatabaseProvider
{
    @Autowired
    private SystemEnvironment systemEnvironment;

    public ConfigurationDatabaseProvider(SystemEnvironment systemEnvironment)
    {
        this.systemEnvironment = systemEnvironment;
        provideConfigurationDatabase();
    }

    public void provideConfigurationDatabase()
    {
        System.out.println("Providing Service Database");

        systemEnvironment.getMainConfiguration();

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setName("default");
        serverConfig.setDdlGenerate(false);
        serverConfig.setDdlRun(false);
        serverConfig.setNamingConvention(new UnderscoreNamingConvention());

        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setDriver(systemEnvironment.getMainConfiguration().getString("database.driver"));
        dataSourceConfig.setUsername(systemEnvironment.getMainConfiguration().getString("database.username"));
        dataSourceConfig.setPassword(systemEnvironment.getMainConfiguration().getString("database.password"));
        dataSourceConfig.setUrl(systemEnvironment.getMainConfiguration().getString("database.databaseURL"));
        dataSourceConfig.setMinConnections(1);
        dataSourceConfig.setMaxConnections(2000);
        dataSourceConfig.setHeartbeatSql("Select 1");
        dataSourceConfig.setIsolationLevel(TxIsolation.READ_COMMITED.getLevel());

        serverConfig.setDataSourceConfig(dataSourceConfig);
        serverConfig.setDefaultServer(true);
        serverConfig.setRegister(true);
        serverConfig.addPackage("ch.ksfx.model");

        EbeanServerFactory.create(serverConfig);
    }
}
