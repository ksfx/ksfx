package ch.ksfx.controller.admin.codelib;

import ch.ksfx.dao.CodeLibDAO;
import ch.ksfx.model.CodeLib;
import ch.ksfx.services.codelib.CodeLibMigrationService;
import ch.ksfx.services.git.ActivityGitRepositoryService;
import ch.ksfx.util.StacktraceUtil;
import groovy.lang.GroovyClassLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.Set;

@Controller
@RequestMapping("/admin/codelib")
public class CodeLibController
{
    private final CodeLibDAO codeLibDAO;
    private final ActivityGitRepositoryService activityGitRepositoryService;
    private final CodeLibMigrationService codeLibMigrationService;

    public CodeLibController(CodeLibDAO codeLibDAO,
                              ActivityGitRepositoryService activityGitRepositoryService,
                              CodeLibMigrationService codeLibMigrationService)
    {
        this.codeLibDAO = codeLibDAO;
        this.activityGitRepositoryService = activityGitRepositoryService;
        this.codeLibMigrationService = codeLibMigrationService;
    }

    @GetMapping("/")
    public String index(Pageable pageable, Model model)
    {
        Page<CodeLib> codeLibsPage = codeLibDAO.getCodeLibsForPageable(pageable);

        model.addAttribute("codeLibsPage", codeLibsPage);

        return "admin/codelib/codelib";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    public String edit(@PathVariable(value = "id", required = false) Long codeLibId, Model model)
    {
        CodeLib codeLib = new CodeLib();

        if (codeLibId != null) {
            codeLib = codeLibDAO.getCodeLibForId(codeLibId);

            if (codeLib.getGitPath() != null && activityGitRepositoryService.isActive()) {
                try {
                    activityGitRepositoryService.sync();
                    codeLib.setGroovyCode(activityGitRepositoryService.readActivitySource(codeLib.getGitPath()));
                } catch (Exception e) {
                    model.addAttribute("gitSyncWarning", "Konnte nicht mit Git synchronisieren, zeige zwischengespeicherten Stand: " + e.getMessage());
                }
            }
        }

        model.addAttribute("codeLib", codeLib);

        return "admin/codelib/codelib_edit";
    }

    @PostMapping({"/edit", "/edit/{id}"})
    public String submit(@PathVariable(value = "id", required = false) Long codeLibId, @Valid @ModelAttribute CodeLib codeLib, BindingResult bindingResult, Model model)
    {
        validate(codeLib, bindingResult);

        if (bindingResult.hasErrors()) {
            return "admin/codelib/codelib_edit";
        }

        if (activityGitRepositoryService.isActive()) {
            try {
                if (codeLib.getGitPath() == null) {
                    String slug = activityGitRepositoryService.uniqueSlug(
                            activityGitRepositoryService.slugify(codeLib.getName()),
                            ActivityGitRepositoryService.LIBS_DIRECTORY);
                    codeLib.setGitPath(ActivityGitRepositoryService.LIBS_DIRECTORY + "/" + slug + ".groovy");
                    activityGitRepositoryService.writeActivitySource(codeLib.getGitPath(), codeLib.getGroovyCode(), "Create code lib: " + codeLib.getName());
                } else {
                    Set<String> siblingPaths = new HashSet<>();
                    for (CodeLib other : codeLibDAO.getAllCodeLibs()) {
                        if (!other.getId().equals(codeLib.getId()) && other.getGitPath() != null) {
                            siblingPaths.add(other.getGitPath());
                        }
                    }

                    String desiredPath = activityGitRepositoryService.desiredPath(ActivityGitRepositoryService.LIBS_DIRECTORY, codeLib.getName(), codeLib.getGitPath(), siblingPaths);

                    if (!desiredPath.equals(codeLib.getGitPath())) {
                        activityGitRepositoryService.renameAndWriteActivitySource(codeLib.getGitPath(), desiredPath, codeLib.getGroovyCode(), "Rename code lib: " + codeLib.getName());
                        codeLib.setGitPath(desiredPath);
                    } else {
                        activityGitRepositoryService.writeActivitySource(codeLib.getGitPath(), codeLib.getGroovyCode(), "Update code lib: " + codeLib.getName());
                    }
                }
            } catch (Exception e) {
                bindingResult.rejectValue("groovyCode", "codeLib.groovyCode", "Konnte nicht ins Git-Repository schreiben: " + e.getMessage() + StacktraceUtil.getStackTrace(e));

                return "admin/codelib/codelib_edit";
            }
        }

        codeLibDAO.saveOrUpdateCodeLib(codeLib);

        return "redirect:/admin/codelib/edit/" + codeLib.getId();
    }

    private void validate(CodeLib codeLib, BindingResult bindingResult)
    {
        try {
            new GroovyClassLoader().parseClass(codeLib.getGroovyCode());
        } catch (Exception e) {
            bindingResult.rejectValue("groovyCode", "codeLib.groovyCode", e.getMessage() + StacktraceUtil.getStackTrace(e));
        }
    }

    @GetMapping({"/delete/{id}"})
    public String delete(@PathVariable(value = "id", required = true) Long codeLibId, RedirectAttributes redirectAttributes)
    {
        CodeLib codeLib = codeLibDAO.getCodeLibForId(codeLibId);

        if (codeLib.getGitPath() != null && activityGitRepositoryService.isActive()) {
            try {
                activityGitRepositoryService.deleteAndPush(codeLib.getGitPath(), "Delete code lib: " + codeLib.getName());
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("resultError", true);
                redirectAttributes.addFlashAttribute("resultMessage", "Konnte Datei nicht aus Git löschen: " + e.getMessage());
            }
        }

        codeLibDAO.deleteCodeLib(codeLib);

        return "redirect:/admin/codelib/";
    }

    @PostMapping("/migratesqlwriter")
    public String migrateSqlWriter(RedirectAttributes redirectAttributes)
    {
        try {
            String result = codeLibMigrationService.migrateSqlWriterFromNoteFile();
            redirectAttributes.addFlashAttribute("resultMessage", result);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "Migration fehlgeschlagen: " + e.getMessage() + StacktraceUtil.getStackTrace(e));
        }

        return "redirect:/admin/codelib/";
    }
}
