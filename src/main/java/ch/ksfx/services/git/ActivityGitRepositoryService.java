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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reads/writes Activity and CodeLib Groovy source from/to this instance's private Git repository,
 * which is the source of truth for that code (see {@link GitSyncConfig}, one row per instance).
 * Inactive (all methods no-op or throw) unless a config row exists and is enabled - callers must
 * check {@link #isActive()} first so non-migrated/non-configured instances keep behaving exactly
 * as before (DB-only).
 */
@Service
public class ActivityGitRepositoryService
{
    public static final String ACTIVITIES_DIRECTORY = "activities";
    public static final String LIBS_DIRECTORY = "libs";
    public static final String REPORTS_DIRECTORY = "reports";
    public static final String REPORT_RESOURCES_DIRECTORY = "report-resources";

    private final GitSyncConfigDAO gitSyncConfigDAO;

    public ActivityGitRepositoryService(GitSyncConfigDAO gitSyncConfigDAO)
    {
        this.gitSyncConfigDAO = gitSyncConfigDAO;
    }

    public boolean isActive()
    {
        GitSyncConfig config = gitSyncConfigDAO.getGitSyncConfig();

        return config != null && config.getEnabled()
                && config.getRepoUrl() != null && !config.getRepoUrl().isEmpty()
                && config.getLocalClonePath() != null && !config.getLocalClonePath().isEmpty();
    }

    @Scheduled(fixedDelay = 120000)
    public void scheduledSync()
    {
        if (!isActive()) {
            return;
        }

        try {
            sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Clones the repo on first use, otherwise fetches and hard-resets the local working copy to
     * origin/&lt;branch&gt; - local edits always go through {@link #writeActivitySource} which
     * commits+pushes immediately, so there is never long-lived local-only history to preserve.
     */
    public synchronized void sync() throws GitAPIException, IOException
    {
        GitSyncConfig config = requireConfig();
        File workingDir = new File(config.getLocalClonePath());

        if (new File(workingDir, ".git").exists()) {
            try (Git git = Git.open(workingDir)) {
                git.fetch().setCredentialsProvider(credentialsProvider(config)).call();
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

    public String readActivitySource(String gitPath) throws IOException
    {
        GitSyncConfig config = requireConfig();
        Path filePath = Paths.get(config.getLocalClonePath(), gitPath);

        return new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
    }

    /**
     * Syncs first (so the write is based on the latest remote state), writes the file, then
     * commits and pushes immediately. On a push rejection (another writer pushed in the
     * meantime) retries once via pull-rebase before pushing again.
     */
    public void writeActivitySource(String gitPath, String content, String commitMessage) throws GitAPIException, IOException
    {
        sync();
        writeFile(gitPath, content);
        commitAndPush(commitMessage);
    }

    /**
     * Renames a file within the working copy, writes the (possibly also-edited) content, then
     * commits and pushes as a single commit - a same-commit delete+add of near/fully-identical
     * content is exactly what git's own rename detection (log --follow, diff -M, GitHub's UI...)
     * looks for, so this is how history survives a rename without needing any special git
     * "rename" API (git doesn't actually have one - renames are always inferred, never recorded).
     */
    public void renameAndWriteActivitySource(String oldGitPath, String newGitPath, String content, String commitMessage) throws GitAPIException, IOException
    {
        sync();
        moveFile(oldGitPath, newGitPath);
        writeFile(newGitPath, content);
        commitAndPush(commitMessage);
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

    /** Writes a file into the local working copy without staging/committing/pushing it. */
    public void writeFile(String gitPath, String content) throws IOException
    {
        GitSyncConfig config = requireConfig();
        Path filePath = Paths.get(config.getLocalClonePath(), gitPath);

        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Stages every pending change in the working copy, commits and pushes. On a push rejection
     * (another writer pushed in the meantime) retries once via pull-rebase before pushing again.
     */
    public void commitAndPush(String commitMessage) throws GitAPIException, IOException
    {
        GitSyncConfig config = requireConfig();

        try (Git git = Git.open(new File(config.getLocalClonePath()))) {
            git.add().addFilepattern(".").call();
            git.add().setUpdate(true).addFilepattern(".").call(); // JGit's plain add() never stages deletions of tracked files
            git.commit().setMessage(commitMessage).call();

            try {
                git.push().setCredentialsProvider(credentialsProvider(config)).call();
            } catch (GitAPIException pushFailure) {
                git.pull().setRebase(true).setCredentialsProvider(credentialsProvider(config)).call();
                git.push().setCredentialsProvider(credentialsProvider(config)).call();
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
    public String uniqueSlug(String baseSlug, String directory)
    {
        GitSyncConfig config = requireConfig();
        String candidate = baseSlug;
        int suffix = 2;

        while (Files.exists(Paths.get(config.getLocalClonePath(), directory, candidate + ".groovy"))) {
            candidate = baseSlug + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    public boolean fileExists(String gitPath)
    {
        GitSyncConfig config = requireConfig();

        return Files.exists(Paths.get(config.getLocalClonePath(), gitPath));
    }

    /** Lists the git-relative paths of every file currently under the given directory in the working copy. */
    public List<String> listFiles(String directory) throws IOException
    {
        GitSyncConfig config = requireConfig();
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
    public void deleteFile(String gitPath) throws IOException
    {
        GitSyncConfig config = requireConfig();

        Files.deleteIfExists(Paths.get(config.getLocalClonePath(), gitPath));
    }

    /**
     * Syncs first, deletes the file, then commits and pushes immediately - the counterpart to
     * {@link #writeActivitySource} for when a KSFX entity itself gets deleted (KSFX is the master
     * for which entries exist, so Git should follow immediately rather than waiting for the next
     * manual reconciliation run).
     */
    public void deleteAndPush(String gitPath, String commitMessage) throws GitAPIException, IOException
    {
        sync();
        deleteFile(gitPath);
        commitAndPush(commitMessage);
    }

    /** Moves a file within the local working copy without staging/committing/pushing it. Does nothing if the source doesn't exist. */
    public void moveFile(String oldGitPath, String newGitPath) throws IOException
    {
        GitSyncConfig config = requireConfig();
        Path oldFile = Paths.get(config.getLocalClonePath(), oldGitPath);
        Path newFile = Paths.get(config.getLocalClonePath(), newGitPath);

        if (!Files.exists(oldFile)) {
            return;
        }

        Files.createDirectories(newFile.getParent());
        Files.move(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Deletes the local working copy entirely and clears the last-synced bookkeeping, so the
     * next {@link #sync()} clones fresh - needed both to let an admin re-test the migration from
     * scratch and to switch repoUrl to a different repository (the existing clone's git remote
     * would otherwise keep pointing at the old one).
     */
    public void deleteLocalClone() throws IOException
    {
        GitSyncConfig config = gitSyncConfigDAO.getGitSyncConfig();

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

    private GitSyncConfig requireConfig()
    {
        GitSyncConfig config = gitSyncConfigDAO.getGitSyncConfig();

        if (config == null || !config.getEnabled()) {
            throw new IllegalStateException("Activity Git repository is not configured/enabled");
        }

        return config;
    }

    private UsernamePasswordCredentialsProvider credentialsProvider(GitSyncConfig config)
    {
        String token = config.getAccessToken() != null ? config.getAccessToken() : "";

        return new UsernamePasswordCredentialsProvider("git", token);
    }
}
