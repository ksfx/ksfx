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

import com.owlike.genson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "spidering_configuration")
@XmlRootElement
public class SpideringConfiguration
{
    private Long id;
    private String name;
    private String cronSchedule;
    private boolean checkDuplicatesGlobally = false;
    private boolean cronScheduleEnabled = false;
    private boolean debugSpidering = false;
    private List<ResourceConfiguration> resourceConfigurations;
    private List<Spidering> spiderings;
    private ResultVerifierConfiguration resultVerifierConfiguration;
    private List<SpideringPostActivity> spideringPostActivities;

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

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getCronSchedule()
    {
        return cronSchedule;
    }

    public void setCronSchedule(String cronSchedule)
    {
        this.cronSchedule = cronSchedule;
    }

    public boolean getCronScheduleEnabled()
    {
        return cronScheduleEnabled;
    }

    public void setCronScheduleEnabled(boolean cronScheduleEnabled)
    {
        this.cronScheduleEnabled = cronScheduleEnabled;
    }

    @XmlTransient
    @JsonIgnore
    public boolean getCheckDuplicatesGlobally()
    {
        return checkDuplicatesGlobally;
    }

    public void setCheckDuplicatesGlobally(boolean checkDuplicatesGlobally)
    {
        this.checkDuplicatesGlobally = checkDuplicatesGlobally;
    }
    
    @XmlTransient
    @JsonIgnore
    public boolean getDebugSpidering()
    {
        return debugSpidering;
    }

    public void setDebugSpidering(boolean debugSpidering)
    {
        this.debugSpidering = debugSpidering;
    }

    @XmlTransient
    @JsonIgnore
    @OneToMany(mappedBy = "spideringConfiguration")
    public List<ResourceConfiguration> getResourceConfigurations()
    {
        return resourceConfigurations;
    }

    public void setResourceConfigurations(List<ResourceConfiguration> resourceConfigurations)
    {
        this.resourceConfigurations = resourceConfigurations;
    }

    @XmlTransient
    @JsonIgnore
    @OneToMany(mappedBy = "spideringConfiguration")
    public List<Spidering> getSpiderings()
    {
        return spiderings;
    }

    public void setSpiderings(List<Spidering> spiderings)
    {
        this.spiderings = spiderings;
    }

    @XmlTransient
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "result_verifier_configuration")
    public ResultVerifierConfiguration getResultVerifierConfiguration()
    {
        return resultVerifierConfiguration;
    }

    public void setResultVerifierConfiguration(ResultVerifierConfiguration resultVerifierConfiguration)
    {
        this.resultVerifierConfiguration = resultVerifierConfiguration;
    }

    @XmlTransient
    @JsonIgnore
    @OneToMany(mappedBy = "spideringConfiguration")
    public List<SpideringPostActivity> getSpideringPostActivities()
    {
        return spideringPostActivities;
    }

    public void setSpideringPostActivities(List<SpideringPostActivity> spideringPostActivities)
    {
        this.spideringPostActivities = spideringPostActivities;
    }

    public String getRootUrl()
    {
        String rootUrl = "";

        if (resourceConfigurations != null && resourceConfigurations.size() > 0) {
            ResourceConfiguration resourceConfiguration = resourceConfigurations.get(0);

            if (resourceConfiguration.getUrlFragments() != null) {
                for (UrlFragment urlFragment : resourceConfiguration.getUrlFragments()) {
                    rootUrl += urlFragment.getFragmentQuery();
                }
            }
        }

        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {}

    @XmlTransient
    @Transient
    @JsonIgnore
    public List<ResultUnitType> getNotUpdateableResultUnitTypes()
    {
        List<ResultUnitType> lockedResultUnitTypes = new ArrayList<ResultUnitType>();

        if (resourceConfigurations != null) {
            for (ResourceConfiguration resourceConfiguration : resourceConfigurations) {
                if (resourceConfiguration.getResultUnitConfigurations() != null) {
                    for (ResultUnitConfiguration resultUnitConfiguration : resourceConfiguration.getResultUnitConfigurations()) {
                        if (resultUnitConfiguration.getLockedForUpdates()) {
                            lockedResultUnitTypes.add(resultUnitConfiguration.getResultUnitType());
                        }
                    }
                }
            }
        }
        return lockedResultUnitTypes;
    }

}
