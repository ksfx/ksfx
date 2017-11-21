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

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityMetadataSource;

import java.util.Collection;
import java.util.List;


public class StaticDefinitionSource implements SecurityMetadataSource
{
    /**
    * @param object
    * security object
    */
    public final List<ConfigAttribute> getAttributes(final Object object)
    {
        ConfigAttributeHolder attrHolder = (ConfigAttributeHolder) object;
        return (List<ConfigAttribute>) attrHolder.getAttributes();
    }

    /**
    * Returns true if clazz is extension of {@link ConfigAttributeHolder}.
    *
    * @param clazz
    * the class that is being queried
    * @return true if clazz is extension of {@link ConfigAttributeHolder}.
    */
    @SuppressWarnings("unchecked")
    public final boolean supports(final Class clazz)
    {
        return ConfigAttributeHolder.class.isAssignableFrom(clazz);
    }

    public Collection<ConfigAttribute> getAllConfigAttributes()
    {
        return null;
    }
}
