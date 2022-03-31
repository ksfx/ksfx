package ch.ksfx.controller.informationretrieval;

import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.spidering.*;
import ch.ksfx.model.spidering.*;
import ch.ksfx.services.scheduler.SchedulerService;
import ch.ksfx.services.spidering.SpideringRunner;
import ch.ksfx.util.StacktraceUtil;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.util.Date;

//https://www.baeldung.com/spring-boot-crud-thymeleaf
@Controller
@RequestMapping("/informationretrieval")
public class InformationRetrievalController
{
    private final SpideringConfigurationDAO spideringConfigurationDAO;
    private final SpideringDAO spideringDAO;
    private final SpideringRunner spideringRunner;
    private final SchedulerService schedulerService;
    private final UrlFragmentDAO urlFragmentDAO;
    private final ResourceLoaderPluginConfigurationDAO resourceLoaderPluginConfigurationDAO;
    private final ResultUnitConfigurationDAO resultUnitConfigurationDAO;
    private final ResultUnitModifierConfigurationDAO resultUnitModifierConfigurationDAO;
    private final ResultVerifierConfigurationDAO resultVerifierConfigurationDAO;
    private final ActivityDAO activityDAO;
    private final ResourceConfigurationDAO resourceConfigurationDAO;
    private final PagingUrlFragmentDAO pagingUrlFragmentDAO;
    private final ResultUnitConfigurationModifiersDAO resultUnitConfigurationModifiersDAO;
    private final SpideringPostActivityDAO spideringPostActivityDAO;

    public InformationRetrievalController(SpideringConfigurationDAO spideringConfigurationDAO,
                                          SpideringDAO spideringDAO,
                                          SpideringRunner spideringRunner,
                                          SchedulerService schedulerService,
                                          UrlFragmentDAO urlFragmentDAO,
                                          ResourceLoaderPluginConfigurationDAO resourceLoaderPluginConfigurationDAO,
                                          ResultUnitConfigurationDAO resultUnitConfigurationDAO,
                                          ResultUnitModifierConfigurationDAO resultUnitModifierConfigurationDAO,
                                          ResultVerifierConfigurationDAO resultVerifierConfigurationDAO,
                                          ActivityDAO activityDAO,
                                          ResourceConfigurationDAO resourceConfigurationDAO,
                                          PagingUrlFragmentDAO pagingUrlFragmentDAO,
                                          ResultUnitConfigurationModifiersDAO resultUnitConfigurationModifiersDAO,
                                          SpideringPostActivityDAO spideringPostActivityDAO)
    {
        this.spideringConfigurationDAO = spideringConfigurationDAO;
        this.spideringDAO = spideringDAO;
        this.spideringRunner = spideringRunner;
        this.schedulerService = schedulerService;
        this.urlFragmentDAO = urlFragmentDAO;
        this.resourceLoaderPluginConfigurationDAO = resourceLoaderPluginConfigurationDAO;
        this.resultUnitConfigurationDAO = resultUnitConfigurationDAO;
        this.resultUnitModifierConfigurationDAO = resultUnitModifierConfigurationDAO;
        this.resultVerifierConfigurationDAO = resultVerifierConfigurationDAO;
        this.activityDAO = activityDAO;
        this.resourceConfigurationDAO = resourceConfigurationDAO;
        this.pagingUrlFragmentDAO = pagingUrlFragmentDAO;
        this.resultUnitConfigurationModifiersDAO = resultUnitConfigurationModifiersDAO;
        this.spideringPostActivityDAO = spideringPostActivityDAO;
    }

    @GetMapping("/")
    public String informationRetrievalIndex(Pageable pageable, Model model)
    {
        Page<SpideringConfiguration> spideringConfigurationsPage = spideringConfigurationDAO.getSpideringConfigutationsForPageable(pageable);

        model.addAttribute("spideringConfigurationsPage", spideringConfigurationsPage);

        return "informationretrieval/information_retrieval";
    }

    @GetMapping({"/spideringconfigurationedit", "/spideringconfigurationedit/{id}"})
    public String spideringConfigurationEdit(@PathVariable(value = "id", required = false) Long spideringConfigurationId, Model model)
    {
        SpideringConfiguration spideringConfiguration = new SpideringConfiguration();

        if (spideringConfigurationId != null) {
            spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
        }

        model.addAttribute("allUrlFragmentFinders", urlFragmentDAO.getAllUrlFragmentFinders());
        model.addAttribute("allResourceLoeaderPluginConfigurations", resourceLoaderPluginConfigurationDAO.getAllResourceLoaderPluginConfigurations());
        model.addAttribute("allResultUnitFinders", resultUnitConfigurationDAO.getAllResultUnitFinders());
        model.addAttribute("allResultUnitTypes", resultUnitConfigurationDAO.getAllResultUnitTypes());
        model.addAttribute("allResultUnitModifierConfigurations", resultUnitModifierConfigurationDAO.getAllResultUnitModifierConfigurations());
        model.addAttribute("allResultVerifierConfigurations", resultVerifierConfigurationDAO.getAllResultVerifierConfigurations());
        model.addAttribute("allActivities", activityDAO.getAllActivities());
        model.addAttribute("spideringConfiguration", spideringConfiguration);

        return "informationretrieval/spidering_configuration_edit";
    }

