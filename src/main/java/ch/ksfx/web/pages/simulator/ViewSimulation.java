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
import ch.ksfx.model.OrderType;
import ch.ksfx.model.simulation.*;
import ch.ksfx.util.GraphSemaphore;
import ch.ksfx.util.ImageStreamResponse;
import ch.ksfx.util.simulation.SimulationHelper;
import ch.ksfx.web.services.chart.ObservationChartGenerator;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.util.TextStreamResponse;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.annotation.Secured;
import java.util.Date;
import java.util.List;


@Deprecated //To be removed
@Secured({"ROLE_ADMIN"})
public class ViewSimulation
{
    @Inject
    private SimulationDAO simulationDAO;

    @Inject
    private SimulatedAssetDecisionStrategyTemplateDAO simulatedAssetDecisionStrategyTemplateDAO;

    @Inject
    private ComponentResources componentResources;

    @Inject
    private ObservationChartGenerator observationChartGenerator;

    @Property
    private Simulation simulation;

    @Property
    private SimulatedDecision simulatedDecision;

    @Property
    private SimulatedTrade simulatedTrade;

    @Property
    private SimulationOptimizationResult simulationOptimizationResult;

    @Property
    private SimulatedAssetDecisionStrategyTemplate simulatedAssetDecisionStrategyTemplate;

    @Property
    @Persist
    private Integer bollingerTimeSeconds;

    @Property
    @Persist
    private Boolean showBollingerTime;

    @Property
    @Persist
    private Integer bollingerTickTicks;

    @Property
    @Persist
    private Boolean showBollingerTick;

    @Property
    @Persist
    private Integer bollingerTimeframeSeconds;

    @Property
    @Persist
    private Integer bollingerTimeframeNumber;

    @Property
    @Persist
    private Boolean showBollingerTimeframe;

    @Property
    @Persist
    private Integer additionalMovingAverageSeconds;

    @Property
    @Persist
    private Boolean showAdditionalMovingAverage;

