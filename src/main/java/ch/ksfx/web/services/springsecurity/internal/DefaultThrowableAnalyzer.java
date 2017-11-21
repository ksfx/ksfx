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

package ch.ksfx.web.services.springsecurity.internal;

import org.springframework.security.web.util.ThrowableAnalyzer;
import org.springframework.security.web.util.ThrowableCauseExtractor;

import javax.servlet.ServletException;


public class DefaultThrowableAnalyzer extends ThrowableAnalyzer
{

    /**
    * @see org.springframework.security.web.util.ThrowableAnalyzer#initExtractorMap()
    */
    protected void initExtractorMap()
    {
        super.initExtractorMap();

        registerExtractor(ServletException.class, new ThrowableCauseExtractor() {
                    public Throwable extractCause(Throwable throwable) {
                ThrowableAnalyzer.verifyThrowableHierarchy(throwable, ServletException.class);
                return ((ServletException) throwable).getRootCause();
                    }
                });
    }
}
