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

package ch.ksfx.model.spidering;

import javax.persistence.*;

/**
 * Created by Kejo on 26.10.2014.
 */
@Entity
@Table(name = "url_fragment")
public class UrlFragment
{
    private Long id;
    private UrlFragmentFinder urlFragmentFinder;
    private ResourceConfiguration resourceConfiguration;
    private String fragmentQuery;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(name = "url_fragment_finder")
    public UrlFragmentFinder getUrlFragmentFinder()
    {
        return urlFragmentFinder;
    }

    public void setUrlFragmentFinder(UrlFragmentFinder urlFragmentFinder)
    {
        this.urlFragmentFinder = urlFragmentFinder;
    }

    @ManyToOne
    @JoinColumn(name = "resource_configuration")
    public ResourceConfiguration getResourceConfiguration()
    {
        return resourceConfiguration;
    }

    public void setResourceConfiguration(ResourceConfiguration resourceConfiguration)
    {
        this.resourceConfiguration = resourceConfiguration;
    }

    public String getFragmentQuery()
    {
        return fragmentQuery;
    }

    public void setFragmentQuery(String fragmentQuery)
    {
        this.fragmentQuery = fragmentQuery;
    }
}
