package ch.ksfx.controller.api;

import ch.ksfx.dao.CodeLibDAO;
import ch.ksfx.model.CodeLib;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.services.git.ActivityGitRepositoryService;
import ch.ksfx.services.systemlogger.SystemLogger;
import com.fasterxml.jackson.annotation.JsonInclude;
import groovy.lang.GroovyClassLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * External /api/** CRUDL surface for CodeLibs (the reusable Groovy library scripts under
 * {@link CodeLib}, otherwise managed via /admin/codelib in the UI). Authenticated the same way
 * as {@link ActivityApiController}, via ch.ksfx.services.security.ApiTokenAuthenticationFilter
 * matching all of /api/**.
 *
 * When Git sync is active, this keeps the Git-backed source in lockstep with the DB row exactly
 * like the MVC /admin/codelib/** controller does (same slug/write/rename/delete calls against
 * {@link ActivityGitRepositoryService}) - the API and the GUI must produce the same end state for
 * the same edit, otherwise callers using one and humans using the other silently diverge.
 */
@RestController
@RequestMapping("/api/codelibs")
public class CodeLibApiController
{
    private final CodeLibDAO codeLibDAO;
    private final ServiceProvider serviceProvider;
    private final ActivityGitRepositoryService activityGitRepositoryService;
    private final SystemLogger systemLogger;

    public CodeLibApiController(CodeLibDAO codeLibDAO,
                                 ServiceProvider serviceProvider,
                                 ActivityGitRepositoryService activityGitRepositoryService,
                                 SystemLogger systemLogger)
    {
        this.codeLibDAO = codeLibDAO;
        this.serviceProvider = serviceProvider;
        this.activityGitRepositoryService = activityGitRepositoryService;
        this.systemLogger = systemLogger;
    }

    @GetMapping
    public ResponseEntity<?> list()
    {
        List<CodeLib> codeLibs = codeLibDAO.getAllCodeLibs();

        return ResponseEntity.ok(codeLibs.stream().map(CodeLibApiDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id)
    {
        CodeLib codeLib = codeLibDAO.getCodeLibForId(id);

        if (codeLib == null) {
            return notFound();
        }

        return ResponseEntity.ok(CodeLibApiDto.fromDetailed(codeLib, resolveGroovyCode(codeLib.getGitPath(), codeLib.getGroovyCode())));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CodeLibApiRequest body)
    {
        if (body.name == null || body.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("name is required"));
        }

        String validationError = validate(body.groovyCode);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(errorBody(validationError));
        }

        CodeLib codeLib = new CodeLib();
        codeLib.setName(body.name);
        codeLib.setDescription(body.description);
        codeLib.setGroovyCode(body.groovyCode);

        if (activityGitRepositoryService.isActive() && codeLib.getGroovyCode() != null) {
            try {
                String slug = activityGitRepositoryService.uniqueSlug(
                        activityGitRepositoryService.slugify(codeLib.getName()),
                        ActivityGitRepositoryService.LIBS_DIRECTORY);
                codeLib.setGitPath(ActivityGitRepositoryService.LIBS_DIRECTORY + "/" + slug + ".groovy");
                activityGitRepositoryService.writeActivitySource(codeLib.getGitPath(), codeLib.getGroovyCode(), "Create code lib: " + codeLib.getName());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody("Could not write to Git repository: " + e.getMessage()));
            }
        }

        codeLibDAO.saveOrUpdateCodeLib(codeLib);

