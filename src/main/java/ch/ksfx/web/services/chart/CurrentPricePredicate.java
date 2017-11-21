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

import ch.ksfx.model.AssetPrice;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.time.DateUtils;

import java.util.Date;


public class CurrentPricePredicate implements Predicate
{
    private AssetPrice referencePrice;
    private Date referenceStartDate;

    public CurrentPricePredicate(AssetPrice referencePrice, Integer filterTimeSeconds)
    {
        this.referencePrice = referencePrice;
        this.referenceStartDate = referencePrice.getPricingTime();
        this.referenceStartDate = DateUtils.addSeconds(referenceStartDate, filterTimeSeconds * -1);
    }

    @Override
    public boolean evaluate(Object o)
    {
        AssetPrice currentPrice = (AssetPrice) o;

        if (!currentPrice.getPricingTime().after(referenceStartDate) || !currentPrice.getPricingTime().before(referencePrice.getPricingTime())) {
            return false;
        }

        return true;
    }
}
*/
