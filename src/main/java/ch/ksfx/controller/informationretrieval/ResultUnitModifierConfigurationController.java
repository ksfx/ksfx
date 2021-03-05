package ch.ksfx.controller.informationretrieval;

import ch.ksfx.dao.spidering.ResultUnitModifierConfigurationDAO;
import ch.ksfx.model.spidering.ResultUnitModifierConfiguration;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.services.systemlogger.SystemLogger;
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
public class ResultUnitModifierConfigurationController
{
    private ResultUnitModifierConfigurationDAO resultUnitModifierConfigurationDAO;
    private SystemLogger systemLogger;

    public ResultUnitModifierConfigurationController(ResultUnitModifierConfigurationDAO resultUnitModifierConfigurationDAO, SystemLogger systemLogger)
    {
        this.resultUnitModifierConfigurationDAO = resultUnitModifierConfigurationDAO;
        this.systemLogger = systemLogger;
    }

    @GetMapping("/resultunitmodifierconfiguration")
    public String resourceLoaderPluginConfigurationIndex(Model model, Pageable pageable)
    {
        model.addAttribute("resultUnitModifierConfigurationsPage", resultUnitModifierConfigurationDAO.getResultUnitModifierConfigurationsForPageable(pageable));

        return "informationretrieval/result_unit_modifier_configuration";
    }

    @GetMapping({"/resultunitmodifierconfigurationedit", "/resultunitmodifierconfigurationedit/{id}"})
    public String resultUnitModifierConfigurationEdit(@PathVariable(value = "id", required = false) Long resultUnitModifierConfigurationId, Model model)
    {
        ResultUnitModifierConfiguration resultUnitModifierConfiguration = new ResultUnitModifierConfiguration();

        if (resultUnitModifierConfigurationId != null) {
            resultUnitModifierConfiguration = resultUnitModifierConfigurationDAO.getResultUnitModifierConfigurationForId(resultUnitModifierConfigurationId);
        }

        model.addAttribute("resultUnitModifierConfiguration", resultUnitModifierConfiguration);

        return "informationretrieval/result_unit_modifier_configuration_edit";
    }

    @PostMapping({"/resultunitmodifierconfigurationedit", "/resultunitmodifierconfigurationedit/{id}"})
    public String resultUnitModifierConfigurationSubmit(@PathVariable(value = "id", required = false) Long resultUnitModifierConfigurationId, @Valid @ModelAttribute ResultUnitModifierConfiguration resultUnitModifierConfiguration, BindingResult bindingResult, Model model)
    {
        validateResultUnitModifierConfiguration(resultUnitModifierConfiguration, bindingResult);

        if (bindingResult.hasErrors()) {
            return "informationretrieval/result_unit_modifier_configuration_edit";
        }

        resultUnitModifierConfigurationDAO.saveOrUpdate(resultUnitModifierConfiguration);

        return "redirect:/informationretrieval/resultunitmodifierconfigurationedit/" + resultUnitModifierConfiguration.getId();
    }

    public void validateResultUnitModifierConfiguration(ResultUnitModifierConfiguration resultUnitModifierConfiguration, BindingResult bindingResult)
    {
        try {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(resultUnitModifierConfiguration.getGroovyCode());

            Constructor cons = clazz.getDeclaredConstructor(SystemLogger.class);
            cons.newInstance(systemLogger);
        } catch (Exception e) {
            bindingResult.rejectValue("groovyCode", "resourceLoaderPluginConfiguration.groovyCode", e.getMessage() + StacktraceUtil.getStackTrace(e));
        }
    }
}
