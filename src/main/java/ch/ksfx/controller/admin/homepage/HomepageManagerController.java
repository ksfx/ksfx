package ch.ksfx.controller.admin.homepage;

import ch.ksfx.controller.HomeController;
import ch.ksfx.dao.GenericDataStoreDAO;
import ch.ksfx.model.GenericDataStore;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/homepagemanager")
public class HomepageManagerController
{
    private GenericDataStoreDAO genericDataStoreDAO;

    public HomepageManagerController(GenericDataStoreDAO genericDataStoreDAO)
    {
        this.genericDataStoreDAO = genericDataStoreDAO;
    }

    @GetMapping("/")
    public String editHomepageContent(Model model)
    {
        GenericDataStore genericDataStore = genericDataStoreDAO.getGenericDataStoreForKey(HomeController.HOMEPAGE_CONTENT_KEY);

        if (genericDataStore == null) {
            genericDataStore = new GenericDataStore(HomeController.HOMEPAGE_CONTENT_KEY, "");
        }

        model.addAttribute("homepageContent", genericDataStore.getDataValue());

        return "admin/homepage/homepage_manager";
    }

    @PostMapping("/")
    public String editHomepageContentSubmit(@RequestParam(value = "content", required = true) String content)
    {
        GenericDataStore genericDataStore = genericDataStoreDAO.getGenericDataStoreForKey(HomeController.HOMEPAGE_CONTENT_KEY);

        if (genericDataStore == null) {
            genericDataStore = new GenericDataStore(HomeController.HOMEPAGE_CONTENT_KEY, "");
        }

        genericDataStore.setDataValue(content);

        genericDataStoreDAO.saveOrUpdateGenericDataStore(genericDataStore);

        return "redirect:/admin/homepagemanager/";
    }
}
