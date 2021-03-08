package ch.ksfx.controller.informationretrieval;

import ch.ksfx.dao.spidering.ResultDAO;
import ch.ksfx.dao.spidering.SpideringConfigurationDAO;
import ch.ksfx.model.spidering.Result;
import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.model.spidering.SpideringConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/informationretrieval/result")
public class ResultController
{
    private ResultDAO resultDAO;
    private SpideringConfigurationDAO spideringConfigurationDAO;

    public ResultController(ResultDAO resultDAO, SpideringConfigurationDAO spideringConfigurationDAO)
    {
        this.resultDAO = resultDAO;
        this.spideringConfigurationDAO = spideringConfigurationDAO;
    }

    @GetMapping("/{spideringconfigurationid}")
    public String resultIndex(@PathVariable(value = "spideringconfigurationid", required = true) Long spideringConfigurationId, Pageable pageable, Model model, HttpServletRequest request)
    {
        boolean filterInvalidResults = false;

        if (request.getSession().getAttribute("filterInvalidResults") != null) {
            System.out.println("Dere " + request.getSession().getAttribute("filterInvalidResults"));
            filterInvalidResults = (Boolean) request.getSession().getAttribute("filterInvalidResults");
        }

        SpideringConfiguration spideringConfiguration = spideringConfigurationDAO.getSpideringConfigurationForId(spideringConfigurationId);
        Page<Result> resultsPage = resultDAO.getResultsForPageableAndSpideringConfigurationAndFilter(pageable, spideringConfiguration, filterInvalidResults);

        model.addAttribute("filterInvalidResults", filterInvalidResults);
        model.addAttribute("spideringConfiguration", spideringConfiguration);
        model.addAttribute("resultsPage", resultsPage);

        return "informationretrieval/result";
    }

    @PostMapping("/{spideringconfigurationid}")
    public String filterInvalidSubmit(@PathVariable(value = "spideringconfigurationid", required = true) Long spideringConfigurationId, @RequestParam(required = false) Boolean filterInvalidResults, HttpServletRequest request)
    {
        System.out.println("POST " + filterInvalidResults);

        if (filterInvalidResults == null) {
            filterInvalidResults = false;
        }

        request.getSession().setAttribute("filterInvalidResults", filterInvalidResults);

        return "redirect:/informationretrieval/result/" + spideringConfigurationId;
    }

}