    @PostMapping({"/spideringconfigurationedit", "/spideringconfigurationedit/{id}"})
    public String informationRetrievalSubmit(@PathVariable(value = "id", required = false) Long spideringConfigurationId, @ModelAttribute SpideringConfiguration spideringConfiguration, BindingResult bindingResult, Model model)
    {
        if (spideringConfiguration.getResourceConfigurations() != null) {
            for (ResourceConfiguration rc : spideringConfiguration.getResourceConfigurations()) {
                if (rc.getResourceLoaderPluginConfiguration().getId() == 0) {
                    rc.setResourceLoaderPluginConfiguration(null);
                }

                if (rc.getPagingResourceLoaderPluginConfiguration() != null) {
                    if (rc.getPagingResourceLoaderPluginConfiguration().getId() == 0) {
                        rc.setPagingResourceLoaderPluginConfiguration(null);
                    }
                }
            }
        }

        if (spideringConfiguration.getResultVerifierConfiguration() != null && spideringConfiguration.getResultVerifierConfiguration().getId() == 0) {
            spideringConfiguration.setResultVerifierConfiguration(null);
        }

        validateSpideringConfiguration(spideringConfiguration, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("allUrlFragmentFinders", urlFragmentDAO.getAllUrlFragmentFinders());
            model.addAttribute("allResourceLoeaderPluginConfigurations", resourceLoaderPluginConfigurationDAO.getAllResourceLoaderPluginConfigurations());
            model.addAttribute("allResultUnitFinders", resultUnitConfigurationDAO.getAllResultUnitFinders());
            model.addAttribute("allResultUnitTypes", resultUnitConfigurationDAO.getAllResultUnitTypes());
            model.addAttribute("allResultUnitModifierConfigurations", resultUnitModifierConfigurationDAO.getAllResultUnitModifierConfigurations());
            model.addAttribute("allResultVerifierConfigurations", resultVerifierConfigurationDAO.getAllResultVerifierConfigurations());
            model.addAttribute("allActivities", activityDAO.getAllActivities());
            model.addAttribute("spideringConfiguration", spideringConfiguration);

            //form validation, if fails -> return to form instead redirect...
            return "informationretrieval/spidering_configuration_edit";
        }

        saveSpideringConfiguation(spideringConfiguration);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + spideringConfiguration.getId();
    }

    public void validateSpideringConfiguration(SpideringConfiguration spideringConfiguration, BindingResult bindingResult)
    {
        if (spideringConfiguration.getCronSchedule() != null && !spideringConfiguration.getCronSchedule().isEmpty()) {
            try {
                CronExpression cronExpression = new CronExpression(spideringConfiguration.getCronSchedule());
            } catch (Exception e) {
                bindingResult.rejectValue("cronSchedule", "spideringConfiguration.cronSchedule", "Cron Schedule not valid");
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
            bindingResult.rejectValue("name", "spideringConfiguration.name", "Only one site identifier can be defined");
        }
    }

    public void saveSpideringConfiguation(SpideringConfiguration spideringConfiguration)
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

    @GetMapping({"/spideringconfigurationrun/{id}"})
    public String spideringConfigurationRun(@PathVariable(value = "id", required = true) Long spideringConfigurationId)
    {
        Spidering spidering = new Spidering();
        spidering.setSpideringConfiguration(spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId));
        spidering.setStarted(new Date());

        spideringDAO.saveOrUpdate(spidering);

        System.out.println("Started spidering with id: " + spidering.getId());

        spideringRunner.runSpidering(spidering);

        return "redirect:/informationretrieval/";
    }

    @GetMapping({"/spideringconfigurationschedule/{id}"})
    public String spideringConfigurationSchedule(@PathVariable(value = "id", required = true) Long spideringConfigurationId)
    {
        SpideringConfiguration spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
        spideringConfiguration.setCronScheduleEnabled(true);

        schedulerService.scheduleSpidering(spideringConfiguration);

        spideringConfigurationDAO.saveOrUpdate(spideringConfiguration);

        return "redirect:/informationretrieval/";
    }

