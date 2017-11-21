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
import ch.ksfx.web.services.logger.SystemLogger;
import groovy.lang.GroovyClassLoader;

import java.lang.reflect.Constructor;

/**
 * Created by Kejo on 15.02.2015.
 */
public class EbeanResultUnitModifierDAO implements ResultUnitModifierDAO
{
    private SystemLogger systemLogger;

    public EbeanResultUnitModifierDAO(SystemLogger systemLogger)
    {
        this.systemLogger = systemLogger;
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

            Constructor cons = clazz.getDeclaredConstructor(SystemLogger.class);

            return (ResultUnitModifier) cons.newInstance(systemLogger);
        } catch (Exception e) {
            systemLogger.logMessage("FATAL","Error while getting decision strategy",e);
        }

        return null;
    }
}

