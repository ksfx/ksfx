package ch.ksfx.controller.dataexplorer;

import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.services.SystemEnvironment;
import ch.ksfx.services.lucene.ObservationSearch;
import ch.ksfx.util.DateFormatUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.data.domain.Pageable;
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
    private ObservationSearch observationSearch;
    private TimeSeriesDAO timeSeriesDAO;

    public SearchController(SystemEnvironment systemEnvironment, ObservationDAO observationDAO, TimeSeriesDAO timeSeriesDAO)
    {
        this.observationSearch = new ObservationSearch(systemEnvironment, observationDAO);
        this.timeSeriesDAO = timeSeriesDAO;
    }

    @GetMapping("/search")
    public String search(Model model, HttpServletRequest request, Pageable pageable)
    {
        SearchCriteria searchCriteria = new SearchCriteria();

        if (request.getSession().getAttribute("searchCriteria") != null) {
            searchCriteria = (SearchCriteria) request.getSession().getAttribute("searchCriteria");
        }

        model.addAttribute("dateFormatUtil", new DateFormatUtil());
        model.addAttribute("searchCriteria", searchCriteria);

        if (searchCriteria.searchActive()) {
            model.addAttribute("resultsPage", observationSearch.getPagedSearch(pageable, searchCriteria.getAllQuery(), searchCriteria.getScalarValueQuery(), searchCriteria.getComplexValueQuery(), searchCriteria.getMetaDataQuery(), searchCriteria.getDateFrom(), searchCriteria.getDateTo(), searchCriteria.getSeriesId()));
        }

        return "dataexplorer/search";
    }

    @PostMapping("/search")
    public String searchSubmit(@Valid @ModelAttribute SearchCriteria searchCriteria, BindingResult bindingResult, Model model, HttpServletRequest request)
    {
        System.out.println("Date FROM " + searchCriteria.getDateFrom());
        System.out.println("Date FROM " + searchCriteria.getDateTo());

        if (searchCriteria.getDateFrom() != null) {
            searchCriteria.setDateFrom(DateUtils.addHours(searchCriteria.getDateFrom(), 0));
            searchCriteria.setDateFrom(DateUtils.addMinutes(searchCriteria.getDateFrom(), 0));
            searchCriteria.setDateFrom(DateUtils.addSeconds(searchCriteria.getDateFrom(), 0));
        }

        if (searchCriteria.getDateTo() != null) {
            searchCriteria.setDateTo(DateUtils.addHours(searchCriteria.getDateTo(), 23));
            searchCriteria.setDateTo(DateUtils.addMinutes(searchCriteria.getDateTo(), 59));
            searchCriteria.setDateTo(DateUtils.addSeconds(searchCriteria.getDateTo(), 59));
        }

        request.getSession().setAttribute("searchCriteria", searchCriteria);

        return "redirect:/dataexplorer/search";
    }

    @GetMapping("/searchreset")
    public String search(HttpServletRequest request)
    {
        request.getSession().setAttribute("searchCriteria", new SearchCriteria());

        return "redirect:/dataexplorer/search";
    }

    @GetMapping(value = "/searchsuggestseries", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity suggestSeries(@RequestParam(name = "search") String search)
    {
        System.out.println("Search: " + search);

        List<TimeSeries> timeSeries = timeSeriesDAO.searchTimeSeries(search, 100);

        List<Map<String, String>> json = new ArrayList<Map<String, String>>();

        for (TimeSeries ts : timeSeries) {
            Map<String, String> jsonMap = new HashMap<String,String>();
            jsonMap.put("snippetIdxId", ts.getId().toString());
            jsonMap.put("snippet", StringUtils.abbreviate(ts.getName(), 80));
            jsonMap.put("positionHint", StringUtils.abbreviate(ts.getLocator(), 80));

            json.add(jsonMap);
        }

        System.out.println(json);

        return new ResponseEntity<Object>(json, HttpStatus.OK);
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
