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

package ch.ksfx.web.pages.admin.timeseries;

import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.model.TimeSeriesType;
import ch.ksfx.util.GenericSelectModel;
import ch.ksfx.web.services.lucene.IndexService;
import ch.ksfx.web.services.seriesbrowser.SeriesBrowser;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.PropertyAccess;
import org.springframework.security.access.annotation.Secured;




@Secured({"ROLE_ADMIN"})
public class ManageTimeSeries
{
    @Property
    private TimeSeries timeSeries;

    @Property
    private GenericSelectModel<TimeSeriesType> allTimeSeriesTypes;

    @Inject
    private TimeSeriesDAO timeSeriesDAO;

    @Inject
    private PropertyAccess propertyAccess;

    @Inject
    private SeriesBrowser seriesBrowser;
    
    @Inject
    private IndexService indexService;

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long timeSeriesId)
    {
        timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);
    }

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {
        allTimeSeriesTypes = new GenericSelectModel<TimeSeriesType>(timeSeriesDAO.getAllTimeSeriesTypes(),TimeSeriesType.class,"name","id",propertyAccess);

        if (timeSeries == null) {
            timeSeries = new TimeSeries();
        }
    }

    public Long onPassivate()
    {
        if (timeSeries != null) {
            return timeSeries.getId();
        }

        return null;
    }

    public Object onSuccessFromTimeSeriesForm()
    {
    	if (timeSeries.getId() != null) {
			TimeSeries oldSeries = timeSeriesDAO.getTimeSeriesForId(timeSeries.getId());
			seriesBrowser.removeSeries(oldSeries);
    	}
		
        timeSeriesDAO.saveOrUpdate(timeSeries);
		seriesBrowser.addSeries(timeSeries);
        
        indexService.refreshIndexableTimeSeriesIds();
		
		//seriesBrowser.rebuildNavigationTree();

        return TimeSeriesIndex.class;
    }
}