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

package ch.ksfx.model;

import org.apache.commons.lang.time.DateUtils;

import javax.persistence.*;
import java.util.Date;


@Entity
@Table(name = "asset_pricing_time_range")
public class AssetPricingTimeRange
{
    private Long id;
    private String name;
    private Integer offsetMin;

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

    public Integer getOffsetMin()
    {
        return offsetMin;
    }

    public void setOffsetMin(Integer offsetMin)
    {
        this.offsetMin = offsetMin;
    }

    @Transient
    public Date getStartDate()
    {
    	if (name.equalsIgnoreCase("max")) {
    		return new Date(0l);	
    	}
    	
        Date fromDate = new Date();

        if (offsetMin == 0) {
            fromDate = DateUtils.setMinutes(fromDate, 0);
            fromDate = DateUtils.setSeconds(fromDate, 0);
            fromDate = DateUtils.setHours(fromDate, 0);
        } else {
            fromDate = DateUtils.addMinutes(fromDate, offsetMin * -1);
        }

        return fromDate;
    }
}
