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

import java.util.ArrayList;
import java.util.List;


public class Page
{
    private String name;
    private String title;
    private String acceptedRoles;
    private String description = null;
    private List<Page> childPages;

    public Page(String name, String title)
    {
        this.name = name;
        this.title = title;
        this.childPages = new ArrayList<Page>();
    }

    public Page(String name, String title, String description)
    {
        this(name, title);
        this.description = description;
    }

    public Page(String name, String title, String description, String acceptedRoles)
    {
        this(name, title, description);
        this.acceptedRoles = acceptedRoles;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setChildPages(List<Page> childPages)
    {
        this.childPages = childPages;
    }

    public void addChildPage(Page page)
    {
        childPages.add(page);
    }

    public List<Page> getChildPages()
    {
        return childPages;
    }

    public String getAcceptedRoles()
    {
        return acceptedRoles;
    }

    public void setAcceptedRoles(String acceptedRoles)
    {
        this.acceptedRoles = acceptedRoles;
    }
}
