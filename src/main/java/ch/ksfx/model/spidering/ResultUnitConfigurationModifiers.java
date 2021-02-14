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
 * Created by Kejo on 28.01.2015.
 */
//This class needs to be an entity for ordering
@Entity
@Table(name = "result_unit_configuration_modifiers")
public class ResultUnitConfigurationModifiers
{
    private Long id;
    private ResultUnitConfiguration resultUnitConfiguration;
    private ResultUnitModifierConfiguration resultUnitModifierConfiguration;

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
    @JoinColumn(name = "result_unit_configuration")
    public ResultUnitConfiguration getResultUnitConfiguration()
    {
        return resultUnitConfiguration;
    }

    public void setResultUnitConfiguration(ResultUnitConfiguration resultUnitConfiguration)
    {
        this.resultUnitConfiguration = resultUnitConfiguration;
    }

    @ManyToOne
    @JoinColumn(name = "result_unit_modifier_configuration")
    public ResultUnitModifierConfiguration getResultUnitModifierConfiguration()
    {
        return resultUnitModifierConfiguration;
    }

    public void setResultUnitModifierConfiguration(ResultUnitModifierConfiguration resultUnitModifierConfiguration)
    {
        this.resultUnitModifierConfiguration = resultUnitModifierConfiguration;
    }
}
