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
package ch.ksfx.web.services.chart;

import ch.ksfx.dao.TradeDAO;
import ch.ksfx.model.Asset;
import ch.ksfx.model.AssetPrice;
import ch.ksfx.model.MarketOrder;
import ch.ksfx.model.Trade;
import ch.ksfx.model.simulation.SimulationRawData;
import ch.ksfx.util.calc.AssetPricePriceComparator;
import ch.ksfx.util.calc.AssetPriceSparser;
import ch.ksfx.util.calc.BollingerCalculator;
import ch.ksfx.util.calc.MovingAverageCalculator;
import org.apache.commons.lang.time.DateUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
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


@Deprecated
public class ChartGenerator
{
    private Logger logger = LoggerFactory.getLogger(ChartGenerator.class);

    private AssetDAO assetDAO;
    private TradeDAO tradeDAO;

    public ChartGenerator(AssetDAO assetDAO, TradeDAO tradeDAO)
    {
        this.assetDAO = assetDAO;
        this.tradeDAO = tradeDAO;
    }

    //XXX Normally a finer graph is needed, need to profile the performance somewhen
    public List<AssetPrice> simplifyAssetPrices(List<AssetPrice> assetPrices)
    {
        if (assetPrices.size() > 1000) {

            List<AssetPrice> assetPricesNew = new ArrayList<AssetPrice>();

            Integer mod = assetPrices.size() / 1000;
            Integer modRun = 0;

            for (AssetPrice ap : assetPrices) {
                if (modRun % mod == 0) {
                    assetPricesNew.add(ap);
                }

                modRun++;
            }

            assetPrices = assetPricesNew;
        }

        return assetPrices;
    }

    public List<TimePeriodValues> getTrades(List<Trade> trades)
    {
        List<TimePeriodValues> tradeTimePeriodValues = new ArrayList<TimePeriodValues>();

        for (Trade t : trades) {
            TimePeriodValues marketOrderPriceTimePeriodValues = new TimePeriodValues("Trade " + t.getId());

            for (MarketOrder mo : t.getMarketOrders()) {
                //System.out.println("Adding value to trade line " + mo.getExecutionDate() + " price " + mo.getExecutionPrice().getAsk());
                marketOrderPriceTimePeriodValues.add(new SimpleTimePeriod(mo.getExecutionDate(), mo.getExecutionDate()), mo.getExecutionPrice());
            }

            tradeTimePeriodValues.add(marketOrderPriceTimePeriodValues);
        }

        return tradeTimePeriodValues;
    }

    public List<TimePeriodValues> getBollingerBandsForTicksBack(Asset asset, Date startDate, Date endDate, Integer ticksBack, Integer bollingerK)
    {
        List<TimePeriodValues> bollingerBands = new ArrayList<TimePeriodValues>();

        List<AssetPrice> assetPrices = assetDAO.getAssetPricesForAssetStartDateEndDate(asset, startDate, endDate);

        //Get some earlier prices to calc simple moving average
        List<AssetPrice> movingAveragePrices = assetDAO.getAssetPricesForAssetStartDateEndDate(asset, DateUtils.addSeconds(startDate, ticksBack * -1), endDate);

        assetPrices = simplifyAssetPrices(assetPrices);

        TimePeriodValues middleBand = new TimePeriodValues("SMA" + ticksBack + "Ticks "  + asset.getName());

        TimePeriodValues topBand = new TimePeriodValues("TicksBollinger Top K" + bollingerK + " "  + asset.getName());
        TimePeriodValues bottomBand = new TimePeriodValues("TicksBollinger Bottom K" + bollingerK + " "  + asset.getName());

        List<AssetPrice> relevantMovingAveragePrices = null;
        Double movingAverage = null;
        Double standardDeviation = null;

        Integer filterSizer = 0;

        for (AssetPrice ap : assetPrices) {

            if ((movingAveragePrices.indexOf(ap) - ticksBack) < 0) {
                continue;
            }

            relevantMovingAveragePrices = movingAveragePrices.subList(movingAveragePrices.indexOf(ap) - ticksBack, movingAveragePrices.indexOf(ap));

            movingAverage = MovingAverageCalculator.calculateMovingAverage(relevantMovingAveragePrices, true);

            middleBand.add(new SimpleTimePeriod(ap.getPricingTime(), ap.getPricingTime()), movingAverage);

            if (relevantMovingAveragePrices.size() > 1) {
                standardDeviation = BollingerCalculator.calcStandardDeviation(relevantMovingAveragePrices, true);

                topBand.add(new SimpleTimePeriod(ap.getPricingTime(), ap.getPricingTime()), movingAverage + (standardDeviation * bollingerK));
                bottomBand.add(new SimpleTimePeriod(ap.getPricingTime(), ap.getPricingTime()), movingAverage - (standardDeviation * bollingerK));
            }
        }

        bollingerBands.add(middleBand);

        bollingerBands.add(topBand);
        bollingerBands.add(bottomBand);

        return bollingerBands;
    }

