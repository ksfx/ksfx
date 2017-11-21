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

package ch.ksfx.web.services.version.impl;

import ch.ksfx.web.services.version.Version;
import org.slf4j.Logger;

import java.util.Properties;


public class VersionImpl implements Version
{
    private Properties buildProperties;
    private Logger logger;

    public VersionImpl(Logger logger)
    {
        this.logger = logger;
    }

    public String getVersion()
    {
        if (buildProperties == null) {
            loadBuildProperties();
        }

        return buildProperties.getProperty("application.version");
    }

    public String getBuildNumber()
    {
        if (buildProperties == null) {
            loadBuildProperties();
        }

        if (buildProperties.getProperty("build.number") != null
                && !buildProperties.getProperty("build.number").startsWith("$")) {
            return buildProperties.getProperty("build.number");
        }

        return null;
    }

    public String getRevision()
    {
        if (buildProperties == null) {
            loadBuildProperties();
        }

        if (buildProperties.getProperty("scm.revision") != null
                && !buildProperties.getProperty("scm.revision").startsWith("$")) {
            return buildProperties.getProperty("scm.revision");
        }

        return null;
    }

    public String getVersionString()
    {
        StringBuilder sb = new StringBuilder();
        if (this.getVersion() != null) {
            sb.append(this.getVersion());
        }
        if (this.getBuildNumber() != null || this.getRevision() != null) {

            sb.append(" (");
            if (this.getBuildNumber() != null) {
                sb.append("b").append(this.getBuildNumber());
            }
            if (this.getRevision() != null) {
                sb.append("r").append(this.getRevision());
            }
            sb.append(")");
        }

        return sb.toString();
    }

    private void loadBuildProperties()
    {
        try {
            buildProperties = new Properties();
            buildProperties.load(getClass().getClassLoader().getResourceAsStream("version.config"));
        } catch (Exception e) {
            logger.error("Unable to load version information", e);
        }
    }
}
