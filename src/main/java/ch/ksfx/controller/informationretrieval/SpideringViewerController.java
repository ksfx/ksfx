package ch.ksfx.controller.informationretrieval;

import ch.ksfx.dao.spidering.SpideringConfigurationDAO;
import ch.ksfx.dao.spidering.SpideringDAO;
import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.model.spidering.SpideringConfiguration;
import ch.ksfx.services.spidering.SpideringRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/informationretrieval")
public class SpideringViewerController
{
    private SpideringDAO spideringDAO;
    private SpideringConfigurationDAO spideringConfigurationDAO;
    private SpideringRunner spideringRunner;

    public SpideringViewerController(SpideringDAO spideringDAO, SpideringConfigurationDAO spideringConfigurationDAO, SpideringRunner spideringRunner)
    {
        this.spideringDAO = spideringDAO;
        this.spideringConfigurationDAO = spideringConfigurationDAO;
        this.spideringRunner = spideringRunner;
    }

    @GetMapping("/spideringsviewer/{spideringconfigurationid}")
    public String informationRetrievalIndex(@PathVariable(value = "spideringconfigurationid", required = true) Long spideringConfigurationId, Pageable pageable, Model model)
    {
        SpideringConfiguration spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
        Page<Spidering> spideringsPage = spideringDAO.getSpideringsForPageableAndSpideringConfiguration(pageable, spideringConfiguration);

        model.addAttribute("spideringDAO", spideringDAO);
        model.addAttribute("spideringRunner", spideringRunner);
        model.addAttribute("spideringConfiguration", spideringConfiguration);
        model.addAttribute("spideringsPage", spideringsPage);

        return "informationretrieval/spiderings_viewer";
    }
}
