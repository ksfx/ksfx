package ch.ksfx.controller.dashboard;

import ch.ksfx.controller.HomeController;
import ch.ksfx.dao.GenericDataStoreDAO;
import ch.ksfx.model.GenericDataStore;
import ch.ksfx.services.lucene.IndexService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController
{
    public static final String DASHBOARD_CONTENT_KEY = "DASHBOARD_CONTENT";

    private GenericDataStoreDAO genericDataStoreDAO;

    public DashboardController(GenericDataStoreDAO genericDataStoreDAO)
    {
        this.genericDataStoreDAO = genericDataStoreDAO;
    }

    @RequestMapping("/")
    public String index(Model model)
    {
        GenericDataStore genericDataStore = genericDataStoreDAO.getGenericDataStoreForKey(DashboardController.DASHBOARD_CONTENT_KEY);

        if (genericDataStore == null) {
            genericDataStore = new GenericDataStore(DashboardController.DASHBOARD_CONTENT_KEY, "");
        }

        model.addAttribute("dashboardContent", genericDataStore.getDataValue());

        return "dashboard/dashboard";
    }
}
