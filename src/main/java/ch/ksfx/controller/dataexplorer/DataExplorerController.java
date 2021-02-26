package ch.ksfx.controller.dataexplorer;

import ch.ksfx.services.seriesbrowser.SeriesBrowser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/dataexplorer")
public class DataExplorerController
{
    private SeriesBrowser seriesBrowser;

    private List<String> openNodes;
    private List<String> filteredSeriesNames;
    private String seriesNameSearch;

    public DataExplorerController(SeriesBrowser seriesBrowser)
    {
        this.seriesBrowser = seriesBrowser;
    }

    @GetMapping("/")
    public String dataExplorerIndex(Model model, HttpServletRequest request)
    {
        List<String> openNodes = (List<String>) request.getSession().getAttribute("openNodes");
        List<String> filteredSeriesNames = (List<String>) request.getSession().getAttribute("filteredSeriesNames");

        if (openNodes == null) {
            openNodes = new ArrayList<String>();
        }

        if (filteredSeriesNames == null) {
            filteredSeriesNames = new ArrayList<String>();
        }

        model.addAttribute("browser", seriesBrowser.getMarkupForNode(openNodes, filteredSeriesNames));

        return "dataexplorer/data_explorer";
    }
}
