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

package ch.ksfx.web.pages;

import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.AssetPricingTimeRange;
import ch.ksfx.model.Observation;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.util.*;
import ch.ksfx.web.services.chart.ObservationChartGenerator;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.corelib.components.Grid;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.PropertyAccess;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.util.TextStreamResponse;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.annotation.Secured;

import java.util.Date;
import java.util.List;


@Secured({"ROLE_ADMIN"})
@Import(library = "context:scripts/viewtimeseries.js")
public class ViewTimeSeries
{
    private Logger logger = LoggerFactory.getLogger(ViewTimeSeries.class);
    
    
//------------------ BEGIN GRAPH PROPERTIES
    @Property
    @Persist
    private AssetPricingTimeRange assetPricingTimeRange;

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
    private Integer additionalMovingAverageSeconds;

    @Property
    @Persist
    private Boolean showAdditionalMovingAverage;

    @Property
    @Persist
    private Integer fastEmaTimeframesBack;

    @Property
    @Persist
    private Integer fastEmaTimeframeSeconds;

    @Property
    @Persist
    private Boolean fastEmaAddCurrentPrice;

    @Property
    @Persist
    private Boolean showFastEma;

    @Property
    @Persist
    private Integer middleEmaTimeframesBack;

    @Property
    @Persist
    private Integer middleEmaTimeframeSeconds;

    @Property
    @Persist
    private Boolean middleEmaAddCurrentPrice;

    @Property
    @Persist
    private Boolean showMiddleEma;

    @Property
    @Persist
    private Integer slowEmaTimeframesBack;

    @Property
    @Persist
    private Integer slowEmaTimeframeSeconds;

    @Property
    @Persist
    private Boolean slowEmaAddCurrentPrice;

    @Property
    @Persist
    private Boolean showSlowEma;
//------------------ END GRAPH PROPERTIES

    @Property
    private TimeSeries timeSeries;

    @Property
    private Observation observation;

    @Inject
    private PageRenderLinkSource linkSource;
    
    @Inject
    private PageRenderLinkSource pageRenderLinkSource;

    @Inject
    private TimeSeriesDAO timeSeriesDAO;

    @Inject
    private ObservationDAO observationDAO;

    @Inject
    private ComponentResources componentResources;

    @Inject
    private PropertyAccess propertyAccess;

    @Inject
    private ObservationChartGenerator observationChartGenerator;

    @Property
    private GenericSelectModel<AssetPricingTimeRange> allAssetPricingTimeRanges;

    @InjectComponent
    private Grid observationsGrid;

    @Property
    @Persist
    private String activeObservationId;

    @Property
    @Persist
    private Date activeObservationObservationTime;

