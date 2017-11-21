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
import javax.xml.bind.annotation.XmlTransient;

/**
 * Created by Kejo on 26.10.2014.
 */
@Entity
@Table(name = "result_unit")
public class ResultUnit
{
    private Long id;
    private ResultUnitType resultUnitType;
    private boolean siteIdentifier = false;
    private String value;
    private Result result;

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

    @XmlTransient
    @JsonIgnore
    public boolean getSiteIdentifier()
    {
        return siteIdentifier;
    }

    public void setSiteIdentifier(boolean siteIdentifier)
    {
        this.siteIdentifier = siteIdentifier;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    @ManyToOne
    @XmlTransient
    @JsonIgnore
    @JoinColumn(name = "result")
    public Result getResult()
    {
        return result;
    }

    public void setResult(Result result)
    {
        this.result = result;
    }
}
