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

import ch.ksfx.dao.spidering.ResultUnitModifierConfigurationDAO;
import ch.ksfx.model.spidering.ResultUnitModifierConfiguration;
import ch.ksfx.util.StacktraceUtil;
import ch.ksfx.web.services.logger.SystemLogger;
import groovy.lang.GroovyClassLoader;
import org.apache.commons.io.IOUtils;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;

/**
 * Created by Kejo on 26.10.2014.
 */
public class ManageResultUnitModifierConfiguration
{
    @Property
    private ResultUnitModifierConfiguration resultUnitModifierConfiguration;

    @Inject
    private ResultUnitModifierConfigurationDAO resultUnitModifierConfigurationDAO;

    @InjectComponent
    private Form resultUnitModifierConfigurationForm;

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long resultUnitModifierConfigurationId)
    {
        resultUnitModifierConfiguration = resultUnitModifierConfigurationDAO.getResultUnitModifierConfigurationForId(resultUnitModifierConfigurationId);
    }

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {
        if (resultUnitModifierConfiguration == null) {
            resultUnitModifierConfiguration = new ResultUnitModifierConfiguration();

            InputStream is = null;
            String resultUnitModifierDemo = null;

            try {
                is = getClass().getClassLoader().getResourceAsStream("DemoResultUnitModifier.groovy");

                StringWriter writer = new StringWriter();
                IOUtils.copy(is, writer, "UTF-8");
                resultUnitModifierDemo = writer.toString();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            resultUnitModifierConfiguration.setGroovyCode(resultUnitModifierDemo);
        }
    }

    public Long onPassivate()
    {
        if (resultUnitModifierConfiguration != null) {
            return resultUnitModifierConfiguration.getId();
        }

        return null;
    }

    public void onSuccess()
    {
        resultUnitModifierConfigurationDAO.saveOrUpdate(resultUnitModifierConfiguration);
    }

    public void onValidateFromResultUnitModifierConfigurationForm()
    {
        try {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(resultUnitModifierConfiguration.getGroovyCode());

            Constructor cons = clazz.getDeclaredConstructor(SystemLogger.class);
        } catch (Exception e) {
            resultUnitModifierConfigurationForm.recordError(StacktraceUtil.getStackTrace(e));
        }
    }
}
