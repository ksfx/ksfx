//https://github.com/neocorp/spring-mvc-thymeleaf-crud/tree/master/src/main/resources/templates
package ch.ksfx.controller;

import ch.ksfx.dao.GenericDataStoreDAO;
import ch.ksfx.model.GenericDataStore;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController
{
    public static final String HOMEPAGE_CONTENT_KEY = "LOGIN_PAGE_INFORMATION";

    private GenericDataStoreDAO genericDataStoreDAO;

    public HomeController(GenericDataStoreDAO genericDataStoreDAO)
    {
        this.genericDataStoreDAO = genericDataStoreDAO;
    }

    @RequestMapping("/")
    public String index(Model model)
    {
        GenericDataStore genericDataStore = genericDataStoreDAO.getGenericDataStoreForKey(HomeController.HOMEPAGE_CONTENT_KEY);

        if (genericDataStore == null) {
            genericDataStore = new GenericDataStore(HomeController.HOMEPAGE_CONTENT_KEY, "");
        }

        model.addAttribute("homepageContent", genericDataStore.getDataValue());

        return "home";
    }
}
