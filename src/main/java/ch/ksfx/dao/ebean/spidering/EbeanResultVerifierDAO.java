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

import ch.ksfx.dao.spidering.ResultVerifierDAO;
import ch.ksfx.model.spidering.ResultVerifier;
import ch.ksfx.model.spidering.ResultVerifierConfiguration;
import ch.ksfx.services.systemlogger.SystemLogger;
import ch.ksfx.services.ServiceProvider;
import groovy.lang.GroovyClassLoader;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Constructor;

/**
 * Created by Kejo on 15.02.2015.
 */
@Repository
public class EbeanResultVerifierDAO implements ResultVerifierDAO
{
    private ServiceProvider serviceProvider;
    private SystemLogger systemLogger;

    public EbeanResultVerifierDAO(ServiceProvider serviceProvider, SystemLogger systemLogger)
    {
        this.serviceProvider = serviceProvider;
        this.systemLogger = systemLogger;
    }

    @Override
    public ResultVerifier getResultVerifier(ResultVerifierConfiguration resultVerifierConfiguration)
    {
        try {
            if (resultVerifierConfiguration.getGroovyCode() == null || resultVerifierConfiguration.getGroovyCode().isEmpty()) {
                throw new IllegalArgumentException("Result verifier has no code");
            }

            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(resultVerifierConfiguration.getGroovyCode());

            Constructor cons = clazz.getDeclaredConstructor(ServiceProvider.class);

            return (ResultVerifier) cons.newInstance(serviceProvider);
        } catch (Exception e) {
            e.printStackTrace();
            systemLogger.logMessage("FATAL","Error while getting result verifier",e);
        }

        return null;
    }
}

