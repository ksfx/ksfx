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
import ch.ksfx.dao.simulation.SimulatedTradeDAO;
import ch.ksfx.dao.simulation.SimulationRawDataDAO;
import ch.ksfx.model.simulation.SimulatedAssetDecisionStrategyTemplate;
import ch.ksfx.model.simulation.SimulatedTrade;
import org.apache.commons.lang.time.DateUtils;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.annotation.Secured;
import java.util.Date;


@Deprecated //To be removed
@Secured({"ROLE_ADMIN"})
public class SimulationRawDataViewer
{
    private Logger logger = LoggerFactory.getLogger(SimulationRawDataViewer.class);

    @Inject
    private SimulationRawDataDAO simulationRawDataDAO;

    @Inject
    private SimulatedTradeDAO simulatedTradeDAO;

//    @Inject
//    private ChartGenerator chartGenerator;

    @Inject
    private ComponentResources componentResources;

    @Inject
    private SimulatedAssetDecisionStrategyTemplateDAO simulatedAssetDecisionStrategyTemplateDAO;

    @Property
    private SimulatedAssetDecisionStrategyTemplate simulatedAssetDecisionStrategyTemplate;

    @Property
    private SimulatedTrade simulatedTrade;

    @Property
    @Persist
    private Date fromDate;

    @Property
    @Persist
    private Date toDate;

    @Persist
    private Long oldTradeId;

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long simulatedAssetDecisionStrategyTemplateId, Long simulatedTradeId)
    {
        this.simulatedAssetDecisionStrategyTemplate = simulatedAssetDecisionStrategyTemplateDAO.getSimulatedAssetDecisionStrategyTemplateForId(simulatedAssetDecisionStrategyTemplateId);
        this.simulatedTrade = simulatedTradeDAO.getSimulatedTradeForId(simulatedTradeId);

        if (oldTradeId == null || !oldTradeId.equals(simulatedTrade.getId())) {
            if (simulatedTrade.getSimulatedMarketOrders().get(0).getExecutionDate() != null) {
                logger.info("=============== Setting new dates ================ ot " + oldTradeId + " nt " + simulatedTrade.getId());
                fromDate = DateUtils.addSeconds(simulatedTrade.getSimulatedMarketOrders().get(0).getExecutionDate(), -10800);
                toDate = DateUtils.addSeconds(simulatedTrade.getSimulatedMarketOrders().get(0).getExecutionDate(), 10800);
                oldTradeId = simulatedTrade.getId();
            }
        }

        logger.info("Fromdate is now " + fromDate);
        logger.info("Todate is now " + toDate);
    }

    public Long[] onPassivate()
    {
        Long[] param = new Long[2];

        if (simulatedAssetDecisionStrategyTemplate != null) {
            param[0] = simulatedAssetDecisionStrategyTemplate.getId();
        }

        if (simulatedTrade != null) {
            param[1] = simulatedTrade.getId();
        }

        return param;
    }

    public void onSuccessFromRawDataViewerForm()
    {
        logger.info("================== Found fromdate " + fromDate);
        logger.info("================== Found todate " + toDate);
    }



    public String getHugeGraphLink()
    {
        return componentResources.createEventLink("hugeGraph").toURI();
    }

    public Long getSystemTime()
    {
        return System.currentTimeMillis();
    }

    public String getGraphLink()
        {
        return componentResources.createEventLink("graph",simulatedAssetDecisionStrategyTemplate.getId()).toURI();
        }
}
*/