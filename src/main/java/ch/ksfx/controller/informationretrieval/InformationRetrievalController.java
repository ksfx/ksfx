package ch.ksfx.controller.informationretrieval;

import ch.ksfx.dao.spidering.SpideringConfigurationDAO;
import ch.ksfx.dao.spidering.UrlFragmentDAO;
import ch.ksfx.model.spidering.SpideringConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

//https://www.baeldung.com/spring-boot-crud-thymeleaf
@Controller
@RequestMapping("/informationretrieval")
public class InformationRetrievalController
{
    private final SpideringConfigurationDAO spideringConfigurationDAO;
    private final UrlFragmentDAO urlFragmentDAO;

    public InformationRetrievalController(SpideringConfigurationDAO spideringConfigurationDAO, UrlFragmentDAO urlFragmentDAO)
    {
        this.spideringConfigurationDAO = spideringConfigurationDAO;
        this.urlFragmentDAO = urlFragmentDAO;
    }

    @GetMapping("/")
    public String informationRetrievalIndex(Pageable pageable, Model model)
    {
        Page<SpideringConfiguration> spideringConfigurationsPage = spideringConfigurationDAO.getSpideringConfigutationsForPageable(pageable);

        model.addAttribute("spideringConfigurationsPage", spideringConfigurationsPage);

        return "informationretrieval/information_retrieval";
    }

    /*
    @GetMapping("/spideringconfigurationedit")
    public String spideringConfigurationEdit(Model model)
    {
        model.addAttribute("spideringConfiguration", new SpideringConfiguration());

        return "informationretrieval/spidering_configuration_edit";
    }
     */

    @GetMapping({"/spideringconfigurationedit", "/spideringconfigurationedit/{id}"})
    public String spideringConfigurationEdit(@PathVariable(value = "id", required = false) Long spideringConfigurationId, Model model)
    {
        SpideringConfiguration spideringConfiguration = new SpideringConfiguration();

        if (spideringConfigurationId != null) {
            spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
        }

        model.addAttribute("allUrlFragmentFinders", urlFragmentDAO.getAllUrlFragmentFinders());
        model.addAttribute("spideringConfiguration", spideringConfiguration);

        return "informationretrieval/spidering_configuration_edit";
    }

    @PostMapping({"/spideringconfigurationedit", "/spideringconfigurationedit/{id}"})
    public String informationRetrievalSubmit(@PathVariable(value = "id", required = false) Long spideringConfigurationId, @ModelAttribute SpideringConfiguration spideringConfiguration, Model model)
    {
        System.out.println("Spidering Configuration name:" + spideringConfiguration.getName());
        System.out.println("Spidering Configuration resource configurations:" + spideringConfiguration.getResourceConfigurations());
        System.out.println("Spidering Configuration root url:" + spideringConfiguration.getRootUrl());

        model.addAttribute("allUrlFragmentFinders", urlFragmentDAO.getAllUrlFragmentFinders());
        model.addAttribute("spideringConfiguration", spideringConfiguration);

        return "informationretrieval/spidering_configuration_edit";
    }
}