    @SetupRender
    public void onSetupRender()
    {
        observationsGrid.getDataModel().getById("scalarValue").sortable(false);
    }

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long timeSeriesId)
    {
        timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);

		//TODO Move out of asset DAO
        allAssetPricingTimeRanges = new GenericSelectModel<AssetPricingTimeRange>(timeSeriesDAO.getAllAssetPricingTimeRanges(),AssetPricingTimeRange.class,"name","id",propertyAccess);
    
    	initializeGraphParameters();
    }
    
    private void initializeGraphParameters()
    {
    	if (assetPricingTimeRange == null) {
            assetPricingTimeRange = timeSeriesDAO.getAssetPricingTimeRangeForName("INTRADAY");
        }
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

        if (showFastEma == null) {
            showFastEma = Boolean.FALSE;
        }

        if (fastEmaTimeframesBack == null) {
            fastEmaTimeframesBack = 9;
        }

        if (fastEmaTimeframeSeconds == null) {
            fastEmaTimeframeSeconds = 1800;
        }

        if (fastEmaAddCurrentPrice == null) {
            fastEmaAddCurrentPrice = Boolean.FALSE;
        }

        if (showMiddleEma == null) {
            showMiddleEma = Boolean.FALSE;
        }

        if (middleEmaTimeframesBack == null) {
            middleEmaTimeframesBack = 22;
        }

        if (middleEmaTimeframeSeconds == null) {
            middleEmaTimeframeSeconds = 1800;
        }

        if (middleEmaAddCurrentPrice == null) {
            middleEmaAddCurrentPrice = Boolean.FALSE;
        }

        if (showSlowEma == null) {
            showSlowEma = Boolean.FALSE;
        }

        if (slowEmaTimeframesBack == null) {
            slowEmaTimeframesBack = 59;
        }

        if (slowEmaTimeframeSeconds == null) {
            slowEmaTimeframeSeconds = 1800;
        }

        if (slowEmaAddCurrentPrice == null) {
            slowEmaAddCurrentPrice = Boolean.FALSE;
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

    public CassandraGridDataSource getObservationDataSource()
    {
        CassandraGridDataSource cassandraGridDataSource = new CassandraGridDataSource(timeSeries);

        return cassandraGridDataSource;
    }

    public String getGraphLink()
    {
        return componentResources.createEventLink("graph").toURI();
    }

    public String getHugeGraphLink()
    {
        return componentResources.createEventLink("hugeGraph").toURI();
    }

    public StreamResponse onGraph()
    {
        Date date = new Date();

        System.out.println("[GRAPH] Creating new Graph start (" + timeSeries.getId() + ") " + date.toString());

        if (GraphSemaphore.available.availablePermits() == 0) {
            System.out.println("[GRAPH] Not creating graph, cannot get semaphore " + date.toString() + " (" + timeSeries.getId() + ")");
            return null;
        }

        try {
            GraphSemaphore.available.acquire();
            return new ImageStreamResponse("image/jpeg", observationChartGenerator.renderChart(generateGraph(), 700, 500));
        } catch (Exception e) {
            logger.error("[GRAPH] Cannot create graph " + date.toString(), e);
            return null;
        } finally {
            System.out.println("[GRAPH] Creating new Graph finished (" + timeSeries.getId() + ") " + date.toString());
            GraphSemaphore.available.release();
        }
    }

    public StreamResponse onHugeGraph()
    {
        if (GraphSemaphore.available.availablePermits() == 0) {
            logger.info("Not creating graph, cannot get semaphore");
            return new TextStreamResponse("text/plain", "UTF-8", "Cannot create graph, graph creation pending, please try again later");
        }

        try {
            GraphSemaphore.available.acquire();
            return new ImageStreamResponse("image/jpeg", observationChartGenerator.renderChart(generateGraph(), 1600, 1280));
        } catch (Exception e) {
            logger.error("Cannot create huge graph", e);
            return null;
        } finally {
            GraphSemaphore.available.release();
        }
    }
    
    public JFreeChart generateGraph()
    {
        Date currentDate = new Date();

        JFreeChart jFreeChart = observationChartGenerator.generateChart(timeSeries,assetPricingTimeRange.getStartDate(),currentDate);
        System.out.println("[GRAPH] Finished generating jFReeChart (" + timeSeries.getId() + ")");

        TimePeriodValuesCollection timePeriodValuesCollection = (TimePeriodValuesCollection) jFreeChart.getXYPlot().getDataset();

        if(showBollingerTime) {
            List<TimePeriodValues> bollingerTimeValues = observationChartGenerator.getBollingerBands(timeSeries, assetPricingTimeRange.getStartDate(), currentDate, bollingerTimeSeconds, 2);

            for (TimePeriodValues tpv:bollingerTimeValues) {
                timePeriodValuesCollection.addSeries(tpv);
            }
        }

        if (showBollingerTick) {
            List<TimePeriodValues> bollingerTickValues = observationChartGenerator.getBollingerBandsForTicksBack(timeSeries,assetPricingTimeRange.getStartDate(), currentDate, bollingerTickTicks, 2);

            for (TimePeriodValues tpv:bollingerTickValues) {
                timePeriodValuesCollection.addSeries(tpv);
            }
        }

        if (showBollingerTimeframe) {
            List<TimePeriodValues> bollingerTimeframeValues = observationChartGenerator.getBollingerTimeframeBands(timeSeries,assetPricingTimeRange.getStartDate(), currentDate, bollingerTimeframeSeconds, bollingerTimeframeNumber, 2);

            for (TimePeriodValues tpv:bollingerTimeframeValues) {
                timePeriodValuesCollection.addSeries(tpv);
            }
        }

        if (showFastEma) {
            logger.info("SHOW FAST EMA REQUESTED");
            timePeriodValuesCollection.addSeries(observationChartGenerator.getExponentialMovingAverage(timeSeries, assetPricingTimeRange.getStartDate(), currentDate, fastEmaTimeframeSeconds, fastEmaTimeframesBack, fastEmaAddCurrentPrice));
        }

        if (showMiddleEma) {
            logger.info("SHOW MIDDLE EMA REQUESTED");
            timePeriodValuesCollection.addSeries(observationChartGenerator.getExponentialMovingAverage(timeSeries, assetPricingTimeRange.getStartDate(), currentDate, middleEmaTimeframeSeconds, middleEmaTimeframesBack, middleEmaAddCurrentPrice));
        }

        if (showSlowEma) {
            logger.info("SHOW SLOW EMA REQUESTED");
            timePeriodValuesCollection.addSeries(observationChartGenerator.getExponentialMovingAverage(timeSeries, assetPricingTimeRange.getStartDate(), currentDate, slowEmaTimeframeSeconds, slowEmaTimeframesBack, slowEmaAddCurrentPrice));
        }

        if (showAdditionalMovingAverage) {
            timePeriodValuesCollection.addSeries(observationChartGenerator.getMovingAverage(timeSeries, assetPricingTimeRange.getStartDate(), currentDate, additionalMovingAverageSeconds));
        }

        System.out.println("[GRAPH] Finished adding additinal series (" + timeSeries.getId() + ")");

        return jFreeChart;
    }

//    public JFreeChart generateGraph()
//    {
//        Date currentDate = new Date();
//
//        JFreeChart jFreeChart = observationChartGenerator.generateChart(timeSeries,assetPricingTimeRange.getStartDate(),currentDate);
//
//        TimePeriodValuesCollection timePeriodValuesCollection = (TimePeriodValuesCollection) jFreeChart.getXYPlot().getDataset();
//
//        return jFreeChart;
//    }

    public boolean getIsDoubleSeries()
    {
        return timeSeries.getTimeSeriesType().getName().equals("Standard Double Series");
    }

    public Long onPassivate()
    {
        if (timeSeries != null) {
            return timeSeries.getId();
        }

        return null;
    }
    
    public void onActionFromDeleteObservation(String observationSourceId, String observationTime)
    {
    	observationDAO.deleteObservation(observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(timeSeries.getId().intValue(), new Date(Long.parseLong(observationTime)), observationSourceId));
    }

    public void onActionFromDeleteAllObservations()
    {
        observationDAO.deleteAllObservationsForTimeSeries(timeSeries);
    }
    
    public String getIframeInfoLink()
    {
    	return pageRenderLinkSource.createPageRenderLinkWithContext("admin/note/viewnotesforentityplain", getInfoContextParameters()).toURI();	
    }
    
    public Object[] getInfoContextParameters()
    {
        if (timeSeries != null) {
            return new Object[]{timeSeries.getId(),
                    null,
                    null,null,null};
        }

        return null;
    }

    public void onActionFromOpenValue(String observationSourceId, String observationTime)
    {
        activeObservationId = observationSourceId;
        activeObservationObservationTime = new Date(Long.parseLong(observationTime));
    }

    public boolean getHasActiveObservation()
    {
        return activeObservationId != null;
    }

    public void onActionFromCloseParameterWindow()
    {
        activeObservationId = null;
        activeObservationObservationTime = null;
    }

    public void onActionFromCancelParameterWindow()
    {
        onActionFromCloseParameterWindow();
    }

    public String getIframeLink()
    {
        return linkSource.createPageRenderLinkWithContext("observation/" + timeSeries.getTimeSeriesType().getObservationView(), getEditingContextParameters()).toURI();
    }

    public Object[] getEditingContextParameters()
    {
        if (activeObservationId != null) {
            return new Object[]{getActiveObservation().getTimeSeriesId(),
                    formatToISO8601TimeAndDateString(getActiveObservation().getObservationTime()),
                    getActiveObservation().getSourceId()};
        }

        return null;
    }

    public String formatToISO8601TimeAndDateString(Date date)
    {
        return DateFormatUtil.formatToISO8601TimeAndDateString(date);
    }

    public Observation getActiveObservation()
    {
        if (activeObservationId == null) {
            return null;
        }

        return observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(timeSeries.getId().intValue(), activeObservationObservationTime,activeObservationId);
    }

    public boolean getIsCurrentObservationActiveObservation()
    {
        return observation.getSourceId().equals(activeObservationId);
    }
}
