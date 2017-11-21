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
import ch.ksfx.dao.simulation.SimulationDAO;
import ch.ksfx.dao.simulation.SimulationOptimizationParameterDAO;
import ch.ksfx.model.simulation.SimulatedAssetDecisionStrategyTemplate;
import ch.ksfx.model.simulation.Simulation;
import ch.ksfx.model.simulation.SimulationOptimizationResult;
import ch.ksfx.util.RunningSimulationCache;
import ch.ksfx.web.services.simulation.SimulationRunner;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;


@Deprecated //To be removed
@Secured({"ROLE_ADMIN"})
public class SimulatorIndex
{
    @Inject
    private SimulationDAO simulationDAO;

    @Inject
    private SimulatedAssetDecisionStrategyTemplateDAO simulatedAssetDecisionStrategyTemplateDAO;

    @Inject
    private SimulationRunner simulationRunner;

    //@Inject
    //private OptimizationRunner optimizationRunner;

    @Inject
    private SimulationOptimizationParameterDAO simulationOptimizationParameterDAO;

    @Property
    private Simulation simulation;


    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {

    }

    public boolean isRunning()
    {
        if (simulation.getStartDate() != null && simulation.getEndDate() == null) {
            return true;
        }

        return false;
    }

    public boolean isOptimizationRunning()
    {
        if (simulation.getOptimizationStartDate() != null && simulation.getOptimizationEndDate() == null) {
            return true;
        }

        return false;
    }

    public List<Simulation> getAllSimulations()
    {
        return simulationDAO.getAllSimulations();
    }

    public void saveSimulation(Simulation simulation)
    {
        simulationDAO.saveOrUpdate(simulation);
    }

    public void saveSimulatedAssetDecisionStrategyTemplate(SimulatedAssetDecisionStrategyTemplate simulatedAssetDecisionStrategyTemplate)
    {
        simulatedAssetDecisionStrategyTemplateDAO.saveOrUpdateSimulatedAssetDecisionStrategyTemplate(simulatedAssetDecisionStrategyTemplate);
    }

    public void onActionFromRunSimulation(Long simulationId)
    {
        Simulation simulation = simulationDAO.getSimulationForId(simulationId);

        simulation.setStartDate(new Timestamp(new Date().getTime()));

        for (SimulatedAssetDecisionStrategyTemplate sadst : simulation.getSimulatedAssetDecisionStrategyTemplates()) {
            sadst.setStopTrade(null);
            saveSimulatedAssetDecisionStrategyTemplate(sadst);
        }

        saveSimulation(simulation);

        simulationRunner.runSimulation(simulation);
    }

    public void onActionFromRunOptimization(Long simulationId)
    {

    }

    public void onActionFromTerminateSimulation(Long simulationId)
    {
        Simulation sim = simulationDAO.getSimulationForId(simulationId);

        if (RunningSimulationCache.runningSimulations.containsKey(sim.getId())) {
            simulationRunner.terminateSimulation(sim);
        } else {
            sim.setEndDate(new Timestamp(new Date().getTime()));
            saveSimulation(sim);
        }
    }

    public void onActionFromTerminateOptimization(Long simulationId)
    {

    }

    public void onActionFromDelete(Long simulationId)
    {
        Simulation sim = simulationDAO.getSimulationForId(simulationId);
        onActionFromDeleteOptimizationData(simulationId);
        simulationDAO.deleteSimulationData(sim);
        simulationDAO.deleteSimulation(sim);
    }

    public void onActionFromDeleteSimulationData(Long simulationId)
    {
        Simulation sim = simulationDAO.getSimulationForId(simulationId);
        simulationDAO.deleteSimulationData(sim);
    }

    public void onActionFromDeleteOptimizationData(Long simulationId)
    {
        Simulation sim = simulationDAO.getSimulationForId(simulationId);

        for (SimulatedAssetDecisionStrategyTemplate simulatedAssetDecisionStrategyTemplate : sim.getSimulatedAssetDecisionStrategyTemplates()) {
            for (SimulationOptimizationResult simulationOptimizationResult : simulatedAssetDecisionStrategyTemplate.getSimulationOptimizationResults()) {
                simulationOptimizationParameterDAO.deleteSimulationOptimizationResult(simulationOptimizationResult);
            }
        }

        sim.setOptimizationEndDate(null);
        sim.setOptimizationStartDate(null);
        sim.setOptimizationCompletedPercentage(0.0f);

        saveSimulation(sim);
    }
}
*/