    public TimePeriodValues getMovingAverage(Asset asset, Date startDate, Date endDate, Integer movingAverageSeconds)
    {
        TimePeriodValues movingAverageBand = new TimePeriodValues("SMA" + (movingAverageSeconds / 60 / 60 / 24) + "DAYS "  + asset.getName());

        List<AssetPrice> assetPrices = assetDAO.getAssetPricesForAssetStartDateEndDate(asset, startDate, endDate);

        //Get some earlier prices to calc simple moving average
        List<AssetPrice> movingAveragePrices = assetDAO.getAssetPricesForAssetStartDateEndDate(asset, DateUtils.addSeconds(startDate, movingAverageSeconds * -1), endDate);

        assetPrices = simplifyAssetPrices(assetPrices);

        Integer filterSizer = 0;
        List<AssetPrice> relevantMovingAveragePrices = null;
        Double movingAverage = null;

        for (AssetPrice ap : assetPrices) {

            Date referenceDate = DateUtils.addSeconds(ap.getPricingTime(), movingAverageSeconds * -1);

            if (movingAveragePrices.get(0).getPricingTime().after(referenceDate)) {
                continue;
            }

            for (Integer iI = filterSizer; iI < movingAveragePrices.size(); iI++) {
                if (movingAveragePrices.get(iI + 1).getPricingTime().after(referenceDate)) {
                    //We went too far, begin index was the last one
                    filterSizer = iI;
                    break;
                }
            }

            relevantMovingAveragePrices = movingAveragePrices.subList(filterSizer, movingAveragePrices.indexOf(ap));

            movingAverage = MovingAverageCalculator.calculateMovingAverage(relevantMovingAveragePrices, true);

            movingAverageBand.add(new SimpleTimePeriod(ap.getPricingTime(), ap.getPricingTime()), movingAverage);
        }

        return movingAverageBand;
    }

    public TimePeriodValues getExponentialMovingAverage(Asset asset, Date startDate, Date endDate, Integer emaTimeframeSeconds, Integer emaTimeframesBack, Boolean addCurrentPrice)
    {
        TimePeriodValues movingAverageBand = new TimePeriodValues("EMA" + emaTimeframesBack + "TIMEFRAMES (" + emaTimeframeSeconds +") "  + asset.getName());

        List<AssetPrice> assetPrices = assetDAO.getAssetPricesForAssetStartDateEndDate(asset, startDate, endDate);

        //Get some earlier prices to calc exponential moving average
        List<AssetPrice> movingAveragePrices = assetDAO.getAssetPricesForAssetStartDateEndDate(asset, DateUtils.addSeconds(startDate, (emaTimeframesBack * emaTimeframeSeconds * 2) * -1), endDate);

        //logger.info("Calculated date which is needed " + DateUtils.addDays(startDate, emaDaysBack * -1));

        assetPrices = simplifyAssetPrices(assetPrices);

        Integer filterSizer = 0;
        List<AssetPrice> relevantMovingAveragePrices = null;
        Double movingAverage = null;

        for (AssetPrice ap : assetPrices) {

            Date referenceDate = DateUtils.addSeconds(ap.getPricingTime(), (emaTimeframesBack * emaTimeframeSeconds) * -1);

            if (movingAveragePrices.get(0).getPricingTime().after(referenceDate)) {
                logger.info("Could not calc EMA earliest available date is " + movingAveragePrices.get(0).getPricingTime());
                continue;
            }

            
            for (Integer iI = filterSizer; iI < movingAveragePrices.size(); iI++) {
                if (movingAveragePrices.get(iI + 1).getPricingTime().after(referenceDate)) {
                    //We went too far, begin index was the last one
                    filterSizer = iI;
                    break;
                }
            }
            

            relevantMovingAveragePrices = movingAveragePrices.subList(filterSizer, movingAveragePrices.indexOf(ap));

            logger.info("Calculating EMA");
            movingAverage = MovingAverageCalculator.calcExponentialMovingAverage(emaTimeframeSeconds, emaTimeframesBack, relevantMovingAveragePrices, true, addCurrentPrice, null);

            movingAverageBand.add(new SimpleTimePeriod(ap.getPricingTime(), ap.getPricingTime()), movingAverage);
        }

        return movingAverageBand;
    }

