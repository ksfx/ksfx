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

import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.spidering.*;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.spidering.*;
import ch.ksfx.util.GenericSelectModel;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.PropertyAccess;
import org.quartz.CronExpression;
import org.springframework.security.access.annotation.Secured;


public class ManageSpideringConfiguration
{
    @Inject
    private SpideringConfigurationDAO spideringConfigurationDAO;

    @Inject
    private ResourceConfigurationDAO resourceConfigurationDAO;

    @Inject
    private SpideringPostActivityDAO spideringPostActivityDAO;

    @Inject
    private ResourceLoaderPluginConfigurationDAO resourceLoaderPluginConfigurationDAO;

    @Inject
    private UrlFragmentDAO urlFragmentDAO;

    @Inject
    private PagingUrlFragmentDAO pagingUrlFragmentDAO;

    @Inject
    private ResultUnitConfigurationDAO resultUnitConfigurationDAO;

    @Inject
    private ResultUnitModifierConfigurationDAO resultUnitModifierConfigurationDAO;

    @Inject
    private ResultUnitConfigurationModifiersDAO resultUnitConfigurationModifiersDAO;

    @Inject
    private ResultVerifierConfigurationDAO resultVerifierConfigurationDAO;

    @Inject
    private ActivityDAO activityDAO;

    @Inject
    private PropertyAccess propertyAccess;

    @InjectComponent
    private Form spideringConfigurationForm;

    @Property
    private Integer resourceConfigurationIndex;

    @Property @Persist
    private GenericSelectModel<UrlFragmentFinder> allUrlFragmentFinders;

    @Property @Persist
    private GenericSelectModel<ResourceLoaderPluginConfiguration> allResourceLoaderPluginConfigurations;

    @Property @Persist
    private GenericSelectModel<ResultUnitType> allResultUnitTypes;

    @Property @Persist
    private GenericSelectModel<ResultUnitFinder> allResultUnitFinders;

    @Property @Persist
    private GenericSelectModel<ResultVerifierConfiguration> allResultVerifierConfigurations;

    @Property @Persist
    private GenericSelectModel<ResultUnitModifierConfiguration> allResultUnitModifierConfigurations;

    @Property @Persist
    private GenericSelectModel<Activity> allActivities;

    @Property
    private Integer urlFragmentIndex;

    @Property
    private UrlFragment urlFragment;

    @Property
    private SpideringPostActivity spideringPostActivity;

    @Property
    private PagingUrlFragment pagingUrlFragment;

    @Property
    private ResultUnitModifierConfiguration resultUnitModifierConfiguration;

    @Property
    private ResultUnitConfigurationModifiers resultUnitConfigurationModifiers;

    @Property
    private Integer resultUnitConfigurationIndex;

    @Property
    private ResultUnitConfiguration resultUnitConfiguration;

    @Property
    private SpideringConfiguration spideringConfiguration;

