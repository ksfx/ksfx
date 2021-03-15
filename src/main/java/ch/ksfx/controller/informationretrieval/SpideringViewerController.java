package ch.ksfx.controller.informationretrieval;

import ch.ksfx.dao.spidering.SpideringConfigurationDAO;
import ch.ksfx.dao.spidering.SpideringDAO;
import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.model.spidering.SpideringConfiguration;
import ch.ksfx.services.spidering.ParsingRunner;
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
    private ParsingRunner parsingRunner;

    public SpideringViewerController(SpideringDAO spideringDAO,
                                     SpideringConfigurationDAO spideringConfigurationDAO,
                                     SpideringRunner spideringRunner,
                                     ParsingRunner parsingRunner)
    {
        this.spideringDAO = spideringDAO;
        this.spideringConfigurationDAO = spideringConfigurationDAO;
        this.spideringRunner = spideringRunner;
        this.parsingRunner = parsingRunner;
    }

    @GetMapping("/spideringsviewer/{spideringconfigurationid}")
    public String informationRetrievalIndex(@PathVariable(value = "spideringconfigurationid", required = true) Long spideringConfigurationId, Pageable pageable, Model model)
    {
        SpideringConfiguration spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
        Page<Spidering> spideringsPage = spideringDAO.getSpideringsForPageableAndSpideringConfiguration(pageable, spideringConfiguration);

        model.addAttribute("spideringDAO", spideringDAO);
        model.addAttribute("spideringRunner", spideringRunner);
        model.addAttribute("parsingRunner", parsingRunner);
        model.addAttribute("spideringConfiguration", spideringConfiguration);
        model.addAttribute("spideringsPage", spideringsPage);

        return "informationretrieval/spiderings_viewer";
    }

    @GetMapping("/spideringterminate/{spideringId}")
    public String terminateSpidering(@PathVariable(value = "spideringId", required = true) Long spideringId)
    {
        Spidering spidering = spideringDAO.getSpideringForId(spideringId);

        spideringRunner.terminateSpidering(spidering);

        return "redirect:/informationretrieval/spideringsviewer/" + spidering.getSpideringConfiguration().getId();
    }

    @GetMapping("/spideringrunparsing/{spideringId}")
    public String runParsing(@PathVariable(value = "spideringId", required = true) Long spideringId)
    {
        Spidering spidering = spideringDAO.getSpideringForId(spideringId);

        parsingRunner.runParsing(spidering);

        return "redirect:/informationretrieval/spideringsviewer/" + spidering.getSpideringConfiguration().getId();
    }

    @GetMapping("/spideringterminateparsing/{spideringId}")
    public String terminateParsing(@PathVariable(value = "spideringId", required = true) Long spideringId)
    {
        Spidering spidering = spideringDAO.getSpideringForId(spideringId);

        parsingRunner.terminateParsing(spidering);

        return "redirect:/informationretrieval/spideringsviewer/" + spidering.getSpideringConfiguration().getId();
    }
}
