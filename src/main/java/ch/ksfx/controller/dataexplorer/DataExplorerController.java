package ch.ksfx.controller.dataexplorer;

import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.model.Observation;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.services.seriesbrowser.SeriesBrowser;
import ch.ksfx.util.DateFormatUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
@RequestMapping("/dataexplorer")
public class DataExplorerController
{
    private SeriesBrowser seriesBrowser;
    private ObservationDAO observationDAO;
    private ActivityDAO activityDAO;
    private TimeSeriesDAO timeSeriesDAO;

//    private List<String> openNodes;
//    private List<String> filteredSeriesNames;
//    private String seriesNameSearch;

    public DataExplorerController(SeriesBrowser seriesBrowser,ObservationDAO observationDAO, ActivityDAO activityDAO, TimeSeriesDAO timeSeriesDAO)
    {
        this.seriesBrowser = seriesBrowser;
        this.observationDAO = observationDAO;
        this.activityDAO = activityDAO;
        this.timeSeriesDAO = timeSeriesDAO;
    }

    @GetMapping({"/{timeSeriesId}","/"})
    public String dataExplorerIndex(@PathVariable(value = "timeSeriesId", required = false) Integer timeSeriesId, Pageable pageable, Model model, HttpServletRequest request)
    {
        List<String> openNodes = (List<String>) request.getSession().getAttribute("openNodes");
        List<String> filteredSeriesNames = (List<String>) request.getSession().getAttribute("filteredSeriesNames");
        Boolean searchActive = (Boolean) request.getSession().getAttribute("searchActive");
        String seriesNameSearch = (String) request.getSession().getAttribute("seriesNameSearch");

        if (timeSeriesId == null) {
            TimeSeries ts = timeSeriesDAO.getFirstTimeSeriesInDatabase();

            if (ts != null) {
                timeSeriesId = ts.getId().intValue();
            }
        }

        if (openNodes == null) {
            openNodes = new ArrayList<String>();
        }

        if (filteredSeriesNames == null) {
            filteredSeriesNames = new ArrayList<String>();
        }

        if (searchActive == null) {
            searchActive = false;
        }

        if (seriesNameSearch == null) {
            seriesNameSearch = "";
        }

        model.addAttribute("seriesNameSearch", seriesNameSearch);
        model.addAttribute("searchActive", searchActive);

        if (timeSeriesId != null) {
            model.addAttribute("timeSeries", timeSeriesDAO.getTimeSeriesForId(timeSeriesId.longValue()));

            Page<Observation> observationsPage = observationDAO.getObservationsForPageableAndTimeSSeriesId(pageable, timeSeriesId);
            model.addAttribute("observationsPage", observationsPage);
        }

        model.addAttribute("browser", seriesBrowser.getMarkupForNode(openNodes, filteredSeriesNames));
        model.addAttribute("dateFormatUtil", new DateFormatUtil());

        return "dataexplorer/data_explorer";
    }

    @GetMapping("/viewobservation/{timeSeriesId}/{observationTime}/{sourceId}")
    public String viewObservation(@PathVariable(value = "timeSeriesId", required = true) String timeSeriesIdParam,
                                  @PathVariable(value = "observationTime", required = true) String observationTimeParam,
                                  @PathVariable(value = "sourceId", required = true) String sourceIdParam,
                                  Model model)
    {
        Observation observation = observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(Integer.parseInt(timeSeriesIdParam), DateFormatUtil.parseISO8601TimeAndDateString(observationTimeParam), UriUtils.decode(sourceIdParam,
                "UTF-8"));

        model.addAttribute("dateFormatUtil", new DateFormatUtil());
        model.addAttribute("allActivities", activityDAO.getAllActivities());
        model.addAttribute("observation", observation);

        return "dataexplorer/observation_viewer";
    }

    @GetMapping("/deleteobservation/{timeSeriesId}/{observationTime}/{sourceId}")
    public String deleteObservation(@PathVariable(value = "timeSeriesId", required = true) String timeSeriesIdParam,
                                  @PathVariable(value = "observationTime", required = true) String observationTimeParam,
                                  @PathVariable(value = "sourceId", required = true) String sourceIdParam,
                                  Model model)
    {
        Observation observation = observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(Integer.parseInt(timeSeriesIdParam), DateFormatUtil.parseISO8601TimeAndDateString(observationTimeParam), UriUtils.decode(sourceIdParam,
                "UTF-8"));

        observationDAO.deleteObservation(observation);

        return "redirect:/dataexplorer/" + observation.getTimeSeriesId();
    }

