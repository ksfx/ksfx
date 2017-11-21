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
/*
package ch.ksfx.web.pages.simulator;

import ch.ksfx.dao.simulation.SimulatedAssetDecisionStrategyTemplateDAO;
import ch.ksfx.dao.simulation.SimulationOptimizationParameterDAO;
import ch.ksfx.model.simulation.SimulatedAssetDecisionStrategyTemplate;
import ch.ksfx.model.simulation.SimulationOptimizationParameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;
import java.util.Map;


@Deprecated //To be removed
@Secured({"ROLE_ADMIN"})
public class ManageDecisionStrategyParameter
{
    @Property
    private SimulatedAssetDecisionStrategyTemplate simulatedAssetDecisionStrategyTemplate;

    @Property
    private String currentKey;

    @Property
    private String currentExecutionMatrixRowKey;

    @Property
    private SimulationOptimizationParameter simulationOptimizationParameter;

    @Property
    private Map<String, Double> executionMatrixRow;

    @Inject
    private SimulatedAssetDecisionStrategyTemplateDAO simulatedAssetDecisionStrategyTemplateDAO;

    @Inject
    private SimulationOptimizationParameterDAO simulationOptimizationParameterDAO;

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long simulatedAssetDecisionStrategyTemplateId)
    {
        simulatedAssetDecisionStrategyTemplate = simulatedAssetDecisionStrategyTemplateDAO.getSimulatedAssetDecisionStrategyTemplateForId(simulatedAssetDecisionStrategyTemplateId);
    }

    public Long onPassivate()
    {
        if (simulatedAssetDecisionStrategyTemplate != null) {
            return simulatedAssetDecisionStrategyTemplate.getId();
        }

        return null;
    }

    public String getCurrentValue()
    {
        return simulatedAssetDecisionStrategyTemplate.getDecisionStrategyParameters().get(currentKey);
    }

    public void setCurrentValue(final String currentValue)
    {
        simulatedAssetDecisionStrategyTemplate.getDecisionStrategyParameters().put(currentKey, currentValue);
    }

    public boolean getIsHeader()
    {
        if (simulatedAssetDecisionStrategyTemplate.getOptimizationMatrix() == null || executionMatrixRow == null) {
            return false;
        }

        if (simulatedAssetDecisionStrategyTemplate.getOptimizationMatrix().indexOf(executionMatrixRow) == 0) {
            return true;
        }

        return false;
    }

    public Double getCurrentExecutionMatrixRowValue()
    {
        return executionMatrixRow.get(currentExecutionMatrixRowKey);
    }

    public void onActionFromDeleteDecisionStrategyParameter(String keyToDelete)
    {
        simulatedAssetDecisionStrategyTemplate.getDecisionStrategyParameters().remove(keyToDelete);
        simulatedAssetDecisionStrategyTemplateDAO.saveOrUpdateSimulatedAssetDecisionStrategyTemplate(simulatedAssetDecisionStrategyTemplate);
    }

    public void onSuccess()
    {
        simulatedAssetDecisionStrategyTemplateDAO.saveOrUpdateSimulatedAssetDecisionStrategyTemplate(simulatedAssetDecisionStrategyTemplate);
    }

    public void onActionFromAddDecisionStrategyParameter()
    {
        simulatedAssetDecisionStrategyTemplate.getDecisionStrategyParameters().put("NEW_PARAMETER","");
        simulatedAssetDecisionStrategyTemplateDAO.saveOrUpdateSimulatedAssetDecisionStrategyTemplate(simulatedAssetDecisionStrategyTemplate);
    }

    public void onActionFromAddSimulationOptimizationParameter()
    {
        SimulationOptimizationParameter simulationOptimizationParameter = new SimulationOptimizationParameter();
        simulationOptimizationParameter.setSimulatedAssetDecisionStrategyTemplate(simulatedAssetDecisionStrategyTemplate);

        simulationOptimizationParameterDAO.saveOrUpdateSimulationOptimizationParameter(simulationOptimizationParameter);
    }

    public void onActionFromDeleteSimulationOptimizationParameter(Long simulationOptimizationParameterId)
    {
        SimulationOptimizationParameter simulationOptimizationParameter = simulationOptimizationParameterDAO.getSimulationOptimizationParameterForId(simulationOptimizationParameterId);
        simulationOptimizationParameterDAO.deleteSimulationOptimizationParameter(simulationOptimizationParameter);
    }
}
*/
