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

package ch.ksfx.services.git;

import ch.ksfx.dao.GitSyncConfigDAO;
import ch.ksfx.model.GitSyncConfig;
import ch.ksfx.model.Project;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Reads/writes Activity, CodeLib and report Groovy source from/to a PROJECT's private Git
 * repository, which is the source of truth for that code (see {@link GitSyncConfig}, one row per
 * project since the project migration of 2026-10-10 - before that one row per instance). Every
 * method takes the project whose repository it acts on; callers derive it from the entity at hand
 * ({@code activity.getProject()} etc.). Inactive (all methods no-op or throw) for a project without
 * an enabled config - callers must check {@link #isActive(Project)} first so projects that are not
 * Git-backed keep behaving exactly as before (DB-only). Sync/commit are serialized per project
 * (one lock per project id), different projects' repositories never block each other.
 */
@Service
public class ActivityGitRepositoryService
{
    public static final String ACTIVITIES_DIRECTORY = "activities";
    public static final String LIBS_DIRECTORY = "libs";
    public static final String REPORTS_DIRECTORY = "reports";
    public static final String REPORT_RESOURCES_DIRECTORY = "report-resources";

    private final GitSyncConfigDAO gitSyncConfigDAO;
    private final Map<Long, Object> projectLocks = new ConcurrentHashMap<>();

    public ActivityGitRepositoryService(GitSyncConfigDAO gitSyncConfigDAO)
    {
        this.gitSyncConfigDAO = gitSyncConfigDAO;
    }

    public boolean isActive(Project project)
    {
        if (project == null || project.getId() == null) {
            return false;
        }

        GitSyncConfig config = gitSyncConfigDAO.getGitSyncConfigForProject(project.getId());

        return config != null && config.getEnabled()
                && config.getRepoUrl() != null && !config.getRepoUrl().isEmpty()
                && config.getLocalClonePath() != null && !config.getLocalClonePath().isEmpty();
    }

    /** Any project Git-backed at all - for UI hints that are not about one specific entity. */
    public boolean isAnyActive()
    {
        for (GitSyncConfig config : gitSyncConfigDAO.getAllGitSyncConfigs()) {
            if (config.getEnabled() && config.getRepoUrl() != null && !config.getRepoUrl().isEmpty()
                    && config.getLocalClonePath() != null && !config.getLocalClonePath().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private Object lockFor(Project project)
    {
        return projectLocks.computeIfAbsent(project.getId(), id -> new Object());
    }

    /**
     * Clones the repo on first use, otherwise fetches and hard-resets the local working copy to
     * origin/&lt;branch&gt; - local edits always go through {@link #writeActivitySource} which
     * commits+pushes immediately, so there is never long-lived local-only history to preserve.
     */
    public void sync(Project project) throws GitAPIException, IOException
    {
        synchronized (lockFor(project)) {
            syncLocked(project);
        }
    }

    private void syncLocked(Project project) throws GitAPIException, IOException
    {
        GitSyncConfig config = requireConfig(project);
        File workingDir = new File(config.getLocalClonePath());

        if (new File(workingDir, ".git").exists()) {
            try (Git git = Git.open(workingDir)) {
                git.fetch().setCredentialsProvider(credentialsProvider(config)).call();
                checkoutBranch(git, config);
                git.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/" + config.getBranch()).call();
            }
        } else {
            workingDir.mkdirs();

            try (Git git = Git.cloneRepository()
                    .setURI(config.getRepoUrl())
                    .setBranch(config.getBranch())
                    .setDirectory(workingDir)
                    .setCredentialsProvider(credentialsProvider(config))
                    .call()) {
            }
        }

        try (Git git = Git.open(workingDir)) {
            config.setLastSyncedCommit(git.getRepository().resolve("HEAD").getName());
            config.setLastSyncedAt(new Date());
            gitSyncConfigDAO.saveOrUpdateGitSyncConfig(config);
        }
    }

    /**
     * Switches the working copy onto the configured branch if it's checked out onto a different
     * one - e.g. after the branch is changed in the config UI. Creates a local tracking branch
     * from origin/&lt;branch&gt; if none exists yet, so plain pushes/pulls go to the right place.
     */
    private void checkoutBranch(Git git, GitSyncConfig config) throws GitAPIException, IOException
    {
        String currentBranch = git.getRepository().getBranch();

        if (currentBranch.equals(config.getBranch())) {
            return;
        }

        boolean localBranchExists = git.branchList().call().stream()
                .anyMatch(ref -> ref.getName().equals("refs/heads/" + config.getBranch()));

        if (localBranchExists) {
            git.checkout().setName(config.getBranch()).call();
        } else {
            git.checkout()
                    .setCreateBranch(true)
                    .setName(config.getBranch())
                    .setStartPoint("origin/" + config.getBranch())
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .call();
        }
    }

    /**
     * "Does this entity already have a file in the repo?" - the check every create-vs-rename
     * decision must use. Null AND blank: the MVC edit forms carry gitPath as a hidden field, and
     * Spring binds an empty hidden input as "" rather than null, so an entity created while Git
     * sync was off comes back from its own edit form with gitPath "" - which a null-only check
     * mistook for "has a path", sent it down the rename branch, and {@link #moveFile} then
     * resolved "" to the repo root and tried to move the whole working copy (AccessDenied on
     * Windows, 2026-10-06).
     */
    public static boolean hasGitPath(String gitPath)
    {
        return gitPath != null && !gitPath.trim().isEmpty();
    }

    /**
     * Resolves a repo-relative path to a file inside the working copy, refusing anything that is
     * not strictly a file path below the clone root: blank (would be the root itself), absolute,
     * or escaping via "..". Every file operation below goes through this, so no caller can ever
     * read, write, move or delete the clone root or anything outside it by accident.
     */
    private Path resolveFile(Project project, String gitPath) throws IOException
    {
        if (!hasGitPath(gitPath)) {
            throw new IOException("Git path must not be empty");
        }

        GitSyncConfig config = requireConfig(project);
        Path root = Paths.get(config.getLocalClonePath()).toAbsolutePath().normalize();
        Path file = root.resolve(gitPath.trim()).normalize();

        if (file.equals(root) || !file.startsWith(root)) {
            throw new IOException("Git path must point to a file inside the repository: " + gitPath);
        }

        return file;
    }

    public String readActivitySource(Project project, String gitPath) throws IOException
    {
        Path filePath = resolveFile(project, gitPath);

        return new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
    }

    /**
     * Syncs first (so the write is based on the latest remote state), writes the file, then
     * commits and pushes immediately. On a push rejection (another writer pushed in the
     * meantime) retries once via pull-rebase before pushing again.
     */
    public void writeActivitySource(Project project, String gitPath, String content, String commitMessage) throws GitAPIException, IOException
    {
        synchronized (lockFor(project)) {
            syncLocked(project);
            writeFile(project, gitPath, content);
            commitAndPushLocked(project, commitMessage);
        }
    }

    /**
     * Renames a file within the working copy, writes the (possibly also-edited) content, then
     * commits and pushes as a single commit - a same-commit delete+add of near/fully-identical
     * content is exactly what git's own rename detection (log --follow, diff -M, GitHub's UI...)
     * looks for, so this is how history survives a rename without needing any special git
     * "rename" API (git doesn't actually have one - renames are always inferred, never recorded).
     */
    public void renameAndWriteActivitySource(Project project, String oldGitPath, String newGitPath, String content, String commitMessage) throws GitAPIException, IOException
    {
        synchronized (lockFor(project)) {
            syncLocked(project);
            moveFile(project, oldGitPath, newGitPath);
            writeFile(project, newGitPath, content);
            commitAndPushLocked(project, commitMessage);
        }
    }

    /**
     * Moves an entity's file from one project's repository to another's - the Git side of
     * re-assigning an entity to a different project (ProjectAssignmentService). Two independent
     * repositories, so this is a delete-and-push in the source and a write-and-push in the target,
     * each only if that project is Git-backed; history does not follow across repositories, same as
     * any cross-repo move. Returns the entity's new gitPath: the same path if the target is
     * Git-backed (the slug is kept unless it collides there, then suffixed), or null if it is not -
     * the entity then lives in the DB only until its project gets a repository.
     */
    public String moveToProject(Project source, Project target, String gitPath, String directory, String content, String commitMessage) throws GitAPIException, IOException
    {
        if (hasGitPath(gitPath) && isActive(source)) {
            deleteAndPush(source, gitPath, commitMessage);
        }

        if (!isActive(target) || content == null || content.isEmpty()) {
            return null;
        }

        String baseSlug = hasGitPath(gitPath)
                ? gitPath.substring(gitPath.lastIndexOf('/') + 1).replaceAll("\\.groovy$", "")
                : slugify(directory);
        String slug = uniqueSlug(target, baseSlug, directory);
        String targetPath = directory + "/" + slug + ".groovy";

        writeActivitySource(target, targetPath, content, commitMessage);

        return targetPath;
    }

    /**
     * The path an entity with this name should have, given the paths already taken by its
     * siblings: its current path if that's still a good fit, otherwise the plain slug, or the
     * next free "-2", "-3", ... suffix if the plain slug collides with a sibling's current path.
     */
    public String desiredPath(String directory, String name, String currentGitPath, Set<String> siblingPaths)
    {
        String baseSlug = slugify(name);
        String candidatePath = directory + "/" + baseSlug + ".groovy";

        if (candidatePath.equals(currentGitPath)) {
            return currentGitPath;
        }

        int suffix = 2;

        while (siblingPaths.contains(candidatePath)) {
            candidatePath = directory + "/" + baseSlug + "-" + suffix + ".groovy";
            suffix++;
        }

        return candidatePath;
    }

    /**
     * Writes a file into the local working copy without staging/committing/pushing it.
     *
     * Normalizes line endings to CRLF first - every existing file in this repo (activities, libs,
     * reports) is CRLF, and there's no .gitattributes to enforce that automatically. Callers here
     * (the REST API's JSON bodies, especially) typically hand over LF-only content; writing that
     * as-is would silently flip a CRLF file to LF on its very next edit, making every single line
     * show as changed in the diff even though only one line of actual content moved. Normalizing
     * via replace("\r\n","\n").replace("\n","\r\n") (rather than a single "\n"->"\r\n" replace) is
     * deliberate - content that already arrives CRLF, or mixed, must not end up double-converted
     * ("\r\r\n").
     */
    public void writeFile(Project project, String gitPath, String content) throws IOException
    {
        Path filePath = resolveFile(project, gitPath);
        String normalizedContent = content.replace("\r\n", "\n").replace("\n", "\r\n");

        Files.createDirectories(filePath.getParent());
        Files.write(filePath, normalizedContent.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Stages every pending change in the working copy, commits and pushes. On a push rejection
     * (another writer pushed in the meantime) retries once via pull-rebase before pushing again.
     */
    public void commitAndPush(Project project, String commitMessage) throws GitAPIException, IOException
    {
        synchronized (lockFor(project)) {
            commitAndPushLocked(project, commitMessage);
        }
    }

    private void commitAndPushLocked(Project project, String commitMessage) throws GitAPIException, IOException
    {
        GitSyncConfig config = requireConfig(project);

        try (Git git = Git.open(new File(config.getLocalClonePath()))) {
            git.add().addFilepattern(".").call();
            git.add().setUpdate(true).addFilepattern(".").call(); // JGit's plain add() never stages deletions of tracked files
            git.commit().setMessage(commitMessage).call();

            RefSpec pushSpec = new RefSpec("HEAD:refs/heads/" + config.getBranch());

            try {
                git.push().setRefSpecs(pushSpec).setCredentialsProvider(credentialsProvider(config)).call();
            } catch (GitAPIException pushFailure) {
                git.pull().setRebase(true).setCredentialsProvider(credentialsProvider(config)).call();
                git.push().setRefSpecs(pushSpec).setCredentialsProvider(credentialsProvider(config)).call();
            }

            config.setLastSyncedCommit(git.getRepository().resolve("HEAD").getName());
            config.setLastSyncedAt(new Date());
            gitSyncConfigDAO.saveOrUpdateGitSyncConfig(config);
        }
    }

    /** Lower-cases, strips diacritics and replaces anything non-alphanumeric with "-". */
    public String slugify(String name)
    {
        String withoutDiacritics = Normalizer.normalize(name, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        String slug = withoutDiacritics.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        return slug.isEmpty() ? "activity" : slug;
    }

    /** Appends -2, -3, ... to baseSlug until no file exists yet at &lt;directory&gt;/&lt;slug&gt;.groovy. */
    public String uniqueSlug(Project project, String baseSlug, String directory)
    {
        GitSyncConfig config = requireConfig(project);
        String candidate = baseSlug;
        int suffix = 2;

        while (Files.exists(Paths.get(config.getLocalClonePath(), directory, candidate + ".groovy"))) {
            candidate = baseSlug + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    public boolean fileExists(Project project, String gitPath)
    {
        try {
            return Files.isRegularFile(resolveFile(project, gitPath));
        } catch (IOException e) {
            return false;
        }
    }

    /** Lists the git-relative paths of every file currently under the given directory in the working copy. */
    public List<String> listFiles(Project project, String directory) throws IOException
    {
        GitSyncConfig config = requireConfig(project);
        Path dir = Paths.get(config.getLocalClonePath(), directory);

        if (!Files.exists(dir)) {
            return Collections.emptyList();
        }

        List<String> paths = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile)
                    .forEach(path -> paths.add(directory + "/" + dir.relativize(path).toString().replace(File.separatorChar, '/')));
        }

        return paths;
    }

    /** Deletes a file from the local working copy without staging/committing/pushing it. */
    public void deleteFile(Project project, String gitPath) throws IOException
    {
        Files.deleteIfExists(resolveFile(project, gitPath));
    }

    /**
     * Syncs first, deletes the file, then commits and pushes immediately - the counterpart to
     * {@link #writeActivitySource} for when a KSFX entity itself gets deleted (KSFX is the master
     * for which entries exist, so Git should follow immediately rather than waiting for the next
     * manual reconciliation run).
     */
    public void deleteAndPush(Project project, String gitPath, String commitMessage) throws GitAPIException, IOException
    {
        synchronized (lockFor(project)) {
            syncLocked(project);
            deleteFile(project, gitPath);
            commitAndPushLocked(project, commitMessage);
        }
    }

    /** Moves a file within the local working copy without staging/committing/pushing it. Does nothing if the source doesn't exist. */
    public void moveFile(Project project, String oldGitPath, String newGitPath) throws IOException
    {
        Path oldFile = resolveFile(project, oldGitPath);
        Path newFile = resolveFile(project, newGitPath);

        if (!Files.isRegularFile(oldFile)) {
            return;
        }

        Files.createDirectories(newFile.getParent());
        Files.move(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Deletes the local working copy entirely and clears the last-synced bookkeeping, so the
     * next {@link #sync(Project)} clones fresh - needed both to let an admin re-test the migration from
     * scratch and to switch repoUrl to a different repository (the existing clone's git remote
     * would otherwise keep pointing at the old one).
     */
    public void deleteLocalClone(Project project) throws IOException
    {
        GitSyncConfig config = project != null && project.getId() != null ? gitSyncConfigDAO.getGitSyncConfigForProject(project.getId()) : null;

        if (config == null || config.getLocalClonePath() == null) {
            return;
        }

        Path workingDir = Paths.get(config.getLocalClonePath());

        if (Files.exists(workingDir)) {
            try (Stream<Path> walk = Files.walk(workingDir)) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }

        config.setLastSyncedCommit(null);
        config.setLastSyncedAt(null);
        gitSyncConfigDAO.saveOrUpdateGitSyncConfig(config);
    }

    private GitSyncConfig requireConfig(Project project)
    {
        GitSyncConfig config = project != null && project.getId() != null ? gitSyncConfigDAO.getGitSyncConfigForProject(project.getId()) : null;

        if (config == null || !config.getEnabled()) {
            throw new IllegalStateException("Git repository is not configured/enabled for project " + (project != null ? project.getName() : "?"));
        }

        return config;
    }

    private UsernamePasswordCredentialsProvider credentialsProvider(GitSyncConfig config)
    {
        String token = config.getAccessToken() != null ? config.getAccessToken() : "";

        return new UsernamePasswordCredentialsProvider("git", token);
    }
}