    public List<TimePeriodValues> getBollingerBands(Asset asset, Date startDate, Date endDate, Integer movingAverageSeconds, Integer bollingerK)
    {
        List<TimePeriodValues> bollingerBands = new ArrayList<TimePeriodValues>();

        List<AssetPrice> assetPrices = assetDAO.getAssetPricesForAssetStartDateEndDate(asset, startDate, endDate);

        //Get some earlier prices to calc simple moving average
        List<AssetPrice> movingAveragePrices = assetDAO.getAssetPricesForAssetStartDateEndDate(asset, DateUtils.addSeconds(startDate, movingAverageSeconds * -1), endDate);

        assetPrices = simplifyAssetPrices(assetPrices);

        TimePeriodValues middleBand = new TimePeriodValues("SMA" + (movingAverageSeconds / 60 / 60 / 24) + "DAYS "  + asset.getName());

        TimePeriodValues topBand = new TimePeriodValues("TimeBollinger Top K" + bollingerK + " "  + asset.getName());
        TimePeriodValues bottomBand = new TimePeriodValues("TimeBollinger Bottom K" + bollingerK + " "  + asset.getName());

        List<AssetPrice> relevantMovingAveragePrices = null;
        Double movingAverage = null;
        Double standardDeviation = null;

        Integer filterSizer = 0;

        for (AssetPrice ap : assetPrices) {

            Date referenceDate = DateUtils.addSeconds(ap.getPricingTime(), movingAverageSeconds * -1);

            if (movingAveragePrices.get(0).getPricingTime().after(referenceDate)) {
                continue;
            }

            for (Integer iI = filterSizer; iI < movingAveragePrices.size(); iI++) {
                if (movingAveragePrices.get(iI + 1).getPricingTime().after(referenceDate)) {
                    //We went to far, begin index was the last one
                    filterSizer = iI;
                    break;
                }
            }

            relevantMovingAveragePrices = movingAveragePrices.subList(filterSizer, movingAveragePrices.indexOf(ap));

            movingAverage = MovingAverageCalculator.calculateMovingAverage(relevantMovingAveragePrices, true);

            middleBand.add(new SimpleTimePeriod(ap.getPricingTime(), ap.getPricingTime()), movingAverage);

            if (relevantMovingAveragePrices.size() > 1) {
                standardDeviation = BollingerCalculator.calcStandardDeviation(relevantMovingAveragePrices, true);

                topBand.add(new SimpleTimePeriod(ap.getPricingTime(), ap.getPricingTime()), movingAverage + (standardDeviation * bollingerK));
                bottomBand.add(new SimpleTimePeriod(ap.getPricingTime(), ap.getPricingTime()), movingAverage - (standardDeviation * bollingerK));
            }
        }

        bollingerBands.add(middleBand);

        bollingerBands.add(topBand);
        bollingerBands.add(bottomBand);

        return bollingerBands;
    }