    private Logger logger = LoggerFactory.getLogger(ViewSimulation.class);

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long simulationId)
    {
        this.simulation = simulationDAO.getSimulationForId(simulationId);

        if (showBollingerTick == null) {
            showBollingerTick = Boolean.FALSE;
        }

        if (showBollingerTime == null) {
            showBollingerTime = Boolean.FALSE;
        }

        if (showAdditionalMovingAverage == null) {
            showAdditionalMovingAverage = Boolean.FALSE;
        }

        if (bollingerTickTicks == null) {
            bollingerTickTicks = 1000;
        }

        if (bollingerTimeSeconds == null) {
            bollingerTimeSeconds = 86400;
        }

        if (additionalMovingAverageSeconds == null) {
            additionalMovingAverageSeconds = 40000;
        }

        if (showBollingerTimeframe == null) {
            showBollingerTimeframe = Boolean.FALSE;
        }

        if (bollingerTimeframeNumber == null) {
            bollingerTimeframeNumber = 30;
        }

        if (bollingerTimeframeSeconds == null) {
            bollingerTimeframeSeconds = 1800;
        }
    }

    public String getGraphLink()
    {
        return componentResources.createEventLink("graph", simulatedAssetDecisionStrategyTemplate.getId()).toURI();
    }

    public String getHugeGraphLink()
    {
        return componentResources.createEventLink("hugeGraph", simulatedAssetDecisionStrategyTemplate.getId()).toURI();
    }

    public JFreeChart generateGraph(Long simulatedAssetDecisionStrategyTemplateId)
    {
        SimulatedAssetDecisionStrategyTemplate simulatedAssetDecisionStrategyTemplate = simulatedAssetDecisionStrategyTemplateDAO.getSimulatedAssetDecisionStrategyTemplateForId(simulatedAssetDecisionStrategyTemplateId);

        JFreeChart jFreeChart = observationChartGenerator.generateChart(simulatedAssetDecisionStrategyTemplate.getTimeSeries(), simulation.getPricingStartDate(), simulation.getPricingEndDate());

        TimePeriodValuesCollection timePeriodValuesCollection = (TimePeriodValuesCollection) jFreeChart.getXYPlot().getDataset();

        if (showBollingerTick) {
            List<TimePeriodValues> bollinger = observationChartGenerator.getBollingerBandsForTicksBack(simulatedAssetDecisionStrategyTemplate.getTimeSeries(),
                    simulation.getPricingStartDate(), simulation.getPricingEndDate(), bollingerTickTicks, 2);

            for (TimePeriodValues tpv : bollinger) {
                timePeriodValuesCollection.addSeries(tpv);
            }
        }

        if (showBollingerTime) {
            List<TimePeriodValues> bollinger = observationChartGenerator.getBollingerBands(simulatedAssetDecisionStrategyTemplate.getTimeSeries(),
                    simulation.getPricingStartDate(), simulation.getPricingEndDate(), bollingerTimeSeconds, 2);

            for (TimePeriodValues tpv : bollinger) {
                timePeriodValuesCollection.addSeries(tpv);
            }
        }

        if (showBollingerTimeframe) {
            List<TimePeriodValues> bollingerTimeframeValues = observationChartGenerator.getBollingerTimeframeBands(simulatedAssetDecisionStrategyTemplate.getTimeSeries(),simulation.getPricingStartDate(), simulation.getPricingEndDate(), bollingerTimeframeSeconds, bollingerTimeframeNumber, 2);

            for (TimePeriodValues tpv:bollingerTimeframeValues) {
                timePeriodValuesCollection.addSeries(tpv);
            }
        }

        if (showAdditionalMovingAverage) {
            timePeriodValuesCollection.addSeries(observationChartGenerator.getMovingAverage(simulatedAssetDecisionStrategyTemplate.getTimeSeries(),
                    simulation.getPricingStartDate(), simulation.getPricingEndDate(), additionalMovingAverageSeconds));
        }

        List<TimePeriodValues> trades = observationChartGenerator.getTrades(SimulationHelper.buildTradesForSimulatedTrades(simulatedAssetDecisionStrategyTemplate.getSimulatedTrades()));

        for (TimePeriodValues tpv : trades) {
            timePeriodValuesCollection.addSeries(tpv);
        }

        return jFreeChart;
    }

    public Integer getLostTrades()
    {
        Integer lost = 0;

        for (SimulatedTrade simulatedTrade : simulatedAssetDecisionStrategyTemplate.getClosedTrades()) {
            if (simulatedTrade.getProfitLoss() < 0) {
                lost++;
            }
        }

        return lost;
    }

    public Integer getWonTrades()
    {
        Integer won = 0;

        for (SimulatedTrade simulatedTrade : simulatedAssetDecisionStrategyTemplate.getClosedTrades()) {
            if (simulatedTrade.getProfitLoss() > 0) {
                won++;
            }
        }

        return won;
    }

    public GridDataSource getSimulatedDecisions()
    {
        return simulatedAssetDecisionStrategyTemplateDAO.getSimulatedDecisionDataSourceForSimulatedAssetDecisionStrategyTemplate(simulatedAssetDecisionStrategyTemplate);
    }

    public Double getTotalProfitLoss()
    {
        Double sum = 0.0;

        for (SimulatedTrade simulatedTrade : simulatedAssetDecisionStrategyTemplate.getClosedTrades()) {
            sum += simulatedTrade.getProfitLoss();
        }

        return sum;
    }

    public StreamResponse onGraph(Long simulatedAssetDecisionStrategyTemplateId)
    {
        if (GraphSemaphore.available.availablePermits() == 0) {
            logger.info("Not creating graph, cannot get semaphore");
            return null;
        }

        try {
            GraphSemaphore.available.acquire();
            return new ImageStreamResponse("image/jpeg", observationChartGenerator.renderChart(generateGraph(simulatedAssetDecisionStrategyTemplateId), 700, 400));
        } catch (Exception e) {
            return null;
        } finally {
            GraphSemaphore.available.release();
        }
    }

    public StreamResponse onHugeGraph(Long simulatedAssetDecisionStrategyTemplateId)
    {
        if (GraphSemaphore.available.availablePermits() == 0) {
            return new TextStreamResponse("text/plain", "UTF-8", "Cannot create graph, graph creation pending, please try again later");
        }

        try {
            GraphSemaphore.available.acquire();
            return new ImageStreamResponse("image/jpeg", observationChartGenerator.renderChart(generateGraph(simulatedAssetDecisionStrategyTemplateId), 1600, 1280));
        } catch (Exception e) {
            return null;
        } finally {
            GraphSemaphore.available.release();
        }
    }

    public Date getTradeOpening()
    {
        if (simulatedTrade.getSimulatedMarketOrders() != null && !simulatedTrade.getSimulatedMarketOrders().isEmpty()) {
            return simulatedTrade.getSimulatedMarketOrders().get(0).getExecutionDate();
        }

        return null;
    }

    public OrderType getTradingType()
    {
        if (simulatedTrade.getSimulatedMarketOrders() != null && !simulatedTrade.getSimulatedMarketOrders().isEmpty()) {
            return simulatedTrade.getSimulatedMarketOrders().get(0).getOrderType();
        }

        return null;
    }

    public Double getOpeningPrice()
    {
        if (simulatedTrade.getSimulatedMarketOrders() != null && !simulatedTrade.getSimulatedMarketOrders().isEmpty()) {
            return simulatedTrade.getSimulatedMarketOrders().get(0).getExecutionPrice();
        }

        return null;
    }

    public Double getClosingPrice()
    {
        if (simulatedTrade.getSimulatedMarketOrders() != null && !simulatedTrade.getSimulatedMarketOrders().isEmpty() && simulatedTrade.getSimulatedMarketOrders().size() > 1) {
            return simulatedTrade.getSimulatedMarketOrders().get(1).getExecutionPrice();
        }

        return null;
    }

    public String getTradingTime()
    {
        Long time = simulatedTrade.getTradingTime();

        Long seconds = time / 1000;

        if (seconds > 3600) {
            return ((seconds / 60.) / 60.) + " Hours";
        }

        if (seconds > 60) {
            return (seconds / 60.) + " Minutes";
        }

        return seconds + " Seconds";
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