package ch.ksfx.controller.admin.dashboard;

import ch.ksfx.dao.GenericDataStoreDAO;
import ch.ksfx.model.GenericDataStore;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/dashboardmanager")
public class DashboardManagerController
{
    private final String DASHBOARD_CONTENT_KEY = "DASHBOARD_CONTENT";
    private GenericDataStoreDAO genericDataStoreDAO;

    public DashboardManagerController(GenericDataStoreDAO genericDataStoreDAO)
    {
        this.genericDataStoreDAO = genericDataStoreDAO;
    }

    @GetMapping("/")
    public String editDashboardContent(Model model)
    {
        GenericDataStore genericDataStore = genericDataStoreDAO.getGenericDataStoreForKey(DASHBOARD_CONTENT_KEY);

        if (genericDataStore == null) {
            genericDataStore = new GenericDataStore(DASHBOARD_CONTENT_KEY, "");
        }

        model.addAttribute("dashboardContent", genericDataStore.getDataValue());

        return "admin/dashboard/dashboard_manager";
    }

    @PostMapping("/")
    public String editDashboardContent(@RequestParam(value = "content", required = true) String content)
    {
        GenericDataStore genericDataStore = genericDataStoreDAO.getGenericDataStoreForKey(DASHBOARD_CONTENT_KEY);

        if (genericDataStore == null) {
            genericDataStore = new GenericDataStore(DASHBOARD_CONTENT_KEY, "");
        }

        genericDataStore.setDataValue(content);

        genericDataStoreDAO.saveOrUpdateGenericDataStore(genericDataStore);

        return "redirect:/admin/dashboardmanager/";
    }
}