    public List<TimePeriodValues> getBollingerTimeframeBands(Asset asset, Date startDate, Date endDate, Integer timeframeSeconds, Integer numberOfTimeframes, Integer bollingerK)
    {
        List<TimePeriodValues> bollingerBands = new ArrayList<TimePeriodValues>();

        List<AssetPrice> assetPrices = assetDAO.getAssetPricesForAssetStartDateEndDate(asset, startDate, endDate);

        //Get some earlier prices to calc simple moving average
        List<AssetPrice> movingAveragePrices =  AssetPriceSparser.sparseAssetPriceWithoutAveraging(asset.getAssetPrices(), timeframeSeconds);

        assetPrices = simplifyAssetPrices(assetPrices);

        TimePeriodValues middleBand = new TimePeriodValues("SMA" + (numberOfTimeframes / 60 / 60 / 24) + " "  + asset.getName());

        TimePeriodValues topBand = new TimePeriodValues("TimeBollinger Top K" + bollingerK + " "  + asset.getName());
        TimePeriodValues bottomBand = new TimePeriodValues("TimeBollinger Bottom K" + bollingerK + " "  + asset.getName());

        List<AssetPrice> relevantMovingAveragePrices = null;
        Double movingAverage = null;
        Double standardDeviation = null;

        Integer filterSizer = 0;

        for (AssetPrice ap : assetPrices) {

            for (Integer iI = filterSizer; (iI + 1) < movingAveragePrices.size(); iI++) {
                if (movingAveragePrices.get(iI + 1).getPricingTime().after(ap.getPricingTime())) {
                    //We went to far, begin index was the last one
                    filterSizer = iI;
                    break;
                }
            }

            if ((filterSizer - numberOfTimeframes) < 0) {
                continue;
            }

            relevantMovingAveragePrices = movingAveragePrices.subList((filterSizer - numberOfTimeframes), filterSizer);

            movingAverage = MovingAverageCalculator.calculateMovingAverage(relevantMovingAveragePrices, true);

            middleBand.add(new SimpleTimePeriod(ap.getPricingTime(), ap.getPricingTime()), movingAverage);

            if (relevantMovingAveragePrices.size() > 1) {
                standardDeviation = BollingerCalculator.calcStandardDeviation(relevantMovingAveragePrices, true);

                topBand.add(new SimpleTimePeriod(ap.getPricingTime(), ap.getPricingTime()), movingAverage + (standardDeviation * bollingerK));
                bottomBand.add(new SimpleTimePeriod(ap.getPricingTime(), ap.getPricingTime()), movingAverage - (standardDeviation * bollingerK));
            }
        }

        bollingerBands.add(middleBand);

        bollingerBands.add(topBand);
        bollingerBands.add(bottomBand);

        return bollingerBands;
    }

