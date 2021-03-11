package ch.ksfx.controller.dashboard;

import ch.ksfx.services.lucene.IndexService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController
{
    private IndexService indexService;

    public DashboardController(IndexService indexService)
    {
        this.indexService = indexService;
    }

    @RequestMapping("/")
    public String index(Model model)
    {
        model.addAttribute("indexService", indexService);

        return "dashboard/dashboard";
    }
}
