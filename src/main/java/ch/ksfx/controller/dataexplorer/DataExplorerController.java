package ch.ksfx.controller.dataexplorer;

import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.model.Observation;
import ch.ksfx.services.seriesbrowser.SeriesBrowser;
import ch.ksfx.util.DateFormatUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/dataexplorer")
public class DataExplorerController
{
    private SeriesBrowser seriesBrowser;
    private ObservationDAO observationDAO;
    private ActivityDAO activityDAO;

    private List<String> openNodes;
    private List<String> filteredSeriesNames;
    private String seriesNameSearch;

    public DataExplorerController(SeriesBrowser seriesBrowser,ObservationDAO observationDAO, ActivityDAO activityDAO)
    {
        this.seriesBrowser = seriesBrowser;
        this.observationDAO = observationDAO;
        this.activityDAO = activityDAO;
    }

    @GetMapping("/")
    public String dataExplorerIndex(Pageable pageable, Model model, HttpServletRequest request)
    {
        List<String> openNodes = (List<String>) request.getSession().getAttribute("openNodes");
        List<String> filteredSeriesNames = (List<String>) request.getSession().getAttribute("filteredSeriesNames");

        if (openNodes == null) {
            openNodes = new ArrayList<String>();
        }

        if (filteredSeriesNames == null) {
            filteredSeriesNames = new ArrayList<String>();
        }

        Page<Observation> observationsPage = observationDAO.getObservationsForPageableAndTimeSSeriesId(pageable, 1);

        model.addAttribute("observationsPage", observationsPage);
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

        model.addAttribute("allActivities", activityDAO.getAllActivities());
        model.addAttribute("observation", observation);

        return "dataexplorer/observation_viewer";
    }

    @GetMapping("/opennode/{node}")
    public String opennode(@PathVariable(value = "node", required = true) String node, Model model, HttpServletRequest request)
    {
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

}
