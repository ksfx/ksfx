/**
 *
 * Copyright (C) 2011-2017 KSFX. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ksfx.controller.admin.gitsync;

import ch.ksfx.dao.GitSyncConfigDAO;
import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.GitSyncConfig;
import ch.ksfx.model.Project;
import ch.ksfx.services.git.ActivityGitRepositoryService;
import ch.ksfx.services.git.GitSyncMigrationService;
import ch.ksfx.services.git.GitSyncReconciliationService;
import ch.ksfx.services.systemlogger.SystemLogger;
import ch.ksfx.util.StacktraceUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

/**
 * A project's Git repository settings and the actions on it (test/sync, migrate-to-Git,
 * unlink, reconcile). One repository per project since the project migration of 2026-10-10 -
 * before that this lived under Admin as the instance-wide "KSFX - GIT Sync" page (that URL now
 * redirects to the project list). The page is reached from the project form.
 */
@Controller
@RequestMapping("/agentic/projects/{projectId}/git")
public class GitSyncConfigController
{
    private final GitSyncConfigDAO gitSyncConfigDAO;
    private final ProjectDAO projectDAO;
    private final ActivityGitRepositoryService activityGitRepositoryService;
    private final GitSyncMigrationService gitSyncMigrationService;
    private final GitSyncReconciliationService gitSyncReconciliationService;
    private final SystemLogger systemLogger;

    public GitSyncConfigController(GitSyncConfigDAO gitSyncConfigDAO,
                                    ProjectDAO projectDAO,
                                    ActivityGitRepositoryService activityGitRepositoryService,
                                    GitSyncMigrationService gitSyncMigrationService,
                                    GitSyncReconciliationService gitSyncReconciliationService,
                                    SystemLogger systemLogger)
    {
        this.gitSyncConfigDAO = gitSyncConfigDAO;
        this.projectDAO = projectDAO;
        this.activityGitRepositoryService = activityGitRepositoryService;
        this.gitSyncMigrationService = gitSyncMigrationService;
        this.gitSyncReconciliationService = gitSyncReconciliationService;
        this.systemLogger = systemLogger;
    }

    @GetMapping("/")
    public String index(@PathVariable Long projectId, Model model)
    {
        Project project = requireProject(projectId);
        GitSyncConfig gitSyncConfig = gitSyncConfigDAO.getGitSyncConfigForProject(projectId);

        if (gitSyncConfig == null) {
            gitSyncConfig = new GitSyncConfig();
            gitSyncConfig.setProject(project);
        }

        model.addAttribute("project", project);
        model.addAttribute("gitSyncConfig", gitSyncConfig);

        return "admin/gitsync/git_sync_config";
    }

    /**
     * The form carries no project field; the row's project is always the one in the URL (an
     * existing row is loaded and updated field by field so nothing but the form's own fields can
     * change - including the project).
     */
    @PostMapping("/")
    public String save(@PathVariable Long projectId, @Valid @ModelAttribute("gitSyncConfig") GitSyncConfig form, BindingResult bindingResult, Model model)
    {
        Project project = requireProject(projectId);

        if (bindingResult.hasErrors()) {
            model.addAttribute("project", project);
            return "admin/gitsync/git_sync_config";
        }

        GitSyncConfig gitSyncConfig = gitSyncConfigDAO.getGitSyncConfigForProject(projectId);

        if (gitSyncConfig == null) {
            gitSyncConfig = new GitSyncConfig();
            gitSyncConfig.setProject(project);
        }

        gitSyncConfig.setRepoUrl(form.getRepoUrl());
        gitSyncConfig.setBranch(form.getBranch());
        gitSyncConfig.setAccessToken(form.getAccessToken());
        gitSyncConfig.setLocalClonePath(form.getLocalClonePath());
        gitSyncConfig.setEnabled(form.getEnabled());
        gitSyncConfigDAO.saveOrUpdateGitSyncConfig(gitSyncConfig);

        return "redirect:/agentic/projects/" + projectId + "/git/";
    }

    @PostMapping("/test")
    public String testConnection(@PathVariable Long projectId, RedirectAttributes redirectAttributes)
    {
        Project project = requireProject(projectId);

        try {
            activityGitRepositoryService.sync(project);
            redirectAttributes.addFlashAttribute("resultMessage", "Verbindung erfolgreich, Repository synchronisiert.");
            systemLogger.logMessage("GITSYNC", "Test connection (" + project.getName() + "): successful.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Verbindung fehlgeschlagen: " + e.getMessage() + StacktraceUtil.getStackTrace(e));
            systemLogger.logMessage("GITSYNC", "Test connection failed (" + project.getName() + ")", e);
        }

        return "redirect:/agentic/projects/" + projectId + "/git/";
    }

    @PostMapping("/migrate")
    public String migrate(@PathVariable Long projectId, RedirectAttributes redirectAttributes)
    {
        Project project = requireProject(projectId);

        try {
            String result = gitSyncMigrationService.migrateAllToGit(project);
            redirectAttributes.addFlashAttribute("resultMessage", result);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Migration fehlgeschlagen: " + e.getMessage() + StacktraceUtil.getStackTrace(e));
            systemLogger.logMessage("GITSYNC", "Migrate failed (" + project.getName() + ")", e);
        }

        return "redirect:/agentic/projects/" + projectId + "/git/";
    }

    @PostMapping("/unlink")
    public String unlink(@PathVariable Long projectId, RedirectAttributes redirectAttributes)
    {
        Project project = requireProject(projectId);

        try {
            String result = gitSyncMigrationService.unlinkAllFromGit(project);
            redirectAttributes.addFlashAttribute("resultMessage", result);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Unlink fehlgeschlagen: " + e.getMessage() + StacktraceUtil.getStackTrace(e));
            systemLogger.logMessage("GITSYNC", "Unlink failed (" + project.getName() + ")", e);
        }

        return "redirect:/agentic/projects/" + projectId + "/git/";
    }

    @PostMapping("/reconcile")
    public String reconcile(@PathVariable Long projectId, RedirectAttributes redirectAttributes)
    {
        Project project = requireProject(projectId);

        try {
            String result = gitSyncReconciliationService.reconcile(project);
            redirectAttributes.addFlashAttribute("resultMessage", result);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Reconciliation fehlgeschlagen: " + e.getMessage() + StacktraceUtil.getStackTrace(e));
            systemLogger.logMessage("GITSYNC", "Reconciliation failed (" + project.getName() + ")", e);
        }

        return "redirect:/agentic/projects/" + projectId + "/git/";
    }

    private Project requireProject(Long projectId)
    {
        Project project = projectDAO.getProjectForId(projectId);

        if (project == null) {
            throw new IllegalArgumentException("No project with id " + projectId);
        }

        return project;
    }
}
