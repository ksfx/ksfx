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

import ch.ksfx.dao.DecisionStrategyTemplateDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.dao.simulation.SimulatedAssetDecisionStrategyTemplateDAO;
import ch.ksfx.dao.simulation.SimulationDAO;
import ch.ksfx.model.DecisionStrategyTemplate;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.model.simulation.SimulatedAssetDecisionStrategyTemplate;
import ch.ksfx.model.simulation.Simulation;
import ch.ksfx.util.GenericSelectModel;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.PropertyAccess;
import org.springframework.security.access.annotation.Secured;
import java.util.ArrayList;
import java.util.List;


@Deprecated //To be removed
@Secured({"ROLE_ADMIN"})
public class ManageSimulation
{
    @Property
    private Simulation simulation;

    @Property
    private SimulatedAssetDecisionStrategyTemplate simulatedAssetDecisionStrategyTemplate;

    @Inject
    private SimulationDAO simulationDAO;

    @Inject
    private SimulatedAssetDecisionStrategyTemplateDAO simulatedAssetDecisionStrategyTemplateDAO;

    @Inject
    private DecisionStrategyTemplateDAO decisionStrategyTemplateDAO;

    @Inject
    private TimeSeriesDAO timeSeriesDAO;

    @Inject
    private PropertyAccess propertyAccess;

    @Persist
    @Property
    private GenericSelectModel<DecisionStrategyTemplate> allMasterDecisionStrategyTemplates;
    
    private String seriesName; 

 //   @Persist
 //   @Property
 //   private GenericSelectModel<TimeSeries> allTimeSeries;

    public void onActionFromAddSimulatedAssetDecisionStrategyTemplate()
    {
        SimulatedAssetDecisionStrategyTemplate strategyTemplate = new SimulatedAssetDecisionStrategyTemplate();
        strategyTemplate.setSimulation(simulation);

        simulatedAssetDecisionStrategyTemplateDAO.saveOrUpdateSimulatedAssetDecisionStrategyTemplate(strategyTemplate);
    }

    public void onActionFromDeleteSimulatedAssetDecisionStrategyTemplate(Long id)
    {
        SimulatedAssetDecisionStrategyTemplate simulatedAssetDecisionStrategyTemplate = simulatedAssetDecisionStrategyTemplateDAO.getSimulatedAssetDecisionStrategyTemplateForId(id);
        simulatedAssetDecisionStrategyTemplateDAO.deleteSimulatedAssetDecisionStrategyTemplate(simulatedAssetDecisionStrategyTemplate);
    }

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long simulationId)
    {
        simulation = simulationDAO.getSimulationForId(simulationId);
    }

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {
        if (simulation == null) {
            simulation = new Simulation();
        }

        allMasterDecisionStrategyTemplates = new GenericSelectModel<DecisionStrategyTemplate>(decisionStrategyTemplateDAO.getAllMasterDecisionStrategyTemplates(),DecisionStrategyTemplate.class,"name","id",propertyAccess);
 //       allTimeSeries = new GenericSelectModel<TimeSeries>(timeSeriesDAO.getAllTimeSeries(),TimeSeries.class,"name","id",propertyAccess);
    }
    
    public String getSeriesName()
    {
    	if (simulatedAssetDecisionStrategyTemplate.getTimeSeries() != null) {
    		return simulatedAssetDecisionStrategyTemplate.getTimeSeries().getName() + " (" +  simulatedAssetDecisionStrategyTemplate.getTimeSeries().getId() + ")";
    	}
    	
    	return "";
    }
    
    public void setSeriesName(String seriesName)
    {
    	if (seriesName != null && !seriesName.isEmpty()) {
    		Long seriesId = Long.parseLong(seriesName.substring(seriesName.lastIndexOf('(') + 1, seriesName.lastIndexOf(')')));
    		
    		System.out.println("Found series id" + seriesId);
    		
    		simulatedAssetDecisionStrategyTemplate.setTimeSeries(timeSeriesDAO.getTimeSeriesForId(seriesId));
    	}
    	
    	this.seriesName = seriesName;
    }
    
    public List<String> onProvideCompletionsFromSeriesName(String partial)
    {
        List<String> seriesNames = new ArrayList<String>();
        
        List<TimeSeries> timeSeries = timeSeriesDAO.searchTimeSeries(partial, 100);

		for (TimeSeries ts : timeSeries) {
			seriesNames.add(ts.getName() + " (" + ts.getId() + ")");	
		}

        return seriesNames;
    }

    public void onSuccessFromSimulationForm()
    {
        simulationDAO.saveOrUpdate(simulation);
    }

    public void onSuccessFromSimulatedAssetDecisionStrategyTemplateForm()
    {
        simulationDAO.saveOrUpdate(simulation);
    }

    public Long onPassivate()
    {
        if (simulation != null) {
            return simulation.getId();
        }

        return null;
    }
}
*/