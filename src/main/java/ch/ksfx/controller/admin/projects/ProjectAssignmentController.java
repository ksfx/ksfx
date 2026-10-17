package ch.ksfx.controller.admin.projects;

import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.Project;
import ch.ksfx.services.project.ProjectAssignmentService;
import ch.ksfx.services.project.ProjectAssignmentService.EntityType;
import ch.ksfx.services.project.ProjectAssignmentService.Item;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin → Projects → Assignments: the one screen where entities change project (see
 * ProjectAssignmentService). Two steps: a plan page that says per entity what the move would
 * do (or why it is blocked), then the move itself for the entities that were OK.
 */
@Controller
@RequestMapping("/admin/projects/assignments")
public class ProjectAssignmentController
{
    private final ProjectAssignmentService assignmentService;
    private final ProjectDAO projectDAO;

    public ProjectAssignmentController(ProjectAssignmentService assignmentService, ProjectDAO projectDAO)
    {
        this.assignmentService = assignmentService;
        this.projectDAO = projectDAO;
    }

    @GetMapping({"", "/"})
    public String index(@RequestParam(defaultValue = "WIKI") String type, @RequestParam(required = false) Long projectId, Model model)
    {
        EntityType entityType = parseType(type);
        List<ProjectAssignmentService.Row> rows = assignmentService.list(entityType);

        if (projectId != null) {
            List<ProjectAssignmentService.Row> filtered = new ArrayList<>();

            for (ProjectAssignmentService.Row row : rows) {
                if (projectId.equals(row.projectId)) {
                    filtered.add(row);
                }
            }

            rows = filtered;
        }

        model.addAttribute("type", entityType.name());
        model.addAttribute("types", EntityType.values());
        model.addAttribute("rows", rows);
        model.addAttribute("projects", projectDAO.getAllProjects());
        model.addAttribute("filterProjectId", projectId);

        return "admin/projects/assignments";
    }

    @PostMapping("/plan")
    public String plan(@RequestParam String type, @RequestParam(required = false) List<Long> ids, @RequestParam Long targetProjectId,
                       Model model, RedirectAttributes redirectAttributes)
    {
        EntityType entityType = parseType(type);
        Project target = projectDAO.getProjectForId(targetProjectId);

        if (ids == null || ids.isEmpty() || target == null) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Select at least one entry and a target project.");
            return "redirect:/admin/projects/assignments?type=" + entityType.name();
        }

        List<Item> items = assignmentService.plan(entityType, ids, target);
        int blocked = 0;

        for (Item item : items) {
            if (item.blocked) {
                blocked++;
            }
        }

        model.addAttribute("type", entityType.name());
        model.addAttribute("target", target);
        model.addAttribute("items", items);
        model.addAttribute("blockedCount", blocked);
        model.addAttribute("okCount", items.size() - blocked);

        return "admin/projects/assignments_plan";
    }

    @PostMapping("/execute")
    public String execute(@RequestParam String type, @RequestParam(required = false) List<Long> ids, @RequestParam Long targetProjectId,
                          RedirectAttributes redirectAttributes)
    {
        EntityType entityType = parseType(type);
        Project target = projectDAO.getProjectForId(targetProjectId);

        if (ids == null || ids.isEmpty() || target == null) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Nothing to move.");
            return "redirect:/admin/projects/assignments?type=" + entityType.name();
        }

        List<Item> items = assignmentService.execute(entityType, ids, target);
        int moved = 0;
        List<String> blocked = new ArrayList<>();

        for (Item item : items) {
            if (item.blocked) {
                blocked.add(item.name + " (" + String.join("; ", item.notes) + ")");
            } else {
                moved++;
            }
        }

        redirectAttributes.addFlashAttribute("resultError", !blocked.isEmpty() && moved == 0);
        redirectAttributes.addFlashAttribute("resultMessage", moved + " moved to " + target.getName()
                + (blocked.isEmpty() ? "." : "; not moved: " + String.join(", ", blocked)));

        return "redirect:/admin/projects/assignments?type=" + entityType.name();
    }

    private EntityType parseType(String type)
    {
        try {
            return EntityType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            return EntityType.WIKI;
        }
    }
}