    private ResourceConfiguration resourceConfiguration;

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long spideringConfigurationId)
    {
        spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
    }

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {
        if (spideringConfiguration == null) {
            spideringConfiguration = new SpideringConfiguration();
        }

        allUrlFragmentFinders = new GenericSelectModel<UrlFragmentFinder>(urlFragmentDAO.getAllUrlFragmentFinders(),UrlFragmentFinder.class,"name","id",propertyAccess);
        allResourceLoaderPluginConfigurations = new GenericSelectModel<ResourceLoaderPluginConfiguration>(resourceLoaderPluginConfigurationDAO.getAllResourceLoaderPluginConfigurations(),ResourceLoaderPluginConfiguration.class,"name","id",propertyAccess);
        allResultUnitTypes = new GenericSelectModel<ResultUnitType>(resultUnitConfigurationDAO.getAllResultUnitTypes(),ResultUnitType.class,"name","id",propertyAccess);
        allResultUnitFinders = new GenericSelectModel<ResultUnitFinder>(resultUnitConfigurationDAO.getAllResultUnitFinders(),ResultUnitFinder.class,"name","id",propertyAccess);
        allResultUnitModifierConfigurations = new GenericSelectModel<ResultUnitModifierConfiguration>(resultUnitModifierConfigurationDAO.getAllResultUnitModifierConfigurations(),ResultUnitModifierConfiguration.class,"name","id",propertyAccess);
        allResultVerifierConfigurations = new GenericSelectModel<ResultVerifierConfiguration>(resultVerifierConfigurationDAO.getAllResultVerifierConfigurations(),ResultVerifierConfiguration.class,"name","id",propertyAccess);
        allActivities = new GenericSelectModel<Activity>(activityDAO.getAllActivities(),Activity.class,"name","id",propertyAccess);
    }

    public void onSuccessFromSpideringConfigurationForm()
    {
        spideringConfigurationDAO.saveOrUpdate(spideringConfiguration);
    }

    public void onActionFromAddResourceConfiguration()
    {
        ResourceConfiguration resourceConfiguration = new ResourceConfiguration();
        resourceConfiguration.setSpideringConfiguration(spideringConfiguration);
        resourceConfigurationDAO.saveOrUpdate(resourceConfiguration);
    }

    public void onActionFromRemoveResourceConfiguration(Long resourceConfigurationId)
    {
        resourceConfigurationDAO.deleteResourceConfiguration(resourceConfigurationDAO.getResourceConfigurationForId(resourceConfigurationId));
    }

    public void onActionFromAddSpideringPostActivity()
    {
        SpideringPostActivity spideringPostActivity = new SpideringPostActivity();
        spideringPostActivity.setSpideringConfiguration(spideringConfiguration);

        spideringPostActivityDAO.saveOrUpdate(spideringPostActivity);
    }

    public void onActionFromRemoveSpideringPostActivity(Long spideringPostActivityId)
    {
        spideringPostActivityDAO.deleteSpideringPostActivity(spideringPostActivityDAO.getSpideringPostActivityForId(spideringPostActivityId));
    }

    public Long onPassivate()
    {
        if (spideringConfiguration != null) {
            return spideringConfiguration.getId();
        }

        return null;
    }

    public ResourceConfiguration getResourceConfiguration()
    {
        return spideringConfiguration.getResourceConfigurations().get(resourceConfigurationIndex);
    }
    public void setResourceConfiguration(ResourceConfiguration resourceConfiguration)
    {
        this.resourceConfiguration = resourceConfiguration;
    }

    public void onActionFromAddUrlFragment(Long resourceConfigurationId)
    {
        ResourceConfiguration resourceConfiguration = resourceConfigurationDAO.getResourceConfigurationForId(resourceConfigurationId);

        UrlFragment urlFragment = new UrlFragment();

        urlFragment.setResourceConfiguration(resourceConfiguration);
        urlFragmentDAO.saveOrUpdate(urlFragment);
    }

    public void onActionFromRemoveUrlFragment(Long urlFragmentId)
    {
        urlFragmentDAO.delete(urlFragmentDAO.getUrlFragmentForId(urlFragmentId));
    }

    public void onActionFromAddResultUnitModifierConfiguration(Long resultUnitConfigurationId)
    {
        ResultUnitConfiguration resultUnitConfiguration = resultUnitConfigurationDAO.getResultUnitConfigurationForId(resultUnitConfigurationId);

        ResultUnitConfigurationModifiers resultUnitConfigurationModifiers = new ResultUnitConfigurationModifiers();
        resultUnitConfigurationModifiers.setResultUnitConfiguration(resultUnitConfiguration);

        resultUnitConfigurationModifiersDAO.saveOrUpdate(resultUnitConfigurationModifiers);
    }

    public void onActionFromRemoveResultUnitModifierConfiguration(Long resultUnitConfigurationModifiersId)
    {
        resultUnitConfigurationModifiersDAO.delete(resultUnitConfigurationModifiersDAO.getResultUnitConfigurationModifiersForId(resultUnitConfigurationModifiersId));
    }

    public void onActionFromAddResultUnitConfiguration(Long resourceConfigurationId)
    {
        ResourceConfiguration resourceConfiguration = resourceConfigurationDAO.getResourceConfigurationForId(resourceConfigurationId);

        ResultUnitConfiguration resultUnitConfiguration = new ResultUnitConfiguration();
        resultUnitConfiguration.setResourceConfiguration(resourceConfiguration);

        resultUnitConfigurationDAO.saveOrUpdate(resultUnitConfiguration);
    }

    public void onActionFromRemoveResultUnitConfiguration(Long resultUnitConfigurationId)
    {
        resultUnitConfigurationDAO.delete(resultUnitConfigurationDAO.getResultUnitConfigurationForId(resultUnitConfigurationId));
    }

    public void onActionFromAddPaging(Long resourceConfigurationId)
    {
        ResourceConfiguration resourceConfiguration = resourceConfigurationDAO.getResourceConfigurationForId(resourceConfigurationId);

        resourceConfiguration.setPaging(true);

        resourceConfigurationDAO.saveOrUpdate(resourceConfiguration);
    }

    public void onActionFromRemovePaging(Long resourceConfigurationId)
    {
        ResourceConfiguration resourceConfiguration = resourceConfigurationDAO.getResourceConfigurationForId(resourceConfigurationId);

        for (PagingUrlFragment pagingUrlFragment : resourceConfiguration.getPagingUrlFragments()) {
            pagingUrlFragmentDAO.delete(pagingUrlFragment);
        }

        resourceConfiguration.setPaging(false);

        resourceConfigurationDAO.saveOrUpdate(resourceConfiguration);
    }

    public void onActionFromAddPagingUrlFragment(Long resourceConfigurationId)
    {
        ResourceConfiguration resourceConfiguration = resourceConfigurationDAO.getResourceConfigurationForId(resourceConfigurationId);

        PagingUrlFragment pagingUrlFragment = new PagingUrlFragment();

        pagingUrlFragment.setResourceConfiguration(resourceConfiguration);
        pagingUrlFragmentDAO.saveOrUpdate(pagingUrlFragment);
    }

    public void onActionFromRemovePagingUrlFragment(Long pagingUrlFragmentId)
    {
        pagingUrlFragmentDAO.delete(pagingUrlFragmentDAO.getPagingUrlFragmentForId(pagingUrlFragmentId));
    }

    public void onSuccess()
    {
        if (spideringConfiguration.getResourceConfigurations() != null) {

            Integer depth = 0;
            for (ResourceConfiguration rc : spideringConfiguration.getResourceConfigurations()) {
                rc.setDepth(depth);
                resourceConfigurationDAO.saveOrUpdate(rc);
                for (UrlFragment uf : rc.getUrlFragments()) {
                    urlFragmentDAO.saveOrUpdate(uf);
                }

                for (PagingUrlFragment uf : rc.getPagingUrlFragments()) {
                    pagingUrlFragmentDAO.saveOrUpdate(uf);
                }

                for (ResultUnitConfiguration ruc : rc.getResultUnitConfigurations()) {
                    resultUnitConfigurationDAO.saveOrUpdate(ruc);

                    for (ResultUnitConfigurationModifiers rucm : ruc.getResultUnitConfigurationModifiers()) {
                        resultUnitConfigurationModifiersDAO.saveOrUpdate(rucm);
                    }
                }

                depth++;
            }
        }

        spideringConfigurationDAO.saveOrUpdate(spideringConfiguration);

        if (spideringConfiguration.getSpideringPostActivities() != null) {
            for (SpideringPostActivity spideringPostActivity : spideringConfiguration.getSpideringPostActivities()) {
                spideringPostActivityDAO.saveOrUpdate(spideringPostActivity);
            }
        }
    }

    public void onValidateFromSpideringConfigurationForm() throws Exception
    {
        if (spideringConfiguration.getCronSchedule() != null && !spideringConfiguration.getCronSchedule().isEmpty()) {
            try {
                CronExpression cronExpression = new CronExpression(spideringConfiguration.getCronSchedule());
            } catch (Exception e) {
                spideringConfigurationForm.recordError("Cron Schedule not valid");
            }
        }

        Integer siteIdentifierCount = 0;

        if (spideringConfiguration.getResourceConfigurations() != null) {
            for (ResourceConfiguration resourceConfiguration : spideringConfiguration.getResourceConfigurations()) {
                if (resourceConfiguration.getResultUnitConfigurations() != null) {
                    for (ResultUnitConfiguration resultUnitConfiguration : resourceConfiguration.getResultUnitConfigurations()) {
                        if (resultUnitConfiguration.getSiteIdentifier()) {
                            siteIdentifierCount++;
                        }
                    }
                }
            }
        }

        if (siteIdentifierCount > 1) {
            spideringConfigurationForm.recordError("Only one site identifier can be defined");
        }
    }

    public Integer getConfigurationDepth()
    {
        return resourceConfigurationIndex * 25;
    }
}
