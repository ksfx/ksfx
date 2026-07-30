package ch.ksfx.controller.admin.gitsync;

import ch.ksfx.dao.GitSyncConfigDAO;
import ch.ksfx.model.GitSyncConfig;
import ch.ksfx.services.git.ActivityGitRepositoryService;
import ch.ksfx.services.git.GitSyncMigrationService;
import ch.ksfx.services.git.GitSyncReconciliationService;
import ch.ksfx.services.systemlogger.SystemLogger;
import ch.ksfx.util.StacktraceUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequestMapping("/admin/gitsync")
public class GitSyncConfigController
{
    private final GitSyncConfigDAO gitSyncConfigDAO;
    private final ActivityGitRepositoryService activityGitRepositoryService;
    private final GitSyncMigrationService gitSyncMigrationService;
    private final GitSyncReconciliationService gitSyncReconciliationService;
    private final SystemLogger systemLogger;

    public GitSyncConfigController(GitSyncConfigDAO gitSyncConfigDAO,
                                    ActivityGitRepositoryService activityGitRepositoryService,
                                    GitSyncMigrationService gitSyncMigrationService,
                                    GitSyncReconciliationService gitSyncReconciliationService,
                                    SystemLogger systemLogger)
    {
        this.gitSyncConfigDAO = gitSyncConfigDAO;
        this.activityGitRepositoryService = activityGitRepositoryService;
        this.gitSyncMigrationService = gitSyncMigrationService;
        this.gitSyncReconciliationService = gitSyncReconciliationService;
        this.systemLogger = systemLogger;
    }

    @GetMapping("/")
    public String index(Model model)
    {
        GitSyncConfig gitSyncConfig = gitSyncConfigDAO.getGitSyncConfig();

        if (gitSyncConfig == null) {
            gitSyncConfig = new GitSyncConfig();
        }

        model.addAttribute("gitSyncConfig", gitSyncConfig);

        return "admin/gitsync/git_sync_config";
    }

    @PostMapping("/")
    public String save(@Valid @ModelAttribute("gitSyncConfig") GitSyncConfig gitSyncConfig, BindingResult bindingResult)
    {
        if (bindingResult.hasErrors()) {
            return "admin/gitsync/git_sync_config";
        }

        gitSyncConfigDAO.saveOrUpdateGitSyncConfig(gitSyncConfig);

        return "redirect:/admin/gitsync/";
    }

    @PostMapping("/test")
    public String testConnection(RedirectAttributes redirectAttributes)
    {
        try {
            activityGitRepositoryService.sync();
            redirectAttributes.addFlashAttribute("resultMessage", "Verbindung erfolgreich, Repository synchronisiert.");
            systemLogger.logMessage("GITSYNC", "Test connection: successful.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Verbindung fehlgeschlagen: " + e.getMessage() + StacktraceUtil.getStackTrace(e));
            systemLogger.logMessage("GITSYNC", "Test connection failed", e);
        }

        return "redirect:/admin/gitsync/";
    }

    @PostMapping("/migrate")
    public String migrate(RedirectAttributes redirectAttributes)
    {
        try {
            String result = gitSyncMigrationService.migrateAllToGit();
            redirectAttributes.addFlashAttribute("resultMessage", result);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Migration fehlgeschlagen: " + e.getMessage() + StacktraceUtil.getStackTrace(e));
            systemLogger.logMessage("GITSYNC", "Migrate failed", e);
        }

        return "redirect:/admin/gitsync/";
    }

    @PostMapping("/unlink")
    public String unlink(RedirectAttributes redirectAttributes)
    {
        try {
            String result = gitSyncMigrationService.unlinkAllFromGit();
            redirectAttributes.addFlashAttribute("resultMessage", result);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Unlink fehlgeschlagen: " + e.getMessage() + StacktraceUtil.getStackTrace(e));
            systemLogger.logMessage("GITSYNC", "Unlink failed", e);
        }

        return "redirect:/admin/gitsync/";
    }

    @PostMapping("/reconcile")
    public String reconcile(RedirectAttributes redirectAttributes)
    {
        try {
            String result = gitSyncReconciliationService.reconcile();
            redirectAttributes.addFlashAttribute("resultMessage", result);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Reconciliation fehlgeschlagen: " + e.getMessage() + StacktraceUtil.getStackTrace(e));
            systemLogger.logMessage("GITSYNC", "Reconciliation failed", e);
        }

        return "redirect:/admin/gitsync/";
    }
}
