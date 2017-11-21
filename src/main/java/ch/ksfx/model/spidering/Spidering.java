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
import java.util.Date;
import java.util.List;


@Entity
@Table(name = "spidering")
public class Spidering
{
    private Long id;
    private Date started;
    private Date finished;
    private List<Result> results;
    private SpideringConfiguration spideringConfiguration;
    private Integer newResultCount;
    private Integer updatedResultCount;
    private Integer unchangedResultCount;
    private Integer resultCount;
    private Integer validResultCount;
    private Integer invalidResultCount;

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

    @Temporal(TemporalType.TIMESTAMP)
    public Date getStarted()
    {
        return started;
    }

    public void setStarted(Date started)
    {
        this.started = started;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getFinished()
    {
        return finished;
    }

    public void setFinished(Date finished)
    {
        this.finished = finished;
    }

    @OneToMany(mappedBy = "spidering")
    public List<Result> getResults()
    {
        return results;
    }

    public void setResults(List<Result> results)
    {
        this.results = results;
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

    public Integer getNewResultCount()
    {
        return newResultCount;
    }

    public void setNewResultCount(Integer newResultCount)
    {
        this.newResultCount = newResultCount;
    }

    public Integer getUpdatedResultCount()
    {
        return updatedResultCount;
    }

    public void setUpdatedResultCount(Integer updatedResultCount)
    {
        this.updatedResultCount = updatedResultCount;
    }

    public Integer getUnchangedResultCount()
    {
        return unchangedResultCount;
    }

    public void setUnchangedResultCount(Integer unchangedResultCount)
    {
        this.unchangedResultCount = unchangedResultCount;
    }

    public Integer getResultCount()
    {
        return resultCount;
    }

    public void setResultCount(Integer resultCount)
    {
        this.resultCount = resultCount;
    }

    public Integer getValidResultCount()
    {
        return validResultCount;
    }

    public void setValidResultCount(Integer validResultCount)
    {
        this.validResultCount = validResultCount;
    }

    public Integer getInvalidResultCount()
    {
        return invalidResultCount;
    }

    public void setInvalidResultCount(Integer invalidResultCount)
    {
        this.invalidResultCount = invalidResultCount;
    }
}
