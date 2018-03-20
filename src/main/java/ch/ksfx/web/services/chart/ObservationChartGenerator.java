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

package ch.ksfx.web.services.chart;

import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.Observation;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.util.calc.AssetPriceSparser;
import ch.ksfx.util.calc.BollingerCalculator;
import ch.ksfx.util.calc.MovingAverageCalculator;
import ch.ksfx.util.calc.ObservationDoubleValueComparator;
import org.apache.commons.lang.time.DateUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class ObservationChartGenerator
{
    private Logger logger = LoggerFactory.getLogger(ObservationChartGenerator.class);

    private TimeSeriesDAO timeSeriesDAO;
    private ObservationDAO observationDAO;

    public ObservationChartGenerator(TimeSeriesDAO timeSeriesDAO, ObservationDAO observationDAO)
    {
        this.timeSeriesDAO = timeSeriesDAO;
        this.observationDAO = observationDAO;
    }

//    //XXX Normally a finer graph is needed, need to profile the performance somewhen
    public List<Observation> simplifyAssetPrices(List<Observation> assetPrices)
    {
        if (assetPrices.size() > 1000) {

            List<Observation> assetPricesNew = new ArrayList<Observation>();

            Integer mod = assetPrices.size() / 1000;
            Integer modRun = 0;

            for (Observation ap : assetPrices) {
                if (modRun % mod == 0) {
                    assetPricesNew.add(ap);
                }

                modRun++;
            }

            assetPrices = assetPricesNew;
        }

        return assetPrices;
    }

    public List<TimePeriodValues> getBollingerBandsForTicksBack(TimeSeries asset, Date startDate, Date endDate, Integer ticksBack, Integer bollingerK)
    {
        List<TimePeriodValues> bollingerBands = new ArrayList<TimePeriodValues>();

        List<Observation> assetPrices = observationDAO.queryObservationsSparse(asset.getId().intValue(), startDate, endDate);

        //Get some earlier prices to calc simple moving average
        List<Observation> movingAveragePrices = observationDAO.queryObservationsSparse(asset.getId().intValue(), (startDate.getTime() == 0l)?startDate:DateUtils.addSeconds(startDate, ticksBack * -1), endDate);

        assetPrices = simplifyAssetPrices(assetPrices);

        TimePeriodValues middleBand = new TimePeriodValues("SMA" + ticksBack + "Ticks "  + asset.getName());

        TimePeriodValues topBand = new TimePeriodValues("TicksBollinger Top K" + bollingerK + " "  + asset.getName());
        TimePeriodValues bottomBand = new TimePeriodValues("TicksBollinger Bottom K" + bollingerK + " "  + asset.getName());

        List<Observation> relevantMovingAveragePrices = null;
        Double movingAverage = null;
        Double standardDeviation = null;

        Integer filterSizer = 0;

        for (Observation ap : assetPrices) {

            if ((movingAveragePrices.indexOf(ap) - ticksBack) < 0) {
                continue;
            }

            relevantMovingAveragePrices = movingAveragePrices.subList(movingAveragePrices.indexOf(ap) - ticksBack, movingAveragePrices.indexOf(ap));

            movingAverage = MovingAverageCalculator.calculateMovingAverageObservation(relevantMovingAveragePrices, true);

            middleBand.add(new SimpleTimePeriod(ap.getObservationTime(), ap.getObservationTime()), movingAverage);

            if (relevantMovingAveragePrices.size() > 1) {
                standardDeviation = BollingerCalculator.calcStandardDeviationObservation(relevantMovingAveragePrices, true);

                topBand.add(new SimpleTimePeriod(ap.getObservationTime(), ap.getObservationTime()), movingAverage + (standardDeviation * bollingerK));
                bottomBand.add(new SimpleTimePeriod(ap.getObservationTime(), ap.getObservationTime()), movingAverage - (standardDeviation * bollingerK));
            }
        }

        bollingerBands.add(middleBand);

        bollingerBands.add(topBand);
        bollingerBands.add(bottomBand);

        return bollingerBands;
    }

    public TimePeriodValues getMovingAverage(TimeSeries asset, Date startDate, Date endDate, Integer movingAverageSeconds)
    {
        TimePeriodValues movingAverageBand = new TimePeriodValues("SMA" + (movingAverageSeconds / 60 / 60 / 24) + "DAYS "  + asset.getName());

        List<Observation> assetPrices = observationDAO.queryObservationsSparse(asset.getId().intValue(), startDate, endDate);

        //Get some earlier prices to calc simple moving average
        List<Observation> movingAveragePrices = observationDAO.queryObservationsSparse(asset.getId().intValue(), (startDate.getTime() == 0l)?startDate:DateUtils.addSeconds(startDate, movingAverageSeconds * -1), endDate);

        assetPrices = simplifyAssetPrices(assetPrices);

        Integer filterSizer = 0;
        List<Observation> relevantMovingAveragePrices = null;
        Double movingAverage = null;

        for (Observation ap : assetPrices) {

            Date referenceDate = DateUtils.addSeconds(ap.getObservationTime(), movingAverageSeconds * -1);

            if (movingAveragePrices.get(0).getObservationTime().after(referenceDate)) {
                continue;
            }

            for (Integer iI = filterSizer; iI < movingAveragePrices.size(); iI++) {
                if (movingAveragePrices.get(iI + 1).getObservationTime().after(referenceDate)) {
                    //We went too far, begin index was the last one
                    filterSizer = iI;
                    break;
                }
            }

            relevantMovingAveragePrices = movingAveragePrices.subList(filterSizer, movingAveragePrices.indexOf(ap));

            movingAverage = MovingAverageCalculator.calculateMovingAverageObservation(relevantMovingAveragePrices, true);

            movingAverageBand.add(new SimpleTimePeriod(ap.getObservationTime(), ap.getObservationTime()), movingAverage);
        }

        return movingAverageBand;
    }

    public TimePeriodValues getExponentialMovingAverage(TimeSeries asset, Date startDate, Date endDate, Integer emaTimeframeSeconds, Integer emaTimeframesBack, Boolean addCurrentPrice)
    {
        TimePeriodValues movingAverageBand = new TimePeriodValues("EMA" + emaTimeframesBack + "TIMEFRAMES (" + emaTimeframeSeconds +") "  + asset.getName());

        List<Observation> assetPrices = observationDAO.queryObservationsSparse(asset.getId().intValue(), startDate, endDate);

        //Get some earlier prices to calc exponential moving average
        List<Observation> movingAveragePrices = observationDAO.queryObservationsSparse(asset.getId().intValue(), (startDate.getTime() == 0l)?startDate:DateUtils.addSeconds(startDate, (emaTimeframesBack * emaTimeframeSeconds * 2) * -1), endDate);

        //logger.info("Calculated date which is needed " + DateUtils.addDays(startDate, emaDaysBack * -1));

        assetPrices = simplifyAssetPrices(assetPrices);

        Integer filterSizer = 0;
        List<Observation> relevantMovingAveragePrices = null;
        Double movingAverage = null;

        for (Observation ap : assetPrices) {

            Date referenceDate = DateUtils.addSeconds(ap.getObservationTime(), (emaTimeframesBack * emaTimeframeSeconds) * -1);

            if (movingAveragePrices.get(0).getObservationTime().after(referenceDate)) {
                logger.info("Could not calc EMA earliest available date is " + movingAveragePrices.get(0).getObservationTime());
                continue;
            }

            /*
            for (Integer iI = filterSizer; iI < movingAveragePrices.size(); iI++) {
                if (movingAveragePrices.get(iI + 1).getPricingTime().after(referenceDate)) {
                    //We went too far, begin index was the last one
                    filterSizer = iI;
                    break;
                }
            }
            */

            relevantMovingAveragePrices = movingAveragePrices.subList(filterSizer, movingAveragePrices.indexOf(ap));

            logger.info("Calculating EMA");
            movingAverage = MovingAverageCalculator.calcExponentialMovingAverageObservation(emaTimeframeSeconds, emaTimeframesBack, relevantMovingAveragePrices, true, addCurrentPrice, null);

            movingAverageBand.add(new SimpleTimePeriod(ap.getObservationTime(), ap.getObservationTime()), movingAverage);
        }

        return movingAverageBand;
    }

    public List<TimePeriodValues> getBollingerBands(TimeSeries asset, Date startDate, Date endDate, Integer movingAverageSeconds, Integer bollingerK)
    {
        List<TimePeriodValues> bollingerBands = new ArrayList<TimePeriodValues>();

        List<Observation> assetPrices = observationDAO.queryObservationsSparse(asset.getId().intValue(), startDate, endDate);

        //Get some earlier prices to calc simple moving average
        List<Observation> movingAveragePrices = observationDAO.queryObservationsSparse(asset.getId().intValue(), (startDate.getTime() == 0l)?startDate:DateUtils.addSeconds(startDate, movingAverageSeconds * -1), endDate);

        assetPrices = simplifyAssetPrices(assetPrices);

        TimePeriodValues middleBand = new TimePeriodValues("SMA" + (movingAverageSeconds / 60 / 60 / 24) + "DAYS "  + asset.getName());

        TimePeriodValues topBand = new TimePeriodValues("TimeBollinger Top K" + bollingerK + " "  + asset.getName());
        TimePeriodValues bottomBand = new TimePeriodValues("TimeBollinger Bottom K" + bollingerK + " "  + asset.getName());

        List<Observation> relevantMovingAveragePrices = null;
        Double movingAverage = null;
        Double standardDeviation = null;

        Integer filterSizer = 0;

        for (Observation ap : assetPrices) {

            Date referenceDate = DateUtils.addSeconds(ap.getObservationTime(), movingAverageSeconds * -1);

            if (movingAveragePrices.get(0).getObservationTime().after(referenceDate)) {
                continue;
            }

            for (Integer iI = filterSizer; iI < movingAveragePrices.size(); iI++) {
                if (movingAveragePrices.get(iI + 1).getObservationTime().after(referenceDate)) {
                    //We went to far, begin index was the last one
                    filterSizer = iI;
                    break;
                }
            }

            relevantMovingAveragePrices = movingAveragePrices.subList(filterSizer, movingAveragePrices.indexOf(ap));

            movingAverage = MovingAverageCalculator.calculateMovingAverageObservation(relevantMovingAveragePrices, true);

            middleBand.add(new SimpleTimePeriod(ap.getObservationTime(), ap.getObservationTime()), movingAverage);

            if (relevantMovingAveragePrices.size() > 1) {
                standardDeviation = BollingerCalculator.calcStandardDeviationObservation(relevantMovingAveragePrices, true);

                topBand.add(new SimpleTimePeriod(ap.getObservationTime(), ap.getObservationTime()), movingAverage + (standardDeviation * bollingerK));
                bottomBand.add(new SimpleTimePeriod(ap.getObservationTime(), ap.getObservationTime()), movingAverage - (standardDeviation * bollingerK));
            }
        }

        bollingerBands.add(middleBand);

        bollingerBands.add(topBand);
        bollingerBands.add(bottomBand);

        return bollingerBands;
    }

    public List<TimePeriodValues> getBollingerTimeframeBands(TimeSeries asset, Date startDate, Date endDate, Integer timeframeSeconds, Integer numberOfTimeframes, Integer bollingerK)
    {
        List<TimePeriodValues> bollingerBands = new ArrayList<TimePeriodValues>();

        List<Observation> assetPrices = observationDAO.queryObservationsSparse(asset.getId().intValue(), startDate, endDate);

        //Get some earlier prices to calc simple moving average TODO earlier prices
        List<Observation> movingAveragePrices =  AssetPriceSparser.sparseAssetPriceWithoutAveragingObservation(observationDAO.queryObservationsSparse(asset.getId().intValue(), startDate, endDate), timeframeSeconds);

        assetPrices = simplifyAssetPrices(assetPrices);

        TimePeriodValues middleBand = new TimePeriodValues("SMA" + (numberOfTimeframes / 60 / 60 / 24) + " "  + asset.getName());

        TimePeriodValues topBand = new TimePeriodValues("TimeBollinger Top K" + bollingerK + " "  + asset.getName());
        TimePeriodValues bottomBand = new TimePeriodValues("TimeBollinger Bottom K" + bollingerK + " "  + asset.getName());

        List<Observation> relevantMovingAveragePrices = null;
        Double movingAverage = null;
        Double standardDeviation = null;

        Integer filterSizer = 0;

        for (Observation ap : assetPrices) {

            for (Integer iI = filterSizer; (iI + 1) < movingAveragePrices.size(); iI++) {
                if (movingAveragePrices.get(iI + 1).getObservationTime().after(ap.getObservationTime())) {
                    //We went to far, begin index was the last one
                    filterSizer = iI;
                    break;
                }
            }

            if ((filterSizer - numberOfTimeframes) < 0) {
                continue;
            }

            relevantMovingAveragePrices = movingAveragePrices.subList((filterSizer - numberOfTimeframes), filterSizer);

            movingAverage = MovingAverageCalculator.calculateMovingAverageObservation(relevantMovingAveragePrices, true);

            middleBand.add(new SimpleTimePeriod(ap.getObservationTime(), ap.getObservationTime()), movingAverage);

            if (relevantMovingAveragePrices.size() > 1) {
                standardDeviation = BollingerCalculator.calcStandardDeviationObservation(relevantMovingAveragePrices, true);

                topBand.add(new SimpleTimePeriod(ap.getObservationTime(), ap.getObservationTime()), movingAverage + (standardDeviation * bollingerK));
                bottomBand.add(new SimpleTimePeriod(ap.getObservationTime(), ap.getObservationTime()), movingAverage - (standardDeviation * bollingerK));
            }
        }

        bollingerBands.add(middleBand);

        bollingerBands.add(topBand);
        bollingerBands.add(bottomBand);

        return bollingerBands;
    }

    public void setRange(JFreeChart jFreeChart, List<Observation> observations)
    {
        //This has to be sorted by value!!
        List<Observation> sortedObservations = new ArrayList<Observation>(observations);
        Collections.sort(sortedObservations, new ObservationDoubleValueComparator());

        XYPlot xyp = jFreeChart.getXYPlot();

        //xyp.addAnnotation(new XYTextAnnotation("Fummel", dataset.getX(0,10).doubleValue(), dataset.getY(0, 10).doubleValue()));

        NumberAxis axis = (NumberAxis) xyp.getRangeAxis();

        Double width = Double.parseDouble(sortedObservations.get(sortedObservations.size() - 1).getScalarValue()) - Double.parseDouble(sortedObservations.get(0).getScalarValue());

        axis.setRange(Double.parseDouble(sortedObservations.get(0).getScalarValue()) - width / 4, Double.parseDouble(sortedObservations.get(sortedObservations.size() - 1).getScalarValue()) + width / 4);

        DateAxis daxis = new DateAxis("Time Axis");

        daxis.setVerticalTickLabels(true);

        daxis.setTickMarkPosition(DateTickMarkPosition.START);
        daxis.setAutoRange(true);
        daxis.setDateFormatOverride(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"));
        xyp.setDomainAxis(daxis);

    }

    public JFreeChart generateChart(TimeSeries timeSeries, Date startDate, Date endDate)
    {
        List<Observation> observations = observationDAO.queryObservationsSparse(timeSeries.getId().intValue(), startDate, endDate);

        if (observations == null || observations.size() == 0 || observations.isEmpty()) {
            Observation o = observationDAO.getLastObservationForTimeSeriesId(timeSeries.getId().intValue());
            Long diffInMillis = endDate.getTime() - startDate.getTime();
            startDate = DateUtils.addMilliseconds(o.getObservationTime(),diffInMillis.intValue() * -1);

            System.out.println("[GRAPH] Found 0 Observations trying new query (" + timeSeries.getId() + ") " + "Startdate: " + startDate.toString() + " End date: " + endDate.toString());
            observations = observationDAO.queryObservationsSparse(timeSeries.getId().intValue(), startDate, DateUtils.addMilliseconds(o.getObservationTime(), 1000));
        }

        System.out.println("[GRAPH] Observations size: " + observations.size() + " for (" + timeSeries.getId() + ")");

        //assetPrices = simplifyAssetPrices(assetPrices);

        TimePeriodValues assetPriceTimePeriodValues = new TimePeriodValues(timeSeries.getName());
        TimePeriodValuesCollection dataset = new TimePeriodValuesCollection();

        for (Observation o : observations) {
            assetPriceTimePeriodValues.add(new SimpleTimePeriod(o.getObservationTime(), o.getObservationTime()), Double.parseDouble(o.getScalarValue()));
        }

        dataset.addSeries(assetPriceTimePeriodValues);

        JFreeChart jFreeChart = ChartFactory.createXYLineChart("Performance","Time","Value", dataset, PlotOrientation.VERTICAL,true, false, false);

        setRange(jFreeChart, observations);

        return jFreeChart;
    }

    public BufferedImage renderChart(JFreeChart jFreeChart, Integer width, Integer height)
    {
        jFreeChart.setBackgroundPaint(Color.white);

        System.out.println("[GRAPH] Creating buffered image");
        BufferedImage bi = jFreeChart.createBufferedImage(width,height);
        System.out.println("[GRAPH] Finished Creating buffered image");

        return bi;
    }
}
