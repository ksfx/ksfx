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

package ch.ksfx.util.calc;

import ch.ksfx.model.Observation;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class MovingAverageCalculator
{
    private static Logger logger = LoggerFactory.getLogger(MovingAverageCalculator.class);
/*
    public static Double calculateMovingAverage(List<AssetPrice> assetPrices, boolean askPrice)
    {
        Double assetPricesSum = 0.0;

        if (askPrice) {
            for (AssetPrice ap : assetPrices) {
                assetPricesSum += ap.getAsk();
            }
        } else {
            for (AssetPrice ap : assetPrices) {
                assetPricesSum += ap.getBid();
            }
        }

        return assetPricesSum / assetPrices.size();
    }
  */  
    //TODO implement askprice
    public static Double calculateMovingAverageObservation(List<Observation> assetPrices, boolean askPrice)
    {
        Double assetPricesSum = 0.0;

        if (askPrice) {
            for (Observation ap : assetPrices) {
                assetPricesSum += Double.parseDouble(ap.getScalarValue());
            }
        } else {
            for (Observation ap : assetPrices) {
                assetPricesSum += Double.parseDouble(ap.getScalarValue());
            }
        }

        return assetPricesSum / assetPrices.size();
    }
/*
    public static Double calculateMovingAverageForSecondsBack(List<AssetPrice> prices, Integer secondsBackFromNow, boolean askPrice)
    {
        List<AssetPrice> aps = new ArrayList<AssetPrice>(prices);

        Collections.sort(aps, new AssetPriceDateComparator());
        Collections.reverse(aps);

        Date referenceDate = DateUtils.addSeconds(aps.get(0).getPricingTime(),(secondsBackFromNow * -1));

        if (prices.get(0).getPricingTime().after(referenceDate)) {
            return null;
        }

        List<AssetPrice> relevantPrices = new ArrayList<AssetPrice>();

        for (Integer iI = 0; iI < aps.size(); iI++) {
            if (aps.get(iI).getPricingTime().after(referenceDate)) {
                relevantPrices.add(aps.get(iI));
            } else {
                break;
            }
        }

        if (askPrice) {
            Double cal = 0.0;

            for (AssetPrice ap : relevantPrices) {
                cal = cal + ap.getAsk();
            }

            return cal / relevantPrices.size();
        } else {
            Double cal = 0.0;

            for (AssetPrice ap : relevantPrices) {
                cal = cal + ap.getBid();
            }

            return cal / relevantPrices.size();
        }
    }
  */  
    //TODO implement ask price
    public static Double calculateMovingAverageForSecondsBackObservation(List<Observation> prices, Integer secondsBackFromNow, boolean askPrice)
    {
        List<Observation> aps = new ArrayList<Observation>(prices);

        Collections.sort(aps, new ObservationDateComparator());
        Collections.reverse(aps);

        Date referenceDate = DateUtils.addSeconds(aps.get(0).getObservationTime(),(secondsBackFromNow * -1));

        if (prices.get(0).getObservationTime().after(referenceDate)) {
            return null;
        }

        List<Observation> relevantPrices = new ArrayList<Observation>();

        for (Integer iI = 0; iI < aps.size(); iI++) {
            if (aps.get(iI).getObservationTime().after(referenceDate)) {
                relevantPrices.add(aps.get(iI));
            } else {
                break;
            }
        }

        if (askPrice) {
            Double cal = 0.0;

            for (Observation ap : relevantPrices) {
                cal = cal + Double.parseDouble(ap.getScalarValue());
            }

            return cal / relevantPrices.size();
        } else {
            Double cal = 0.0;

            for (Observation ap : relevantPrices) {
                cal = cal + Double.parseDouble(ap.getScalarValue());
            }

            return cal / relevantPrices.size();
        }
    }
/*
    public static Double calculateMovingAverage(List<AssetPrice> prices, Integer ticksBackFromNow, boolean askPrice)
    {
        if (prices.size() < ticksBackFromNow) {
            return null;
        }

        List<AssetPrice> aps = new ArrayList<AssetPrice>(prices);

        Collections.sort(aps, new AssetPriceDateComparator());
        Collections.reverse(aps);

        //for (AssetPrice test : aps) {
        //    logger.info("SORTTEST = " + test.getPricingTime());
        //}

        if (askPrice) {
            Double cal = 0.0;

            for (Integer i = 0; i < ticksBackFromNow && prices.size() >= ticksBackFromNow; i++) {
                cal = cal + aps.get(i).getAsk();
            }

            return cal / ticksBackFromNow;
        } else {
            Double cal = 0.0;

            for (Integer i = 0; i < ticksBackFromNow && prices.size() >= ticksBackFromNow; i++) {

                //logger.info("MA based on " + aps.get(i).getBid() + " TIME: " + aps.get(i).getPricingTime());

                cal = cal + aps.get(i).getBid();
            }

            //logger.info("====================");

            return cal / ticksBackFromNow;
        }
    }
*/    
    //TODO implement ask price
    public static Double calculateMovingAverageObservation(List<Observation> prices, Integer ticksBackFromNow, boolean askPrice)
    {
        if (prices.size() < ticksBackFromNow) {
            return null;
        }

        List<Observation> aps = new ArrayList<Observation>(prices);

        Collections.sort(aps, new ObservationDateComparator());
        Collections.reverse(aps);

        //for (AssetPrice test : aps) {
        //    logger.info("SORTTEST = " + test.getPricingTime());
        //}

        if (askPrice) {
            Double cal = 0.0;

            for (Integer i = 0; i < ticksBackFromNow && prices.size() >= ticksBackFromNow; i++) {
                cal = cal + Double.parseDouble(aps.get(i).getScalarValue());
            }

            return cal / ticksBackFromNow;
        } else {
            Double cal = 0.0;

            for (Integer i = 0; i < ticksBackFromNow && prices.size() >= ticksBackFromNow; i++) {

                //logger.info("MA based on " + aps.get(i).getBid() + " TIME: " + aps.get(i).getPricingTime());

                cal = cal + Double.parseDouble(aps.get(i).getScalarValue());
            }

            //logger.info("====================");

            return cal / ticksBackFromNow;
        }
    }

/*
    //http://www.iexplain.org/exponential-moving-average-defined/
    public static Double calcExponentialMovingAverage(Integer timeFrameSeconds, Integer timeFramesBack, List<AssetPrice> assetPrices, boolean askPrice, boolean acceptCurrentPrice, AssetPrice forcedPriced)
    {
        AssetPrice currentPrice = assetPrices.get(assetPrices.size() - 1);

        List<AssetPrice> aps = AssetPriceSparser.sparseAssetPriceWithoutAveraging(assetPrices, timeFrameSeconds);

        Collections.sort(aps, new AssetPriceDateComparator());
        Collections.reverse(aps);

        if (forcedPriced != null) {
            if (!forcedPriced.equals(aps.get(0))) {
                return null;
            }
        }

        if (aps.size() < timeFramesBack) {
            logger.info("Cannot calc EMA not enough values, after sparsing we only have: " + aps.size() + " values");

            for (AssetPrice ip : aps) {
                logger.info("Asset Price Pricing Date " + ip.getPricingTime());
            }

            return null;
        }

        List<AssetPrice> relevantPrices = new ArrayList<AssetPrice>();

        if (acceptCurrentPrice) {
            relevantPrices.add(currentPrice);
        } else {
            relevantPrices.add(aps.get(0));
        }

        Integer lastAddedIndex = 0;

        //Shouldn't it be enough to sublist here?
        for (Integer iI = 0; iI < aps.size(); iI++) {
            if (relevantPrices.get(lastAddedIndex).getPricingTime().getTime() - aps.get(iI).getPricingTime().getTime() >= timeFrameSeconds) {
                relevantPrices.add(aps.get(iI));

                if (lastAddedIndex > timeFramesBack - 1) {
                    break;
                }
            }
        }

        //starting point
        Double average = calculateMovingAverage(relevantPrices, true);

        Double k = 2. / (timeFramesBack + 1.);

        Double ema = 0.0;
        Double emaYesterday = average;

        for (Integer iK = 2; iK <= relevantPrices.size(); iK++) {
            ema = relevantPrices.get(relevantPrices.size() - iK).getAsk() * k + emaYesterday * (1. - k);
            emaYesterday = ema;
        }

        logger.info("Calculated EMA " + ema + " - k was " + k);

        return ema;
    }
*/    
    //TODO implement ask price
    //http://www.iexplain.org/exponential-moving-average-defined/
    public static Double calcExponentialMovingAverageObservation(Integer timeFrameSeconds, Integer timeFramesBack, List<Observation> assetPrices, boolean askPrice, boolean acceptCurrentPrice, Observation forcedPriced)
    {
    	//TODO this might be 0
        Observation currentPrice = assetPrices.get(assetPrices.size() - 1);

        List<Observation> aps = AssetPriceSparser.sparseAssetPriceWithoutAveragingObservation(assetPrices, timeFrameSeconds);

        Collections.sort(aps, new ObservationDateComparator());
        Collections.reverse(aps);

        if (forcedPriced != null) {
            if (!forcedPriced.equals(aps.get(0))) {
                return null;
            }
        }

        if (aps.size() < timeFramesBack) {
            logger.info("Cannot calc EMA not enough values, after sparsing we only have: " + aps.size() + " values");

            for (Observation ip : aps) {
                logger.info("Asset Price Pricing Date " + ip.getObservationTime());
            }

            return null;
        }

        List<Observation> relevantPrices = new ArrayList<Observation>();

        if (acceptCurrentPrice) {
            relevantPrices.add(currentPrice);
        } else {
            relevantPrices.add(aps.get(0));
        }

        Integer lastAddedIndex = 0;

        //Shouldn't it be enough to sublist here?
        for (Integer iI = 0; iI < aps.size(); iI++) {
            if (relevantPrices.get(lastAddedIndex).getObservationTime().getTime() - aps.get(iI).getObservationTime().getTime() >= timeFrameSeconds) {
                relevantPrices.add(aps.get(iI));

                if (lastAddedIndex > timeFramesBack - 1) {
                    break;
                }
            }
        }

        //starting point
        Double average = calculateMovingAverageObservation(relevantPrices, true);

        Double k = 2. / (timeFramesBack + 1.);

        Double ema = 0.0;
        Double emaYesterday = average;

        for (Integer iK = 2; iK <= relevantPrices.size(); iK++) {
            ema = Double.parseDouble(relevantPrices.get(relevantPrices.size() - iK).getScalarValue()) * k + emaYesterday * (1. - k);
            emaYesterday = ema;
        }

        logger.info("Calculated EMA " + ema + " - k was " + k);

        return ema;
    }
/*
    public static Double calcExponentialMovingAverage(List<AssetPrice> assetPrices, boolean askPrice)
    {
        Collections.sort(assetPrices, new AssetPriceDateComparator());
        Collections.reverse(assetPrices);

        //this is just a test
        //System.out.println("-- Test Start");
        //for (AssetPrice ap : assetPrices) {
        //    System.out.println(ap.getPricingTime());
        //}
        //System.out.println("-- Test End");
        //Test end

        Double average = calculateMovingAverage(assetPrices, true);

        Double k = 2. / (assetPrices.size() + 1.);

        Double ema = 0.0;
        Double emaYesterday = average;

        for (Integer iK = 2; iK <= assetPrices.size(); iK++) {
            ema = assetPrices.get(assetPrices.size() - iK).getAsk() * k + emaYesterday * (1. - k);
            emaYesterday = ema;
        }

        return ema;
    }
*/  
    //TODO implement ask price
    public static Double calcExponentialMovingAverageObservation(List<Observation> assetPrices, boolean askPrice)
    {
        Collections.sort(assetPrices, new ObservationDateComparator());
        Collections.reverse(assetPrices);

        //this is just a test
        //System.out.println("-- Test Start");
        //for (AssetPrice ap : assetPrices) {
        //    System.out.println(ap.getPricingTime());
        //}
        //System.out.println("-- Test End");
        //Test end

        Double average = calculateMovingAverageObservation(assetPrices, true);

        Double k = 2. / (assetPrices.size() + 1.);

        Double ema = 0.0;
        Double emaYesterday = average;

        for (Integer iK = 2; iK <= assetPrices.size(); iK++) {
            ema = Double.parseDouble(assetPrices.get(assetPrices.size() - iK).getScalarValue()) * k + emaYesterday * (1. - k);
            emaYesterday = ema;
        }

        return ema;
    }
}
