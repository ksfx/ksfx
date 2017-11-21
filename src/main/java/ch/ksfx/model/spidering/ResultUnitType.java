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
@Table(name = "result_unit_type")
//TODO extend this class to make it full blown with time dimension etc.
public class ResultUnitType
{
    private Long id;
    private String name; //e.g. Title / MainText / Date

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId()
    {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResultUnitType)) return false;

        ResultUnitType that = (ResultUnitType) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        if (id == null) {
            return 0;
        }

        return id.hashCode();
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
}