        return ResponseEntity.status(HttpStatus.CREATED).body(CodeLibApiDto.from(codeLib));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CodeLibApiRequest body)
    {
        CodeLib codeLib = codeLibDAO.getCodeLibForId(id);

        if (codeLib == null) {
            return notFound();
        }

        if (body.name == null || body.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("name is required"));
        }

        // groovyCode is only validated/overwritten if actually present in the request body - an
        // update that only e.g. changes the description shouldn't have to resend the script, and
        // shouldn't have it wiped either. Mirrors ActivityApiController.update's same choice.
        if (body.groovyCode != null) {
            String validationError = validate(body.groovyCode);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(errorBody(validationError));
            }

            codeLib.setGroovyCode(body.groovyCode);
        }

        codeLib.setName(body.name);
        codeLib.setDescription(body.description);

        if (activityGitRepositoryService.isActive() && codeLib.getGroovyCode() != null) {
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
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody("Could not write to Git repository: " + e.getMessage()));
            }
        }

        codeLibDAO.saveOrUpdateCodeLib(codeLib);

        return ResponseEntity.ok(CodeLibApiDto.from(codeLib));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id)
    {
        CodeLib codeLib = codeLibDAO.getCodeLibForId(id);

        if (codeLib == null) {
            return notFound();
        }

        // Mirrors CodeLibController.delete: a Git failure is logged but never blocks deleting the
        // DB row - an orphaned file in Git is recoverable, a CodeLib the API refuses to delete
        // because Git is unreachable is a worse failure mode for the caller.
        if (codeLib.getGitPath() != null && activityGitRepositoryService.isActive()) {
            try {
                activityGitRepositoryService.deleteAndPush(codeLib.getGitPath(), "Delete code lib: " + codeLib.getName());
            } catch (Exception e) {
                systemLogger.logMessage("WARN", "Could not delete CodeLib '" + codeLib.getName() + "' from Git", e);
            }
        }

        codeLibDAO.deleteCodeLib(codeLib);

        return ResponseEntity.noContent().build();
    }

    /**
     * Same compile-and-instantiate check as ActivityApiController.validate, using the exact
     * constructor convention CodeLibLoaderService.instantiate expects at load time (public
     * constructor taking a single ServiceProvider) - so an invalid CodeLib is rejected here
     * instead of failing later for every caller that tries to load it.
     */
    private String validate(String groovyCode)
    {
        if (groovyCode == null) {
            return null;
        }

        try {
            Class<?> clazz = new GroovyClassLoader().parseClass(groovyCode);
            clazz.getDeclaredConstructor(ServiceProvider.class).newInstance(serviceProvider);
        } catch (Exception e) {
            return "groovyCode does not compile: " + e.getMessage();
        }

        return null;
    }

    /**
     * Mirrors CodeLibController.edit's own git-freshness behaviour: if the CodeLib is Git-backed
     * and sync is active, re-fetch and return the live Git content instead of the possibly-stale
     * DB cache, falling back to the cache on any Git error rather than failing the whole request.
     */
    private String resolveGroovyCode(String gitPath, String cachedGroovyCode)
    {
        if (gitPath == null || !activityGitRepositoryService.isActive()) {
            return cachedGroovyCode;
        }

        try {
            activityGitRepositoryService.sync();
            return activityGitRepositoryService.readActivitySource(gitPath);
        } catch (Exception e) {
            systemLogger.logMessage("WARN", "Could not read Git source for gitPath '" + gitPath + "', falling back to cached groovyCode", e);
            return cachedGroovyCode;
        }
    }

    private ResponseEntity<?> notFound()
    {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("Not found"));
    }

    private Map<String, String> errorBody(String message)
    {
        return Collections.singletonMap("error", message);
    }

    private static class CodeLibApiRequest
    {
        public String name;
        public String description;
        public String groovyCode;
    }

    /**
     * Deliberately not the raw CodeLib entity - avoids serializing its Ebean internals directly.
     * groovyCode is omitted from list/create/update responses (via {@link #from}) since it's not
     * relevant there and can be large; {@link #get} uses {@link #fromDetailed} instead, the only
     * place it's populated (`@JsonInclude(NON_NULL)` keeps it out of the JSON entirely everywhere
     * else, rather than serializing `"groovyCode":null`). Mirrors ActivityApiDto's identical split.
     */
    private static class CodeLibApiDto
    {
        public Long id;
        public String name;
        public String description;
        public String gitPath;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String groovyCode;

        static CodeLibApiDto from(CodeLib codeLib)
        {
            CodeLibApiDto dto = new CodeLibApiDto();
            dto.id = codeLib.getId();
            dto.name = codeLib.getName();
            dto.description = codeLib.getDescription();
            dto.gitPath = codeLib.getGitPath();

            return dto;
        }

        static CodeLibApiDto fromDetailed(CodeLib codeLib, String resolvedGroovyCode)
        {
            CodeLibApiDto dto = from(codeLib);
            dto.groovyCode = resolvedGroovyCode;

            return dto;
        }
    }
}
