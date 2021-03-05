package ch.ksfx.controller.informationretrieval;

import ch.ksfx.dao.spidering.ResourceLoaderPluginConfigurationDAO;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.spidering.ResourceLoaderPluginConfiguration;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.util.StacktraceUtil;
import groovy.lang.GroovyClassLoader;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.lang.reflect.Constructor;

@Controller
@RequestMapping("/informationretrieval")
public class ResourceLoaderPluginConfigurationController
{
    private ResourceLoaderPluginConfigurationDAO resourceLoaderPluginConfigurationDAO;
    private ServiceProvider serviceProvider;

    public ResourceLoaderPluginConfigurationController(ResourceLoaderPluginConfigurationDAO resourceLoaderPluginConfigurationDAO, ServiceProvider serviceProvider)
    {
        this.resourceLoaderPluginConfigurationDAO = resourceLoaderPluginConfigurationDAO;
        this.serviceProvider = serviceProvider;
    }

    @GetMapping("/resourceloaderpluginconfiguration")
    public String resourceLoaderPluginConfigurationIndex(Model model, Pageable pageable)
    {
        model.addAttribute("resourceLoaderPluginConfigurationsPage", resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationsForPageable(pageable));

        return "informationretrieval/resource_loader_plugin_configuration";
    }

    @GetMapping({"/resourceloaderpluginconfigurationedit", "/resourceloaderpluginconfigurationedit/{id}"})
    public String resourceLoaderPluginConfigurationEdit(@PathVariable(value = "id", required = false) Long resourceLoaderPluginConfigurationId, Model model)
    {
        ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration = new ResourceLoaderPluginConfiguration();

        if (resourceLoaderPluginConfigurationId != null) {
            resourceLoaderPluginConfiguration = resourceLoaderPluginConfigurationDAO.getResourceLoaderPluginConfigurationForId(resourceLoaderPluginConfigurationId);
        }

        model.addAttribute("resourceLoaderPluginConfiguration", resourceLoaderPluginConfiguration);

        return "informationretrieval/resource_loader_plugin_configuration_edit";
    }

    @PostMapping({"/resourceloaderpluginconfigurationedit", "/resourceloaderpluginconfigurationedit/{id}"})
    public String resourceLoaderPluginConfigurationSubmit(@PathVariable(value = "id", required = false) Long activityId, @Valid @ModelAttribute ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration, BindingResult bindingResult, Model model)
    {
        validateResourceLoaderPluginConfiguration(resourceLoaderPluginConfiguration, bindingResult);

        if (bindingResult.hasErrors()) {
            return "informationretrieval/resource_loader_plugin_configuration_edit";
        }

        resourceLoaderPluginConfigurationDAO.saveOrUpdate(resourceLoaderPluginConfiguration);

        return "redirect:/informationretrieval/resourceloaderpluginconfigurationedit/" + resourceLoaderPluginConfiguration.getId();
    }

    public void validateResourceLoaderPluginConfiguration(ResourceLoaderPluginConfiguration resourceLoaderPluginConfiguration, BindingResult bindingResult)
    {
        try {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(resourceLoaderPluginConfiguration.getGroovyCode());

            Constructor cons = clazz.getDeclaredConstructor(ServiceProvider.class);
            cons.newInstance(serviceProvider);
        } catch (Exception e) {
            bindingResult.rejectValue("groovyCode", "resourceLoaderPluginConfiguration.groovyCode", e.getMessage() + StacktraceUtil.getStackTrace(e));
        }
    }
}
