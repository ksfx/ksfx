package ch.ksfx.controller.console;

import org.springframework.beans.factory.annotation.Autowired;
import ch.ksfx.services.project.UserScope;
import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.publishing.PublishingConfiguration;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/console")
public class ConsoleController
{
    @Autowired
    private UserScope userScope;

    private PublishingConfigurationDAO publishingConfigurationDAO;
    private ActivityInstanceDAO activityInstanceDAO;

    public ConsoleController(PublishingConfigurationDAO publishingConfigurationDAO, ActivityInstanceDAO activityInstanceDAO)
    {
        this.publishingConfigurationDAO = publishingConfigurationDAO;
        this.activityInstanceDAO = activityInstanceDAO;
    }

    @GetMapping("/{entityType}/{entityId}")
    public String viewConsole(@PathVariable(value = "entityType", required = true) String entityType, @PathVariable(value = "entityId", required = true) Long entityId, Model model)
    {
        String console = "";

        if (entityType.equals("activityInstance")) {
            userScope.requireRead(activityInstanceDAO.getActivityInstanceForId(entityId).getActivity().getProject());
            console = StringEscapeUtils.escapeHtml(activityInstanceDAO.getActivityInstanceForId(entityId).getConsole());
        } else if (entityType.equals("publishingConfiguration")) {
            userScope.requireRead(publishingConfigurationDAO.getPublishingConfigurationForId(entityId).getProject());
            console = StringEscapeUtils.escapeHtml(publishingConfigurationDAO.getPublishingConfigurationForId(entityId).getConsole());
        }

        model.addAttribute("entityType",entityType);
        model.addAttribute("entityId",entityId);
        model.addAttribute("console",console);

        return "console/console";
    }

    @GetMapping("/clearconsole/{entityType}/{entityId}")
    public String clearConsole(@PathVariable(value = "entityType", required = true) String entityType, @PathVariable(value = "entityId", required = true) Long entityId, Model model)
    {
        if (entityType.equals("activityInstance")) {
            ActivityInstance ai = activityInstanceDAO.getActivityInstanceForId(entityId);
            userScope.requireWrite(ai.getActivity().getProject());
            ai.setConsole("");
            activityInstanceDAO.saveOrUpdateActivityInstance(ai);
        } else if (entityType.equals("publishingConfiguration")) {
            PublishingConfiguration pc = publishingConfigurationDAO.getPublishingConfigurationForId(entityId);
            userScope.requireWrite(pc.getProject());
            pc.setConsole("");
            publishingConfigurationDAO.saveOrUpdatePublishingConfiguration(pc);
        }

        return "redirect:/console/" + entityType + "/" + entityId;
    }
}