    @GetMapping({"/spideringconfigurationdeleteschedule/{id}"})
    public String spideringConfigurationDeleteSchedule(@PathVariable(value = "id", required = true) Long spideringConfigurationId) throws SchedulerException
    {
        SpideringConfiguration spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
        spideringConfiguration.setCronScheduleEnabled(false);

        schedulerService.deleteJob("Spidering" + spideringConfiguration.getId().toString(), "Spiderings");

        spideringConfigurationDAO.saveOrUpdate(spideringConfiguration);

        return "redirect:/informationretrieval/";
    }

    @GetMapping({"/spideringconfigurationaddresourceconfiguration/{id}"})
    public String spideringConfigurationAddResourceConfiguration(@PathVariable(value = "id", required = true) Long spideringConfigurationId)
    {
        SpideringConfiguration spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);

        ResourceConfiguration resourceConfiguration = new ResourceConfiguration();
        resourceConfiguration.setSpideringConfiguration(spideringConfiguration);
        resourceConfigurationDAO.saveOrUpdate(resourceConfiguration);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + spideringConfiguration.getId();
    }

    @GetMapping({"/spideringconfigurationaddspideringpostactivity/{id}"})
    public String spideringConfigurationAddSpideringPostActivity(@PathVariable(value = "id", required = true) Long spideringConfigurationId)
    {
        SpideringConfiguration spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);

        SpideringPostActivity spideringPostActivity = new SpideringPostActivity();
        spideringPostActivity.setSpideringConfiguration(spideringConfiguration);

        spideringPostActivityDAO.saveOrUpdate(spideringPostActivity);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + spideringConfiguration.getId();
    }

    @GetMapping({"/spideringconfigurationremovespideringpostactivity/{id}"})
    public String onActionFromRemoveSpideringPostActivity(@PathVariable(value = "id", required = true) Long spideringPostActivityId)
    {
        SpideringPostActivity spideringPostActivity = spideringPostActivityDAO.getSpideringPostActivityForId(spideringPostActivityId);

        spideringPostActivityDAO.deleteSpideringPostActivity(spideringPostActivity);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + spideringPostActivity.getSpideringConfiguration().getId();
    }

    @GetMapping({"/spideringconfigurationremoveresourceconfiguration/{id}"})
    public String spideringConfigurationRemoveResourceConfiguration(@PathVariable(value = "id", required = true) Long resourceConfigurationId)
    {
        ResourceConfiguration resourceConfiguration = resourceConfigurationDAO.getResourceConfigurationForId(resourceConfigurationId);

        resourceConfigurationDAO.deleteResourceConfiguration(resourceConfiguration);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + resourceConfiguration.getSpideringConfiguration().getId();
    }

    @GetMapping({"/spideringconfigurationaddpaging/{id}"})
    public String spideringConfigurationAddPaging(@PathVariable(value = "id", required = true) Long resourceConfigurationId)
    {
        ResourceConfiguration resourceConfiguration = resourceConfigurationDAO.getResourceConfigurationForId(resourceConfigurationId);

        resourceConfiguration.setPaging(true);

        resourceConfigurationDAO.saveOrUpdate(resourceConfiguration);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + resourceConfiguration.getSpideringConfiguration().getId();
    }

    @GetMapping({"/spideringconfigurationremovepaging/{id}"})
    public String spideringConfigurationRemovePaging(@PathVariable(value = "id", required = true) Long resourceConfigurationId)
    {
        ResourceConfiguration resourceConfiguration = resourceConfigurationDAO.getResourceConfigurationForId(resourceConfigurationId);

        for (PagingUrlFragment pagingUrlFragment : resourceConfiguration.getPagingUrlFragments()) {
            pagingUrlFragmentDAO.delete(pagingUrlFragment);
        }

        resourceConfiguration.setPaging(false);

        resourceConfigurationDAO.saveOrUpdate(resourceConfiguration);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + resourceConfiguration.getSpideringConfiguration().getId();
    }

    @GetMapping({"/spideringconfigurationaddpagingurlfragment/{id}"})
    public String spideringConfigurationAddPagingUrlFragment(@PathVariable(value = "id", required = true) Long resourceConfigurationId)
    {
        ResourceConfiguration resourceConfiguration = resourceConfigurationDAO.getResourceConfigurationForId(resourceConfigurationId);

        PagingUrlFragment pagingUrlFragment = new PagingUrlFragment();

        pagingUrlFragment.setResourceConfiguration(resourceConfiguration);
        pagingUrlFragmentDAO.saveOrUpdate(pagingUrlFragment);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + resourceConfiguration.getSpideringConfiguration().getId();
    }

    @GetMapping({"/spideringconfigurationremovepagingurlfragment/{id}"})
    public String spideringConfigurationRemovePagingUrlFragment(@PathVariable(value = "id", required = true) Long pagingUrlFragmentId)
    {
        PagingUrlFragment  pagingUrlFragment = pagingUrlFragmentDAO.getPagingUrlFragmentForId(pagingUrlFragmentId);

        pagingUrlFragmentDAO.delete(pagingUrlFragment);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + pagingUrlFragment.getResourceConfiguration().getSpideringConfiguration().getId();
    }

    @GetMapping({"/spideringconfigurationaddurlfragment/{id}"})
    public String onActionFromAddUrlFragment(@PathVariable(value = "id", required = true) Long resourceConfigurationId)
    {
        ResourceConfiguration resourceConfiguration = resourceConfigurationDAO.getResourceConfigurationForId(resourceConfigurationId);

        UrlFragment urlFragment = new UrlFragment();

        urlFragment.setResourceConfiguration(resourceConfiguration);
        urlFragmentDAO.saveOrUpdate(urlFragment);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + resourceConfiguration.getSpideringConfiguration().getId();
    }

    @GetMapping({"/spideringconfigurationremoveurlfragment/{id}"})
    public String onActionFromRemoveUrlFragment(@PathVariable(value = "id", required = true) Long urlFragmentId)
    {
        UrlFragment urlFragment = urlFragmentDAO.getUrlFragmentForId(urlFragmentId);

        urlFragmentDAO.delete(urlFragment);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + urlFragment.getResourceConfiguration().getSpideringConfiguration().getId();
    }

    @GetMapping({"/spideringconfigurationaddresultunitconfiguration/{id}"})
    public String spideringConfigurationAddResultUnitConfiguration(@PathVariable(value = "id", required = true) Long resourceConfigurationId)
    {
        ResourceConfiguration resourceConfiguration = resourceConfigurationDAO.getResourceConfigurationForId(resourceConfigurationId);

        ResultUnitConfiguration resultUnitConfiguration = new ResultUnitConfiguration();
        resultUnitConfiguration.setResourceConfiguration(resourceConfiguration);

        resultUnitConfigurationDAO.saveOrUpdate(resultUnitConfiguration);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + resourceConfiguration.getSpideringConfiguration().getId();
    }

    @GetMapping({"/spideringconfigurationremoveresultunitconfiguration/{id}"})
    public String spideringConfigurationRemoveResultUnitConfiguration(@PathVariable(value = "id", required = true) Long resultUnitConfigurationId)
    {
        ResultUnitConfiguration resultUnitConfiguration = resultUnitConfigurationDAO.getResultUnitConfigurationForId(resultUnitConfigurationId);

        resultUnitConfigurationDAO.delete(resultUnitConfiguration);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + resultUnitConfiguration.getResourceConfiguration().getSpideringConfiguration().getId();
    }

    @GetMapping({"/spideringconfigurationaddresultunitmodifier/{id}"})
    public String onActionFromAddResultUnitModifierConfiguration(@PathVariable(value = "id", required = true) Long resultUnitConfigurationId)
    {
        ResultUnitConfiguration resultUnitConfiguration = resultUnitConfigurationDAO.getResultUnitConfigurationForId(resultUnitConfigurationId);

        ResultUnitConfigurationModifiers resultUnitConfigurationModifiers = new ResultUnitConfigurationModifiers();
        resultUnitConfigurationModifiers.setResultUnitConfiguration(resultUnitConfiguration);

        resultUnitConfigurationModifiersDAO.saveOrUpdate(resultUnitConfigurationModifiers);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + resultUnitConfiguration.getResourceConfiguration().getSpideringConfiguration().getId();
    }

    @GetMapping({"/spideringconfigurationremoveresultunitmodifier/{id}"})
    public String onActionFromRemoveResultUnitModifierConfiguration(@PathVariable(value = "id", required = true) Long resultUnitConfigurationModifiersId)
    {
        ResultUnitConfigurationModifiers resultUnitConfigurationModifiers = resultUnitConfigurationModifiersDAO.getResultUnitConfigurationModifiersForId(resultUnitConfigurationModifiersId);

        resultUnitConfigurationModifiersDAO.delete(resultUnitConfigurationModifiers);

        return "redirect:/informationretrieval/spideringconfigurationedit/" + resultUnitConfigurationModifiers.getResultUnitConfiguration().getResourceConfiguration().getSpideringConfiguration().getId();
    }

    @GetMapping({"/spideringconfigurationdelete/{id}"})
    public String onActionFromDelete(@PathVariable(value = "id", required = true) Long spideringConfigurationId)
    {
        spideringConfigurationDAO.deleteSpideringConfiguration(spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId));

        return "redirect:/informationretrieval/";
    }
}
