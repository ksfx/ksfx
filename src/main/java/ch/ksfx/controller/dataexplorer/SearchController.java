package ch.ksfx.controller.dataexplorer;

import ch.ksfx.model.spidering.ResourceLoaderPluginConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dataexplorer")
public class SearchController
{
    @GetMapping("/search")
    public String search(Model model, HttpServletRequest request)
    {
        SearchCriteria searchCriteria = new SearchCriteria();


        if (request.getSession().getAttribute("searchCriteria") != null) {
            searchCriteria = (SearchCriteria) request.getSession().getAttribute("searchCriteria");
        }

        model.addAttribute("searchCriteria", searchCriteria);

        return "dataexplorer/search";
    }

    @PostMapping("/search")
    public String searchSubmit(@Valid @ModelAttribute SearchCriteria searchCriteria, BindingResult bindingResult, Model model, HttpServletRequest request)
    {
        request.getSession().setAttribute("searchCriteria", searchCriteria);
/*
        if (request.getSession().getAttribute("searchCriteria") != null) {
            List<String> complexValueQueryKeysSess = (List<String>) request.getSession().getAttribute("complexValueQueryKeys");
            List<String> complexValueQueryValuesSess = (List<String>) request.getSession().getAttribute("complexValueQueryValues");

            complexValueQueryKeys.addAll(complexValueQueryKeysSess);
            complexValueQueryValues.addAll(complexValueQueryValuesSess);
        }
*/
//
//        request.getSession().setAttribute("complexValueQueryValues", complexValueQueryValues);

        return "redirect:/dataexplorer/search";
    }


    @GetMapping(value = "/suggestseries", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity suggestSeries(@RequestParam(name = "search") String search)
    {
        Map<String, String> myMap = new HashMap<String, String>();
        myMap.put("1", "Allianz");
        myMap.put("2", "Generali");

        return new ResponseEntity<Object>(myMap, HttpStatus.OK);
    }

    @GetMapping("/searchaddcomplexvaluequery")
    public String searchAddComplexValueQuery(HttpServletRequest request)
    {
        SearchCriteria searchCriteria = new SearchCriteria();

        if (request.getSession().getAttribute("searchCriteria") != null) {
            searchCriteria = (SearchCriteria) request.getSession().getAttribute("searchCriteria");
        }

        if (searchCriteria.getComplexValueQueryKeys() == null) {
            searchCriteria.setComplexValueQueryKeys(new ArrayList<String>());
            searchCriteria.setComplexValueQueryValues(new ArrayList<String>());
        }

        searchCriteria.getComplexValueQueryKeys().add("");
        searchCriteria.getComplexValueQueryValues().add("");

        request.getSession().setAttribute("searchCriteria", searchCriteria);

        return "redirect:/dataexplorer/search";
    }
}
