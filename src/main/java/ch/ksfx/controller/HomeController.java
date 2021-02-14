//https://github.com/neocorp/spring-mvc-thymeleaf-crud/tree/master/src/main/resources/templates
package ch.ksfx.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController
{
    @RequestMapping("/")
    public String index()
    {
        System.out.println("Home controller invoked");
        return "home";
    }
}