    public void setRange(JFreeChart jFreeChart, List<AssetPrice> assetPrices)
    {
        List<AssetPrice> sortedAssetPrices = new ArrayList<AssetPrice>(assetPrices);

        Collections.sort(sortedAssetPrices, new AssetPricePriceComparator());

        XYPlot xyp = jFreeChart.getXYPlot();

        //xyp.addAnnotation(new XYTextAnnotation("Fummel", dataset.getX(0,10).doubleValue(), dataset.getY(0, 10).doubleValue()));

        NumberAxis axis = (NumberAxis) xyp.getRangeAxis();

        Double width = sortedAssetPrices.get(sortedAssetPrices.size() - 1).getAsk() - sortedAssetPrices.get(0).getAsk();

        axis.setRange(sortedAssetPrices.get(0).getAsk() - width / 4,sortedAssetPrices.get(sortedAssetPrices.size() - 1).getAsk() + width / 4);

        DateAxis daxis = new DateAxis("Time Axis");

        daxis.setVerticalTickLabels(true);

        daxis.setTickMarkPosition(DateTickMarkPosition.START);
        daxis.setAutoRange(true);
        daxis.setDateFormatOverride(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"));
        xyp.setDomainAxis(daxis);

    }

    public JFreeChart generateChart(Asset asset, Date startDate, Date endDate)
    {
        List<AssetPrice> assetPrices = assetDAO.getAssetPricesForAssetStartDateEndDate(asset,startDate,endDate);

        assetPrices = simplifyAssetPrices(assetPrices);

        TimePeriodValues assetPriceTimePeriodValues = new TimePeriodValues(asset.getName());
        TimePeriodValuesCollection dataset = new TimePeriodValuesCollection();

        for (AssetPrice ap : assetPrices) {
            assetPriceTimePeriodValues.add(new SimpleTimePeriod(ap.getPricingTime(), ap.getPricingTime()), ap.getAsk());
        }

        dataset.addSeries(assetPriceTimePeriodValues);

        JFreeChart jFreeChart = ChartFactory.createXYLineChart("Performance","Time","Value", dataset, PlotOrientation.VERTICAL,true, false, false);

        setRange(jFreeChart, assetPrices);

        return jFreeChart;
    }

    public BufferedImage renderChart(JFreeChart jFreeChart, Integer width, Integer height)
    {
        jFreeChart.setBackgroundPaint(Color.white);

        BufferedImage bi = jFreeChart.createBufferedImage(width,height);

        return bi;
    }

    public JFreeChart generateChartForSimulationRawData(List<SimulationRawData> simulationRawDatas)
    {
        Integer seriesCount = 0;

        TimePeriodValuesCollection dataset = new TimePeriodValuesCollection();
        TimePeriodValues prices = new TimePeriodValues("Prices");
        List<TimePeriodValues> tpv = new ArrayList<TimePeriodValues>();
        List<TimePeriodValues> trades = new ArrayList<TimePeriodValues>();
        Double maxPrice = null;
        Double minPrice = null;

        for (SimulationRawData srd : simulationRawDatas) {

            prices.add(new SimpleTimePeriod(srd.getPricingTime(), srd.getPricingTime()), srd.getPrice());

            if (maxPrice == null) {
                maxPrice = srd.getPrice();
            } else {
                if (maxPrice < srd.getPrice()) {
                    maxPrice = srd.getPrice();
                }
            }

            if (minPrice == null) {
                minPrice = srd.getPrice();
            } else {
                if (minPrice > srd.getPrice()) {
                    minPrice = srd.getPrice();
                }
            }

            if (srd.getTradePrice() != null) {
                //logger.info("====================== Adding trade " + srd.getTradePrice());
                if (trades.isEmpty() || trades.get(trades.size() - 1).getItemCount() == 2) {
                    trades.add(new TimePeriodValues("Trades " + (trades.size() + 1)));
                }

                if (srd.getTradePrice() < minPrice) {
                    minPrice = srd.getTradePrice();
                }

                if (srd.getTradePrice() > maxPrice) {
                    maxPrice = srd.getTradePrice();
                }

                trades.get(trades.size() - 1).add(new SimpleTimePeriod(srd.getPricingTime(), srd.getPricingTime()), srd.getTradePrice());

                //logger.info("======== Adding trade price " + (trades.size() + 1) + " price: " + srd.getTradePrice());
            }

            Integer count = 0;

            if (srd.getRawData() != null) {
                //logger.info("===============");
                for (String s : srd.getRawData().split(";")) {

                    Double d = Double.parseDouble(s);

                    if (d < minPrice) {
                        minPrice = d;
                    }

                    if (d > maxPrice) {
                        maxPrice = d;
                    }

                    if ((tpv.size() - 1) < count) {
                        tpv.add(new TimePeriodValues("Line " + count));
                    }

                    tpv.get(count).add(new SimpleTimePeriod(srd.getPricingTime(), srd.getPricingTime()), d);

                    //logger.info("Adding " + Double.parseDouble(s));

                    count++;
                }
                //logger.info("===============");
            }
        }

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        dataset.addSeries(prices);
        renderer.setSeriesShapesVisible(seriesCount.intValue(), false);
        seriesCount++;

        for (TimePeriodValues t : tpv) {
            dataset.addSeries(t);
            renderer.setSeriesShapesVisible(seriesCount.intValue(), false);
            seriesCount++;
        }



        for (TimePeriodValues t : trades) {
            dataset.addSeries(t);
            seriesCount++;

            renderer.setSeriesShapesVisible(seriesCount.intValue(), true);
        }

        JFreeChart jFreeChart = ChartFactory.createXYLineChart("Performance", "Time", "Value", dataset, PlotOrientation.VERTICAL, true, false, false);

        jFreeChart.getXYPlot().setRenderer(renderer);

        XYPlot xyp = jFreeChart.getXYPlot();

        NumberAxis axis = (NumberAxis) xyp.getRangeAxis();

        Double width = maxPrice - minPrice;

        axis.setRange(minPrice - width / 4,maxPrice + width / 4);

        DateAxis daxis = new DateAxis("Time Axis");

        daxis.setVerticalTickLabels(true);

        daxis.setTickMarkPosition(DateTickMarkPosition.START);
        daxis.setAutoRange(true);
        daxis.setDateFormatOverride(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"));
        xyp.setDomainAxis(daxis);

        return jFreeChart;
    }
}
*/