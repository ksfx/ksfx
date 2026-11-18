package ch.ksfx.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Landing page for a valid account without any project membership (concept, Q3) - see NoProjectInterceptor. */
@Controller
public class NoProjectController
{
    @GetMapping("/noproject")
    public String noProject()
    {
        return "noproject";
    }
}
