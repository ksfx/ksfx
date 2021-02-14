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

/**
 * Created by Kejo on 26.10.2014.
 *
 *
 */
@Entity
@Table(name = "result_unit_configuration")
public class ResultUnitConfiguration
{
    private Long id;
    private ResultUnitType resultUnitType;
    private ResultUnitFinder resultUnitFinder;
    private ResourceConfiguration resourceConfiguration;
    private boolean siteIdentifier = false;
    private boolean lockedForUpdates = false;
    private String finderQuery;
    private List<ResultUnitConfigurationModifiers> resultUnitConfigurationModifiers;

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
    @JoinColumn(name = "result_unit_type")
    public ResultUnitType getResultUnitType()
    {
        return resultUnitType;
    }

    public void setResultUnitType(ResultUnitType resultUnitType)
    {
        this.resultUnitType = resultUnitType;
    }

    @ManyToOne
    @JoinColumn(name = "result_unit_finder")
    public ResultUnitFinder getResultUnitFinder()
    {
        return resultUnitFinder;
    }

    public void setResultUnitFinder(ResultUnitFinder resultUnitFinder)
    {
        this.resultUnitFinder = resultUnitFinder;
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

    public boolean getSiteIdentifier()
    {
        return siteIdentifier;
    }

    public void setSiteIdentifier(boolean siteIdentifier)
    {
        this.siteIdentifier = siteIdentifier;
    }

    public boolean getLockedForUpdates()
    {
        return lockedForUpdates;
    }

    public void setLockedForUpdates(boolean lockedForUpdates)
    {
        this.lockedForUpdates = lockedForUpdates;
    }

    public String getFinderQuery()
    {
        return finderQuery;
    }

    public void setFinderQuery(String finderQuery)
    {
        this.finderQuery = finderQuery;
    }

    @OneToMany(mappedBy = "resultUnitConfiguration")
    public List<ResultUnitConfigurationModifiers> getResultUnitConfigurationModifiers()
    {
        return resultUnitConfigurationModifiers;
    }

    public void setResultUnitConfigurationModifiers(List<ResultUnitConfigurationModifiers> resultUnitConfigurationModifiers)
    {
        this.resultUnitConfigurationModifiers = resultUnitConfigurationModifiers;
    }
}
