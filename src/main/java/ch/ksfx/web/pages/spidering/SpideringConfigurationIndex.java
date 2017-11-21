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

package ch.ksfx.web.pages.spidering;

import ch.ksfx.dao.spidering.SpideringConfigurationDAO;
import ch.ksfx.dao.spidering.SpideringDAO;
import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.model.spidering.SpideringConfiguration;
import ch.ksfx.web.services.scheduler.SchedulerService;
import ch.ksfx.web.services.spidering.SpideringRunner;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.quartz.SchedulerException;
import org.springframework.security.access.annotation.Secured;

import java.util.Date;
import java.util.List;


public class SpideringConfigurationIndex
{
    @Inject
    private SpideringConfigurationDAO spideringConfigurationDAO;
    
    @Inject
    private PageRenderLinkSource pageRenderLinkSource;

    @Inject
    private SpideringDAO spideringDAO;

    @Inject
    private SpideringRunner spideringRunner;

    @Inject
    private SchedulerService schedulerService;

    @Property
    private SpideringConfiguration spideringConfiguration;

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {

    }

    public List<SpideringConfiguration> getAllSpideringConfigurations()
    {
        return spideringConfigurationDAO.getAllSpideringConfigurations();
    }

    public void onActionFromDelete(Long spideringConfigurationId)
    {
        spideringConfigurationDAO.deleteSpideringConfiguration(spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId));
    }

    public void onActionFromRunSpidering(Long spideringConfigurationId)
    {
        Spidering spidering = new Spidering();
        spidering.setSpideringConfiguration(spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId));
        spidering.setStarted(new Date());

        spideringDAO.saveOrUpdate(spidering);

        System.out.println("Started spidering with id: " + spidering.getId());

        spideringRunner.runSpidering(spidering);
    }

    public void onActionFromSchedule(Long spideringConfigurationId)
    {
        SpideringConfiguration spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
        spideringConfiguration.setCronScheduleEnabled(true);

        schedulerService.scheduleSpidering(spideringConfiguration);

        spideringConfigurationDAO.saveOrUpdate(spideringConfiguration);
    }

    public void onActionFromDeleteSchedule(Long spideringConfigurationId) throws SchedulerException
    {
        SpideringConfiguration spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
        spideringConfiguration.setCronScheduleEnabled(false);

        schedulerService.deleteJob("Spidering" + spideringConfiguration.getId().toString(), "Spiderings");

        spideringConfigurationDAO.saveOrUpdate(spideringConfiguration);
    }

    public boolean getScheduled() throws SchedulerException
    {
        return schedulerService.jobExists("Spidering" + spideringConfiguration.getId().toString(),"Spiderings");
    }
    
    public String getIframeInfoLink()
    {
    	return pageRenderLinkSource.createPageRenderLinkWithContext("admin/note/viewnotesforentityplain", getInfoContextParameters()).toURI();	
    }
    
    public Object[] getInfoContextParameters()
    {
        if (spideringConfiguration != null) {
            return new Object[]{null,
                    null,
                    null,spideringConfiguration.getId(),null};
        }

        return null;
    }
}
