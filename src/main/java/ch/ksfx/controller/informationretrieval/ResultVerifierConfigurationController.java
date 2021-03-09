package ch.ksfx.controller.informationretrieval;

import ch.ksfx.dao.spidering.ResultVerifierConfigurationDAO;
import ch.ksfx.model.spidering.ResultVerifierConfiguration;
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
public class ResultVerifierConfigurationController
{
    private ResultVerifierConfigurationDAO resultVerifierConfigurationDAO;
    private SystemLogger systemLogger;
    private ServiceProvider serviceProvider;

    public ResultVerifierConfigurationController(ResultVerifierConfigurationDAO resultVerifierConfigurationDAO, SystemLogger systemLogger, ServiceProvider serviceProvider)
    {
        this.resultVerifierConfigurationDAO = resultVerifierConfigurationDAO;
        this.systemLogger = systemLogger;
        this.serviceProvider = serviceProvider;
    }

    @GetMapping("/resultverifierconfiguration")
    public String resultVerifierConfigurationIndex(Model model, Pageable pageable)
    {
        model.addAttribute("resultVerifierConfigurationsPage", resultVerifierConfigurationDAO.getResultVerifierConfigurationsForPageable(pageable));

        return "informationretrieval/result_verifier_configuration";
    }

    @GetMapping({"/resultverifierconfigurationedit", "/resultverifierconfigurationedit/{id}"})
    public String resultVerifierConfigurationEdit(@PathVariable(value = "id", required = false) Long resultVerifierConfigurationId, Model model)
    {
        ResultVerifierConfiguration resultVerifierConfiguration = new ResultVerifierConfiguration();

        if (resultVerifierConfigurationId != null) {
            resultVerifierConfiguration = resultVerifierConfigurationDAO.getResultVerifierConfigurationForId(resultVerifierConfigurationId);
        }

        model.addAttribute("resultVerifierConfiguration", resultVerifierConfiguration);

        return "informationretrieval/result_verifier_configuration_edit";
    }

    @PostMapping({"/resultverifierconfigurationedit", "/resultverifierconfigurationedit/{id}"})
    public String resultVerifierConfigurationSubmit(@PathVariable(value = "id", required = false) Long resultVerifierConfigurationId, @Valid @ModelAttribute ResultVerifierConfiguration resultVerifierConfiguration, BindingResult bindingResult, Model model)
    {
        validateResultVerifierConfiguration(resultVerifierConfiguration, bindingResult);

        if (bindingResult.hasErrors()) {
            return "informationretrieval/result_verifier_configuration_edit";
        }

        resultVerifierConfigurationDAO.saveOrUpdate(resultVerifierConfiguration);

        return "redirect:/informationretrieval/resultverifierconfigurationedit/" + resultVerifierConfiguration.getId();
    }

    public void validateResultVerifierConfiguration(ResultVerifierConfiguration resultVerifierConfiguration, BindingResult bindingResult)
    {
        try {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class clazz = groovyClassLoader.parseClass(resultVerifierConfiguration.getGroovyCode());

            Constructor cons = clazz.getDeclaredConstructor(ServiceProvider.class);
            cons.newInstance(serviceProvider);
        } catch (Exception e) {
            bindingResult.rejectValue("groovyCode", "resultVerifierConfiguration.groovyCode", e.getMessage() + StacktraceUtil.getStackTrace(e));
        }
    }
}