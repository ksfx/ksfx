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

import ch.ksfx.util.MD5Helper;
import com.owlike.genson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Date;
import java.util.List;


@Entity
@Table(name = "result")
@XmlRootElement
public class Result
{
    private Long id;
    private Resource baseLevelResource;
    private Date firstFound;
    private Date lastFound;
    private List<ResultUnit> resultUnits;
    private String resultHash;
    private String siteIdentifierHash;
    private Spidering spidering;
    //For performance reasons
    private SpideringConfiguration spideringConfiguration;
    private String uriString;
    private boolean isValid = true;
    private boolean updated = false;

    //Only for stupid study project
//    private String url;
//    private Long sourceId;

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
    @XmlTransient
    @JsonIgnore
    @JoinColumn(name = "spidering")
    public Spidering getSpidering()
    {
        return spidering;
    }

    public void setSpidering(Spidering spidering)
    {
        this.spidering = spidering;
    }

    @ManyToOne
    @XmlTransient
    @JsonIgnore
    @JoinColumn(name = "base_level_resource")
    public Resource getBaseLevelResource()
    {
        return baseLevelResource;
    }

    public void setBaseLevelResource(Resource baseLevelResource)
    {
        this.baseLevelResource = baseLevelResource;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getFirstFound()
    {
        return firstFound;
    }

    public void setFirstFound(Date firstFound)
    {
        this.firstFound = firstFound;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getLastFound()
    {
        return lastFound;
    }

    public void setLastFound(Date lastFound)
    {
        this.lastFound = lastFound;
    }

    @OneToMany(mappedBy = "result")
    public List<ResultUnit> getResultUnits()
    {
        return resultUnits;
    }

    public void setResultUnits(List<ResultUnit> resultUnits)
    {
        this.resultUnits = resultUnits;
    }

    public String getResultHash()
    {
        return resultHash;
    }

    public void setResultHash(String resultHash)
    {
        this.resultHash = resultHash;
    }

    @ManyToOne
    @XmlTransient
    @JsonIgnore
    @JoinColumn(name = "spidering_configuration")
    public SpideringConfiguration getSpideringConfiguration()
    {
        return spideringConfiguration;
    }

    public void setSpideringConfiguration(SpideringConfiguration spideringConfiguration)
    {
        this.spideringConfiguration = spideringConfiguration;
    }
    
    public String getUriString()
    {
    	return uriString;
    }
    
    public void setUriString(String uriString)
    {
    	this.uriString = uriString;
    }

    @XmlTransient
    @JsonIgnore
    public boolean getIsValid()
    {
        return isValid;
    }

    public void setIsValid(boolean isValid)
    {
        this.isValid = isValid;
    }

    @XmlTransient
    @JsonIgnore
    public boolean getUpdated()
    {
        return updated;
    }

    public void setUpdated(boolean updated)
    {
        this.updated = updated;
    }
    
    public String getSiteIdentifierHash()
    {
    	return siteIdentifierHash;
    }
    
    public void setSiteIdentifierHash(String siteIdentifierHash)
    {
    	this.siteIdentifierHash = siteIdentifierHash;
    }

//    @Transient
//    public String getUrl()
//    {
//        if (getBaseLevelResource() != null) {
//            return getBaseLevelResource().getUrl();
//        } else {
//            if(getSiteIdentifier() != null) {
//                return getSiteIdentifier().getValue();
//            } else {
//                return "";
//            }
//        }
//    }
//
//    public void setUrl(String url)
//    {
//
//    }
//
//    @Transient
//    public Long getSourceId()
//    {
//        return getSpideringConfiguration().getId();
//    }
//
//    public void setSourceId(Long sourceId)
//    {
//
//    }

    public String calculateResultHash()
    {
        String hash = "";

        for (ResultUnit resultUnit : resultUnits) {
            if (!resultUnit.getSiteIdentifier()) {
                hash += resultUnit.getValue();
            }
        }

        System.out.println("Clear text hash: " + hash);

        return MD5Helper.md5HexHash(hash);
    }

    public ResultUnit getSiteIdentifier()
    {
        if (resultUnits != null) {
            for (ResultUnit resultUnit : resultUnits) {
                if (resultUnit.getSiteIdentifier()) {
                    return  resultUnit;
                }
            }
        }

        return null;
    }

    public ResultUnit getResultUnitForResultUnitTypeName(String resultUnitTypeName)
    {
        for (ResultUnit resultUnit : resultUnits) {
            if (resultUnit.getResultUnitType().getName().equalsIgnoreCase(resultUnitTypeName)) {
                return resultUnit;
            }
        }

        return null;
    }
}
