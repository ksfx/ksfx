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

package ch.ksfx.web.services.systemenvironment.impl;

import ch.ksfx.web.services.systemenvironment.SystemEnvironment;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;


public class SystemEnvironmentImpl implements SystemEnvironment
{
    public static final String HOME_DIRECTORY = "ksfxHome";

    public static final String DEFAULT_NAME = "ksfx";

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private Logger logger = LoggerFactory.getLogger(SystemEnvironmentImpl.class);

    public String getApplicationHomeDirectoryPath()
    {
        String ksfxHome = getEnvironmentVariableValue(HOME_DIRECTORY);

        if (ksfxHome == null || ksfxHome.isEmpty()) {
            ksfxHome = System.getProperty("user.home");
            ksfxHome += FILE_SEPARATOR + DEFAULT_NAME;
        }

        return ksfxHome;
    }

    public String getConfigurationDirectory()
    {
        return getApplicationHomeDirectoryPath() + FILE_SEPARATOR + "conf";
    }

    public String getConfigurationFilePath()
    {
        return getConfigurationDirectory() + FILE_SEPARATOR + "conf.xml";
    }

    public Configuration getMainConfiguration()
    {
        try {
            Configuration mainConfiguration = new XMLConfiguration(getConfigurationFilePath());
            return mainConfiguration;
        } catch (ConfigurationException e) {
            logger.error("Could not get configuration!", e);

            return new XMLConfiguration();
        }
    }

    @Override
    public String getEnvironmentVariableValue(String environmentVariableName)
    {
        try {
            // check if property is set
            return System.getProperty(environmentVariableName);
        } catch (Exception e) {
            // property is not set
        }

        try {
            // check if environment variable is set
            return System.getenv(environmentVariableName);
        } catch (Exception e) {
            // environment variable is not set
        }

        return null;
    }

    @Override
    public String getApplicationLogfilePath() 
    {
        String logfilePath = getApplicationHomeDirectoryPath();
        
        if (!logfilePath.endsWith(FILE_SEPARATOR)) {
            logfilePath += FILE_SEPARATOR;
        }
        
        logfilePath += "logs" + FILE_SEPARATOR;
        
        File logfile = new File(logfilePath);
        
        if (!logfile.isDirectory()) {
            logfile.mkdirs();
        }
        
        return logfilePath + "out";
    }
    
    @Override
    public String getApplicationIndexfilePath() 
    {
        String indexfilePath = getApplicationHomeDirectoryPath();
        
        if (!indexfilePath.endsWith(FILE_SEPARATOR)) {
            indexfilePath += FILE_SEPARATOR;
        }
        
        indexfilePath += "index" + FILE_SEPARATOR;
        
        File indexfile = new File(indexfilePath);
        
        if (!indexfile.isDirectory()) {
            indexfile.mkdirs();
        }
        
        return indexfilePath;
    }
}