    @GetMapping("/deletetimeseriesobservations/{timeSeriesId}")
    public String deleteTimeSeriesObservations(@PathVariable(value = "timeSeriesId", required = true) Long timeSeriesId,
                                    Model model)
    {
        TimeSeries timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);

        observationDAO.deleteAllObservationsForTimeSeries(timeSeries);

        return "redirect:/dataexplorer/" + timeSeries.getId();
    }

    @GetMapping("/opennode/{node}")
    public String opennode(@PathVariable(value = "node", required = true) String node, Model model, HttpServletRequest request)
    {
        List<String> openNodes = (List<String>) request.getSession().getAttribute("openNodes");

        if (openNodes == null) {
            openNodes = new ArrayList<String>();
        }

        if (!openNodes.contains(node)) {
            openNodes.add(node);
        }

        Collections.sort(openNodes);

        request.getSession().setAttribute("openNodes", openNodes);

        return "redirect:/dataexplorer/";
    }

    @GetMapping("/closenode/{node}")
    public String closenode(@PathVariable(value = "node", required = true) String nodeString, Model model, HttpServletRequest request)
    {
        List<String> openNodes = (List<String>) request.getSession().getAttribute("openNodes");

        if (openNodes == null) {
            openNodes = new ArrayList<String>();
        }

        List<String> nodesToClose = new ArrayList<String>();

        for (String node : openNodes) {
            if (node.contains(nodeString)) {
                nodesToClose.add(node);
            }
        }

        for (String nodeToClose : nodesToClose) {
            openNodes.remove(nodeToClose);
        }

        Collections.sort(openNodes);

        request.getSession().setAttribute("openNodes", openNodes);

        return "redirect:/dataexplorer/";
    }

    @PostMapping({"/searchseries"})
    public String onSuccessFromSeriesNameSearchForm(@RequestParam(name = "seriesNameSearch") String seriesNameSearch, HttpServletRequest request)
    {
        List<String> filteredSeriesNames = new ArrayList<String>();
        List<String> openNodes = new ArrayList<String>();

        if (seriesNameSearch != null && seriesNameSearch.length() >= 3) {
            List<TimeSeries> timeSeries = timeSeriesDAO.searchTimeSeries(seriesNameSearch, 100);

            for (TimeSeries ts : timeSeries) {
                String locator = ts.getLocator();
                String[] parts = locator.split("-");

                for (Integer i = 0; i < parts.length; i++) {
                    List<String> listParts = Arrays.asList(parts);
                    List<String> subParts = listParts.subList(0,i+1);

                    String locatorPart = StringUtils.join(subParts, "-");

                    if (!openNodes.contains(locatorPart)) {
                        openNodes.add(locatorPart);
                    }
                }


                filteredSeriesNames.add(ts.getName());
            }

            Collections.sort(openNodes);

            request.getSession().setAttribute("seriesNameSearch", seriesNameSearch);
            request.getSession().setAttribute("openNodes", openNodes);
            request.getSession().setAttribute("filteredSeriesNames", filteredSeriesNames);
            request.getSession().setAttribute("searchActive", true);
        }

        return "redirect:/dataexplorer/";
    }

    @GetMapping({"/searchseriesreset"})
    public String onActionFromResetSearch(HttpServletRequest request)
    {
        request.getSession().setAttribute("seriesNameSearch", "");
        request.getSession().setAttribute("openNodes", new ArrayList<String>());
        request.getSession().setAttribute("filteredSeriesNames", new ArrayList<String>());
        request.getSession().setAttribute("searchActive", false);

        return "redirect:/dataexplorer/";
    }

    @GetMapping(value = "/dataexplorersuggestseries", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity suggestSeries(@RequestParam(name = "search") String search)
    {
        List<TimeSeries> timeSeries = timeSeriesDAO.searchTimeSeries(search, 100);

        List<Map<String, String>> json = new ArrayList<Map<String, String>>();

        for (TimeSeries ts : timeSeries) {
            Map<String, String> jsonMap = new HashMap<String,String>();
            jsonMap.put("snippetIdxId", '"' + ts.getName() + '"');
            jsonMap.put("snippet", StringUtils.abbreviate(ts.getName(), 80));
            jsonMap.put("positionHint", StringUtils.abbreviate(ts.getLocator(), 80));

            json.add(jsonMap);
        }

        return new ResponseEntity<Object>(json, HttpStatus.OK);
    }
}
