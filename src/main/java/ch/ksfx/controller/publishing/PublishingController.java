package ch.ksfx.controller.publishing;

import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.publishing.PublishingResourceDAO;
import ch.ksfx.dao.publishing.PublishingSharedDataDAO;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.model.publishing.PublishingResource;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.util.StacktraceUtil;
import groovy.lang.GroovyClassLoader;
import org.quartz.CronExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/publishing")
public class PublishingController
{
    private PublishingConfigurationDAO publishingConfigurationDAO;
    private PublishingResourceDAO publishingResourceDAO;
    private PublishingSharedDataDAO publishingSharedDataDAO;
    private ServiceProvider serviceProvider;

    public PublishingController(PublishingConfigurationDAO publishingConfigurationDAO, PublishingResourceDAO publishingResourceDAO, PublishingSharedDataDAO publishingSharedDataDAO, ServiceProvider serviceProvider)
    {
        this.publishingConfigurationDAO = publishingConfigurationDAO;
        this.publishingResourceDAO = publishingResourceDAO;
        this.publishingSharedDataDAO = publishingSharedDataDAO;
        this.serviceProvider = serviceProvider;
    }

    @GetMapping("/")
    public String publishingIndex(Pageable pageable, Model model)
    {
        Page<PublishingConfiguration> publishingConfigurationsPage = publishingConfigurationDAO.getPublishingConfigutationsForPageableAndPublishingCategory(pageable, null);

        model.addAttribute("publishingConfigurationsPage", publishingConfigurationsPage);

        return "publishing/publishing";
    }

    @GetMapping({"/publishingconfigurationedit", "/publishingconfigurationedit/{id}"})
    public String publishingConfigurationEdit(@PathVariable(value = "id", required = false) Long publishingConfigurationId, Model model, Pageable pageable)
    {
        PublishingConfiguration publishingConfiguration = new PublishingConfiguration();

        if (publishingConfigurationId != null) {
            publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
        } else {
            InputStream inputStream = null;
            String demoPublishingStrategy = null;

            try {
                inputStream = getClass().getClassLoader().getResourceAsStream("groovy/DemoPublishingStrategy.groovy");

                demoPublishingStrategy = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            publishingConfiguration.setPublishingStrategy(demoPublishingStrategy);
        }

        model.addAttribute("allPublishingCategories", publishingConfigurationDAO.getAllPublishingCategories());

        if (publishingConfiguration.getId() != null) {
            model.addAttribute("publishingResources", publishingResourceDAO.getAllPublishingResourcesForPublishingConfiguration(publishingConfiguration));
            model.addAttribute("publishingSharedDataPage", publishingSharedDataDAO.getPublishingSharedDataForPageableAndPublishingConfiguration(pageable,publishingConfiguration));
        }

        model.addAttribute("publishingConfiguration", publishingConfiguration);

        return "publishing/publishing_configuration_edit";
    }

    @PostMapping({"/publishingconfigurationedit", "/publishingconfigurationedit/{id}"})
    public String publishingConfigurationSubmit(@PathVariable(value = "id", required = false) Long publishingConfigurationId, @Valid @ModelAttribute PublishingConfiguration publishingConfiguration, BindingResult bindingResult, Model model, Pageable pageable)
    {
        validatePublishingConfiguration(publishingConfiguration, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("allPublishingCategories", publishingConfigurationDAO.getAllPublishingCategories());

            if (publishingConfiguration.getId() != null) {
                model.addAttribute("publishingResources", publishingResourceDAO.getAllPublishingResourcesForPublishingConfiguration(publishingConfiguration));
                model.addAttribute("publishingSharedDataPage", publishingSharedDataDAO.getPublishingSharedDataForPageableAndPublishingConfiguration(pageable,publishingConfiguration));
            }

            return "publishing/publishing_configuration_edit";
        }

        if (publishingConfiguration.getPublishingCategory().getId() == 0) {
            publishingConfiguration.setPublishingCategory(null);
        }

        publishingConfigurationDAO.saveOrUpdatePublishingConfiguration(publishingConfiguration);

        return "redirect:/publishing/publishingconfigurationedit/" + publishingConfiguration.getId();
    }

    public void validatePublishingConfiguration(PublishingConfiguration publishingConfiguration, BindingResult bindingResult)
    {
        PublishingConfiguration publishingConfigurationOld = null;

        if (publishingConfiguration.getId() != null) {
            publishingConfigurationOld = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfiguration.getId());
        }

        if (publishingConfigurationOld != null && publishingConfigurationOld.getLockedForEditing() == true && publishingConfiguration.getLockedForEditing() == true) {
            bindingResult.rejectValue("lockedForEditing","publishingConfiguration.lockedForEditing","This publishing configuration is locked, please unlock it first!");
        }

        if (publishingConfiguration.getCronSchedule() != null && !publishingConfiguration.getCronSchedule().isEmpty()) {
            try {
                CronExpression cronExpression = new CronExpression(publishingConfiguration.getCronSchedule());
            } catch (Exception e) {
                bindingResult.rejectValue("cronSchedule", "publishingConfiguration.cronSchedule", "Cron Schedule not valid");
            }
        }

        try {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(publishingConfiguration.getPublishingStrategy());

            Constructor cons = clazz.getDeclaredConstructor(ServiceProvider.class);
            //cons.newInstance(serviceProvider);
        } catch (Exception e) {
            bindingResult.rejectValue("publishingStrategy", "publishingConfiguration.publishingStrategy", e.getMessage() + StacktraceUtil.getStackTrace(e));
        }
    }

