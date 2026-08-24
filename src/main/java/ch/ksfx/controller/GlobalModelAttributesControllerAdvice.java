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

package ch.ksfx.controller;

import ch.ksfx.services.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes the instance's identity (as configured in conf.xml) available to every Thymeleaf
 * template, so non-production instances can render a visible banner.
 */
@ControllerAdvice
public class GlobalModelAttributesControllerAdvice
{
    public static final String PRODUCTION_INSTANCE_NAME = "PRODUCTION";

    @Autowired
    private SystemEnvironment systemEnvironment;

    @ModelAttribute("instanceName")
    public String instanceName()
    {
        return systemEnvironment.getMainConfiguration().getString("instanceName");
    }

    @ModelAttribute("isProductionInstance")
    public boolean isProductionInstance()
    {
        return PRODUCTION_INSTANCE_NAME.equals(instanceName());
    }
}
