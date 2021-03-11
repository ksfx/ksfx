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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/informationretrieval")
public class ResultUnitModifierConfigurationController
{
    private ResultUnitModifierConfigurationDAO resultUnitModifierConfigurationDAO;
    private SystemLogger systemLogger;
    private ServiceProvider serviceProvider;

    public ResultUnitModifierConfigurationController(ResultUnitModifierConfigurationDAO resultUnitModifierConfigurationDAO, SystemLogger systemLogger, ServiceProvider serviceProvider)
    {
        this.resultUnitModifierConfigurationDAO = resultUnitModifierConfigurationDAO;
        this.systemLogger = systemLogger;
        this.serviceProvider = serviceProvider;
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
        } else {
            InputStream inputStream = null;
            String resultUnitModifierDemo = null;

            try {
                inputStream = getClass().getClassLoader().getResourceAsStream("groovy/DemoResultUnitModifier.groovy");

                resultUnitModifierDemo = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            resultUnitModifierConfiguration.setGroovyCode(resultUnitModifierDemo);
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

            Constructor cons = clazz.getDeclaredConstructor(ServiceProvider.class);
            cons.newInstance(serviceProvider);
        } catch (Exception e) {
            bindingResult.rejectValue("groovyCode", "resourceLoaderPluginConfiguration.groovyCode", e.getMessage() + StacktraceUtil.getStackTrace(e));
        }
    }

    @GetMapping({"/resultunitmodifierconfigurationdelete/{id}"})
    public String resultUnitModifierConfigurationDelete(@PathVariable(value = "id", required = true) Long resultUnitModifierConfigurationId, Model model)
    {
        ResultUnitModifierConfiguration resultUnitModifierConfiguration = resultUnitModifierConfigurationDAO.getResultUnitModifierConfigurationForId(resultUnitModifierConfigurationId);

        resultUnitModifierConfigurationDAO.deleteResultUnitModifierConfiguration(resultUnitModifierConfiguration);

        return "redirect:/informationretrieval/resultunitmodifierconfiguration";
    }
}
