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

package ch.ksfx.web.services.sitemap;

import org.apache.tapestry5.ioc.annotations.InjectService;
import org.apache.tapestry5.services.ComponentClassResolver;

import java.util.ArrayList;
import java.util.List;


public class AnnotationPageProvider implements PageProvider
{
    private List<Page> firstLevelPages;
    private ComponentClassResolver componentClassResolver;

    public AnnotationPageProvider(
            @InjectService("ComponentClassResolver") ComponentClassResolver componentClassResolver)
    {
        this.componentClassResolver = componentClassResolver;
        gatherFirstLevelPages();
    }

    public List<Page> getFirstLevelPages()
    {
        return this.firstLevelPages;
    }

    private void gatherFirstLevelPages()
    {
        this.firstLevelPages = new ArrayList<Page>();

        for (String pageName : componentClassResolver.getPageNames()) {
            // @todo
        }
    }
}
