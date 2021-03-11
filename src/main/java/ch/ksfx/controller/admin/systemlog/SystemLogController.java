package ch.ksfx.controller.admin.systemlog;

import ch.ksfx.dao.LogMessageDAO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/systemlog")
public class SystemLogController
{
    private LogMessageDAO logMessageDAO;

    public SystemLogController(LogMessageDAO logMessageDAO)
    {
        this.logMessageDAO = logMessageDAO;
    }

    @GetMapping("/")
    public String systemLogIndex(Pageable pageable, Model model)
    {
        model.addAttribute("logMessagesPage", logMessageDAO.getLogMessagesForPageableAndTag(pageable, null));

        return "admin/systemlog/system_log";
    }

    @GetMapping("/clearlogs")
    public String clearLogs()
    {
        logMessageDAO.clearLogMessages();

        return "redirect:/admin/systemlog/";
    }
}
