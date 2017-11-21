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
import java.util.List;


@Entity
@Table(name = "resource_configuration")
public class ResourceConfiguration
{
    private Long id;
    private SpideringConfiguration spideringConfiguration;
    private ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration;
    List<UrlFragment> urlFragments;
    private ResourceLoaderPluginConfiguration pagingResourceLoaderPluginConfiguration;
    List<PagingUrlFragment> pagingUrlFragments;
    List<ResultUnitConfiguration> resultUnitConfigurations;
    private boolean paging;
    private Integer depth;
    private boolean baseLevel = false;

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
    @JoinColumn(name = "spidering_configuration")
    public SpideringConfiguration getSpideringConfiguration()
    {
        return spideringConfiguration;
    }

    public void setSpideringConfiguration(SpideringConfiguration spideringConfiguration)
    {
        this.spideringConfiguration = spideringConfiguration;
    }
    
    @ManyToOne
    @JoinColumn(name = "resource_loader_plugin_configuration")
    public ResourceLoaderPluginConfiguration getResourceLoaderPluginConfiguration()
    {
        return resourceLoaderPluginConfiguration;
    }

    public void setResourceLoaderPluginConfiguration(ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration)
    {
        this.resourceLoaderPluginConfiguration = resourceLoaderPluginConfiguration;
    }

    @OneToMany(mappedBy = "resourceConfiguration")
    public List<UrlFragment> getUrlFragments()
    {
        return urlFragments;
    }

    public void setUrlFragments(List<UrlFragment> urlFragments)
    {
        this.urlFragments = urlFragments;
    }

    @ManyToOne
    @JoinColumn(name = "paging_resource_loader_plugin_configuration")
    public ResourceLoaderPluginConfiguration getPagingResourceLoaderPluginConfiguration()
    {
        return pagingResourceLoaderPluginConfiguration;
    }

    public void setPagingResourceLoaderPluginConfiguration(ResourceLoaderPluginConfiguration pagingResourceLoaderPluginConfiguration)
    {
        this.pagingResourceLoaderPluginConfiguration = pagingResourceLoaderPluginConfiguration;
    }

    @OneToMany(mappedBy = "resourceConfiguration")
    public List<PagingUrlFragment> getPagingUrlFragments()
    {
        return pagingUrlFragments;
    }

    public void setPagingUrlFragments(List<PagingUrlFragment> pagingUrlFragments)
    {
        this.pagingUrlFragments = pagingUrlFragments;
    }

    @OneToMany(mappedBy = "resourceConfiguration")
    public List<ResultUnitConfiguration> getResultUnitConfigurations()
    {
        return resultUnitConfigurations;
    }

    public void setResultUnitConfigurations(List<ResultUnitConfiguration> resultUnitConfigurations)
    {
        this.resultUnitConfigurations = resultUnitConfigurations;
    }

    public boolean getPaging()
    {
        return paging;
    }

    public void setPaging(boolean paging)
    {
        this.paging = paging;
    }

    public boolean getBaseLevel()
    {
        return baseLevel;
    }

    public void setBaseLevel(boolean baseLevel)
    {
        this.baseLevel = baseLevel;
    }

    public Integer getDepth()
    {
        return depth;
    }

    public void setDepth(Integer depth)
    {
        this.depth = depth;
    }
}
