package ch.ksfx.controller.publishing;

import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.publishing.PublishingResourceDAO;
import ch.ksfx.model.PublishingConfiguration;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.spidering.SpideringConfiguration;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.util.StacktraceUtil;
import groovy.lang.GroovyClassLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.lang.reflect.Constructor;

@Controller
@RequestMapping("/publishing")
public class PublishingController
{
    private PublishingConfigurationDAO publishingConfigurationDAO;
    private PublishingResourceDAO publishingResourceDAO;

    public PublishingController(PublishingConfigurationDAO publishingConfigurationDAO, PublishingResourceDAO publishingResourceDAO)
    {
        this.publishingConfigurationDAO = publishingConfigurationDAO;
        this.publishingResourceDAO = publishingResourceDAO;
    }

    @GetMapping("/")
    public String publishingIndex(Pageable pageable, Model model)
    {
        Page<PublishingConfiguration> publishingConfigurationsPage = publishingConfigurationDAO.getPublishingConfigutationsForPageableAndPublishingCategory(pageable, null);

        model.addAttribute("publishingConfigurationsPage", publishingConfigurationsPage);

        return "publishing/publishing";
    }

    @GetMapping({"/publishingconfigurationedit", "/publishingconfigurationedit/{id}"})
    public String publishingConfigurationEdit(@PathVariable(value = "id", required = false) Long publishingConfigurationId, Model model)
    {
        PublishingConfiguration publishingConfiguration = new PublishingConfiguration();

        if (publishingConfiguration != null) {
            publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
        }

        model.addAttribute("allPublishingCategories", publishingConfigurationDAO.getAllPublishingCategories());

        if (publishingConfiguration.getId() != null) {
            model.addAttribute("publishingResources", publishingResourceDAO.getAllPublishingResourcesForPublishingConfiguration(publishingConfiguration));
        }

        model.addAttribute("publishingConfiguration", publishingConfiguration);

        return "publishing/publishing_configuration_edit";
    }

    @PostMapping({"/publishingconfigurationedit", "/publishingconfigurationedit/{id}"})
    public String publishingConfigurationSubmit(@PathVariable(value = "id", required = false) Long publishingConfigurationId, @Valid @ModelAttribute PublishingConfiguration publishingConfiguration, BindingResult bindingResult, Model model)
    {
        validatePublishingConfiguration(publishingConfiguration, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("allPublishingCategories", publishingConfigurationDAO.getAllPublishingCategories());

            return "publishing/publishing_configuration_edit";
        }

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

        try {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(publishingConfiguration.getPublishingStrategy());

            Constructor cons = clazz.getDeclaredConstructor(ServiceProvider.class);
        } catch (Exception e) {
            bindingResult.rejectValue("publishingStrategy", "publishingConfiguration.publishingStrategy", e.getMessage() + StacktraceUtil.getStackTrace(e));
        }
    }
}
