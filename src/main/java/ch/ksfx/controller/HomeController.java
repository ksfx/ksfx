//https://github.com/neocorp/spring-mvc-thymeleaf-crud/tree/master/src/main/resources/templates
package ch.ksfx.controller;

import ch.ksfx.dao.GenericDataStoreDAO;
import ch.ksfx.model.GenericDataStore;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    public String index(Model model, HttpServletRequest request)
    {
        Cookie cookie = WebUtils.getCookie(request, "ksfxdisplaymode");

        if (cookie != null) {
            request.getSession().setAttribute("ksfxdisplaymode", cookie.getValue());
        }

        GenericDataStore genericDataStore = genericDataStoreDAO.getGenericDataStoreForKey(HomeController.HOMEPAGE_CONTENT_KEY);

        if (genericDataStore == null) {
            genericDataStore = new GenericDataStore(HomeController.HOMEPAGE_CONTENT_KEY, "");
        }

        model.addAttribute("homepageContent", genericDataStore.getDataValue());

        return "home";
    }

    @RequestMapping("/toggledisplaymode")
    public String toggleDisplayMode(HttpServletRequest request, HttpServletResponse response)
    {
        Cookie cookie = WebUtils.getCookie(request, "ksfxdisplaymode");

        if (cookie == null) {
            cookie = new Cookie("ksfxdisplaymode", "dark");
        } else if (cookie.getValue() != null && cookie.getValue().equals("bright")) {
            cookie.setValue("dark");
        } else if (cookie.getValue() != null && cookie.getValue().equals("dark")) {
            cookie.setValue("bright");
        }

//maxAge is one month: 30*24*60*60
        cookie.setMaxAge(2592000);
//cookie.setDomain("projectName");
//cookie.setPath("/");
        response.addCookie(cookie);

//        if (request.getHeader("Referer") != null) {
//            return "redirect:" + request.getHeader("Referer");
//        }

        return "redirect:/";
    }
}
