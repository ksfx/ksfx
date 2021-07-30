package ch.ksfx.controller.admin.systemlog;

import ch.ksfx.controller.dataexplorer.SearchCriteria;
import ch.ksfx.dao.LogMessageDAO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("/admin/systemlog")
public class SystemLogController
{
    private LogMessageDAO logMessageDAO;

    public SystemLogController(LogMessageDAO logMessageDAO)
    {
        this.logMessageDAO = logMessageDAO;
    }

    @PostMapping("/selecttag")
    public String selectTag(@RequestParam(name = "selectedTag") String selectedTag, HttpServletRequest request)
    {
        request.getSession().setAttribute("selectSystemLogTag", selectedTag);

        return "redirect:/admin/systemlog/";
    }

    @GetMapping("/")
    public String systemLogIndex(Pageable pageable, Model model, HttpServletRequest request)
    {
        String selectedTag = null;

        if (request.getSession().getAttribute("selectSystemLogTag") != null) {
            selectedTag = (String) request.getSession().getAttribute("selectSystemLogTag");
        }

        if (selectedTag != null && selectedTag.equals("0")) {
            selectedTag = null;
        }

        model.addAttribute("logMessagesPage", logMessageDAO.getLogMessagesForPageableAndTag(pageable, selectedTag));
        model.addAttribute("availableTags", logMessageDAO.getLogMessageTags());
        model.addAttribute("selectedTag", selectedTag);

        return "admin/systemlog/system_log";
    }

    @GetMapping("/clearlogs")
    public String clearLogs()
    {
        logMessageDAO.clearLogMessages();

        return "redirect:/admin/systemlog/";
    }
}