    @GetMapping("/publishingconfigurationdelete/{id}")
    public String publishingConfigurationDelete(@PathVariable(value = "id", required = false) Long publishingConfigurationId)
    {
        PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
        publishingConfigurationDAO.deletePublishingConfiguration(publishingConfiguration);

        return "redirect:/publishing/";
    }

    @GetMapping({"/publishingresourceedit/{publishingConfigurationId}", "/publishingresourceedit/{publishingConfigurationId}/{publishingResourceId}"})
    public String publishingResourceEdit(@PathVariable(value = "publishingConfigurationId", required = true) Long publishingConfigurationId, @PathVariable(value = "publishingResourceId", required = false) Long publishingResourceId, Model model, Pageable pageable)
    {
        PublishingResource publishingResource = new PublishingResource();

        if (publishingResourceId != null) {
            publishingResource = publishingResourceDAO.getPublishingResourceForId(publishingResourceId);
        } else {
            InputStream inputStream = null;
            String demoPublishingResource = null;

            try {
                inputStream = getClass().getClassLoader().getResourceAsStream("groovy/DemoPublishingResource.groovy");

                demoPublishingResource = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            publishingResource.setPublishingStrategy(demoPublishingResource);
        }


        PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);

        model.addAttribute("publishingConfiguration", publishingConfiguration);
        model.addAttribute("publishingResource", publishingResource);

        return "publishing/publishing_resource_edit";
    }

    @PostMapping({"/publishingresourceedit/{publishingConfigurationId}", "/publishingresourceedit/{publishingConfigurationId}/{publishingResourceId}"})
    public String publishingResourceSubmit(@PathVariable(value = "publishingConfigurationId", required = true) Long publishingConfigurationId, @PathVariable(value = "publishingResourceId", required = false) Long publishingResourceId, @Valid @ModelAttribute PublishingResource publishingResource, BindingResult bindingResult, Model model)
    {
        System.out.println("ID: " + publishingConfigurationId);

        if (publishingResource.getPublishingConfiguration() == null || publishingResource.getPublishingConfiguration().getId() == null) {
            publishingResource.setPublishingConfiguration(publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId));
        }

        validatePublishingResource(publishingResource, bindingResult);

        if (bindingResult.hasErrors()) {
            PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
            model.addAttribute("publishingConfiguration", publishingConfiguration);

            return "publishing/publishing_resource_edit";
        }

        publishingResourceDAO.saveOrUpdatePublishingResource(publishingResource);

        return "redirect:/publishing/publishingresourceedit/" + publishingResource.getPublishingConfiguration().getId() + "/" + publishingResource.getId();
    }

    public void validatePublishingResource(PublishingResource publishingResource, BindingResult bindingResult)
    {
        PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingResource.getPublishingConfiguration().getId());

        if (publishingConfiguration.getLockedForEditing() == true) {
            bindingResult.rejectValue("title","publishingResource.title","This publishing configuration is locked, please unlock it first!");
        }

        try {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(publishingResource.getPublishingStrategy());

            Constructor cons = clazz.getDeclaredConstructor(ServiceProvider.class);
            //cons.newInstance(serviceProvider);
        } catch (Exception e) {
            bindingResult.rejectValue("publishingStrategy", "publishingResource.publishingStrategy", e.getMessage() + StacktraceUtil.getStackTrace(e));
        }
    }

    @GetMapping({"/publishingresourcedelete/{publishingResourceId}"})
    public String publishingResourceDelete(@PathVariable(value = "publishingResourceId", required = false) Long publishingResourceId)
    {
        PublishingResource publishingResource = publishingResourceDAO.getPublishingResourceForId(publishingResourceId);

        publishingResourceDAO.deletePublishingResource(publishingResource);

        return "redirect:/publishing/publishingconfigurationedit/" + publishingResource.getPublishingConfiguration().getId();
    }
}
