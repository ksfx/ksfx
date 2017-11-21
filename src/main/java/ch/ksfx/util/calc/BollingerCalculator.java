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

//import ch.ksfx.model.AssetPrice;

import ch.ksfx.model.Observation;

import java.util.List;


public class BollingerCalculator
{
	/*
    public static Double calcMeanValue(List<AssetPrice> relevantPrices, boolean askPrice)
    {
        Double meanValue = 0.0;

        if (askPrice) {
            for (AssetPrice ap : relevantPrices) {
                meanValue += ap.getAsk();
            }
        } else {
            for (AssetPrice ap : relevantPrices) {
                meanValue += ap.getBid();
            }
        }

        meanValue = meanValue * (1.0 / relevantPrices.size());

        return meanValue;
    }
    */

	/*
    public static Double calcStandardDeviation(List<AssetPrice> relevantMovingAveragePrices, boolean askPrice)
    {
        Double standardDeviation = 0.0;
        Double meanValue = calcMeanValue(relevantMovingAveragePrices, askPrice);

        if (askPrice) {
            for (AssetPrice ap : relevantMovingAveragePrices) {
                standardDeviation += (ap.getAsk() - meanValue) * (ap.getAsk() - meanValue);
            }
        } else {
            for (AssetPrice ap : relevantMovingAveragePrices) {
                standardDeviation += (ap.getBid() - meanValue) * (ap.getBid() - meanValue);
            }
        }

        standardDeviation = Math.sqrt(standardDeviation * (1.0 / (relevantMovingAveragePrices.size() - 1.0)));

        return standardDeviation;
    }
    */
    
    //TODO implement ask price
    public static Double calcMeanValueObservation(List<Observation> relevantPrices, boolean askPrice)
    {
        Double meanValue = 0.0;

        if (askPrice) {
            for (Observation ap : relevantPrices) {
                meanValue += Double.parseDouble(ap.getScalarValue());
            }
        } else {
            for (Observation ap : relevantPrices) {
                meanValue += Double.parseDouble(ap.getScalarValue());
            }
        }

        meanValue = meanValue * (1.0 / relevantPrices.size());

        return meanValue;
    }
    
    //TODO implement ask price
    public static Double calcStandardDeviationObservation(List<Observation> relevantMovingAveragePrices, boolean askPrice)
    {
        Double standardDeviation = 0.0;
        Double meanValue = calcMeanValueObservation(relevantMovingAveragePrices, askPrice);

        if (askPrice) {
            for (Observation ap : relevantMovingAveragePrices) {
                standardDeviation += (Double.parseDouble(ap.getScalarValue()) - meanValue) * (Double.parseDouble(ap.getScalarValue()) - meanValue);
            }
        } else {
            for (Observation ap : relevantMovingAveragePrices) {
                standardDeviation += (Double.parseDouble(ap.getScalarValue()) - meanValue) * (Double.parseDouble(ap.getScalarValue()) - meanValue);
            }
        }

        standardDeviation = Math.sqrt(standardDeviation * (1.0 / (relevantMovingAveragePrices.size() - 1.0)));

        return standardDeviation;
    }
}
