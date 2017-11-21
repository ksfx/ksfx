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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AssetPriceSparser
{
    /*
    public static List<AssetPrice> sparseAssetPriceWithoutAveraging(List<AssetPrice> assetPrices, Integer sparsingSeconds)
    {
        Collections.sort(assetPrices, new AssetPriceDateComparator());

        Integer currentIndex = 0;
        List<AssetPrice> sparsedPrices = new ArrayList<AssetPrice>();

        for (AssetPrice ap : assetPrices) {
            if (sparsedPrices.isEmpty()) {
                sparsedPrices.add(ap);
            }

            if (DateUtils.addSeconds(sparsedPrices.get(currentIndex).getPricingTime(), sparsingSeconds).before(ap.getPricingTime())) {
                sparsedPrices.add(ap);
                currentIndex++;
            }
        }

        return sparsedPrices;
    }
    */
    
    public static List<Observation> sparseAssetPriceWithoutAveragingObservation(List<Observation> assetPrices, Integer sparsingSeconds)
    {
        Collections.sort(assetPrices, new ObservationDateComparator());

        Integer currentIndex = 0;
        List<Observation> sparsedPrices = new ArrayList<Observation>();

        for (Observation ap : assetPrices) {
            if (sparsedPrices.isEmpty()) {
                sparsedPrices.add(ap);
            }

            if (DateUtils.addSeconds(sparsedPrices.get(currentIndex).getObservationTime(), sparsingSeconds).before(ap.getObservationTime())) {
                sparsedPrices.add(ap);
                currentIndex++;
            }
        }

        return sparsedPrices;
    }

    //TODO This is either unstable OR broken!!!
    /*
    @Deprecated
    public static List<AssetPrice> sparseAssetPriceWithoutAveragingFast(List<AssetPrice> assetPrices, Integer sparsingSeconds, Integer guessedStepForward)
    {
        //Collections.sort(assetPrices, new AssetPriceDateComparator());

        Integer currentIndex = 0;
        List<AssetPrice> sparsedPrices = new ArrayList<AssetPrice>();

        for (Integer i = 0; i < assetPrices.size(); i += guessedStepForward) {
            if (sparsedPrices.isEmpty()) {
                sparsedPrices.add(assetPrices.get(i));
            }

            if (i < assetPrices.size()) {
                if (DateUtils.addSeconds(sparsedPrices.get(currentIndex).getPricingTime(), sparsingSeconds).before(assetPrices.get(i).getPricingTime())) {
                    Integer relevantIndex = i;
                    for (Integer iI = i; iI < guessedStepForward; iI--) {
                        if (!DateUtils.addSeconds(sparsedPrices.get(currentIndex).getPricingTime(), sparsingSeconds).before(assetPrices.get(iI).getPricingTime())) {
                            break;
                        }

                        relevantIndex = iI;
                    }

                    sparsedPrices.add(assetPrices.get(relevantIndex));
                    currentIndex++;
                }
            } else {
                for (Integer iI = i; iI < guessedStepForward; iI--) {
                    if (iI < assetPrices.size() && DateUtils.addSeconds(sparsedPrices.get(currentIndex).getPricingTime(), sparsingSeconds).before(assetPrices.get(iI).getPricingTime())) {
                        sparsedPrices.add(assetPrices.get(iI));
                    }
                }
            }
        }

        return sparsedPrices;
    }

    //TODO Implement
    public static List<AssetPrice> sparseAssetPriceWithAveraging(List<AssetPrice> assetPrices, Integer sparsingSeconds)
    {
        throw new RuntimeException("To be implemented");
    }
    */
}
 