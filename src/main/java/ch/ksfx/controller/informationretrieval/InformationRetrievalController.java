package ch.ksfx.controller.informationretrieval;

import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.spidering.*;
import ch.ksfx.model.spidering.*;
import ch.ksfx.services.scheduler.SchedulerService;
import ch.ksfx.services.spidering.SpideringRunner;
import org.quartz.SchedulerException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    public String informationRetrievalSubmit(@PathVariable(value = "id", required = false) Long spideringConfigurationId, @ModelAttribute SpideringConfiguration spideringConfiguration, Model model)
    {
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

        saveSpideringConfiguation(spideringConfiguration);

        System.out.println("Spidering Post Activity Id" + spideringConfiguration.getSpideringPostActivities().get(0).getId());
        System.out.println("Spidering Post Activity Spidering Configuration id" + spideringConfiguration.getSpideringPostActivities().get(0).getSpideringConfiguration().getId());

        model.addAttribute("allUrlFragmentFinders", urlFragmentDAO.getAllUrlFragmentFinders());
        model.addAttribute("allResourceLoeaderPluginConfigurations", resourceLoaderPluginConfigurationDAO.getAllResourceLoaderPluginConfigurations());
        model.addAttribute("allResultUnitFinders", resultUnitConfigurationDAO.getAllResultUnitFinders());
        model.addAttribute("allResultUnitTypes", resultUnitConfigurationDAO.getAllResultUnitTypes());
        model.addAttribute("allResultUnitModifierConfigurations", resultUnitModifierConfigurationDAO.getAllResultUnitModifierConfigurations());
        model.addAttribute("allResultVerifierConfigurations", resultVerifierConfigurationDAO.getAllResultVerifierConfigurations());
        model.addAttribute("allActivities", activityDAO.getAllActivities());
        model.addAttribute("spideringConfiguration", spideringConfiguration);

        //form validation, if fails -> return to form instead redirect...
        //return "informationretrieval/spidering_configuration_edit";

        return "redirect:/informationretrieval/spideringconfigurationedit/" + spideringConfiguration.getId();
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
}
