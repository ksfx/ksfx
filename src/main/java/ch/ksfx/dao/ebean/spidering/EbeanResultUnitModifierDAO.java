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

package ch.ksfx.dao.ebean.spidering;

import ch.ksfx.dao.spidering.ResultUnitModifierDAO;
import ch.ksfx.model.spidering.ResultUnitModifier;
import ch.ksfx.model.spidering.ResultUnitModifierConfiguration;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.services.systemlogger.SystemLogger;
import groovy.lang.GroovyClassLoader;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Constructor;

/**
 * Created by Kejo on 15.02.2015.
 */
@Repository
public class EbeanResultUnitModifierDAO implements ResultUnitModifierDAO
{
    private SystemLogger systemLogger;
    private ServiceProvider serviceProvider;

    public EbeanResultUnitModifierDAO(SystemLogger systemLogger, ServiceProvider serviceProvider)
    {
        this.systemLogger = systemLogger;
        this.serviceProvider = serviceProvider;
    }

    @Override
    public ResultUnitModifier getResultUnitModifier(ResultUnitModifierConfiguration resultUnitModifierConfiguration)
    {
        try {
            if (resultUnitModifierConfiguration.getGroovyCode() == null || resultUnitModifierConfiguration.getGroovyCode().isEmpty()) {
                throw new IllegalArgumentException("Result unit modifier has no code");
            }

            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(resultUnitModifierConfiguration.getGroovyCode());

            Constructor cons = clazz.getDeclaredConstructor(ServiceProvider.class);

            return (ResultUnitModifier) cons.newInstance(serviceProvider);
        } catch (Exception e) {
            e.printStackTrace();
            systemLogger.logMessage("FATAL","Error while getting result unit modifier",e);
        }

        return null;
    }
}

