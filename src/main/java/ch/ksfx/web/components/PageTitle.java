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

package ch.ksfx.web.components;

import ch.ksfx.web.services.sitemap.Page;
import ch.ksfx.web.services.sitemap.Sitemap;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;


public class PageTitle
{
    @Property
    private Page page;
    @Inject
    private Sitemap sitemap;
    @Inject
    private Request request;

    @SetupRender
    boolean setupRender()
    {
        page = sitemap.getPageForPath(request.getPath());
        return true;
    }

    public Sitemap getSitemap()
    {
        return sitemap;
    }
}
