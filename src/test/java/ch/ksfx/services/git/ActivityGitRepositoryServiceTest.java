package ch.ksfx.services.git;

import ch.ksfx.dao.GitSyncConfigDAO;
import ch.ksfx.model.GitSyncConfig;
import java.util.List;
import java.util.Collections;
import ch.ksfx.model.Project;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link ActivityGitRepositoryService} against a local temporary bare repository -
 * no real network access, no Spring context.
 */
public class ActivityGitRepositoryServiceTest
{
    private Path bareRepoDir;
    private Path localCloneDir;
    private ActivityGitRepositoryService service;
    private Project project;

    @BeforeEach
    public void setUp() throws Exception
    {
        bareRepoDir = Files.createTempDirectory("activity-git-bare");
        localCloneDir = Files.createTempDirectory("activity-git-clone");
        Files.delete(localCloneDir);

        try (Git ignored = Git.init().setDirectory(bareRepoDir.toFile()).setBare(true).call()) {
        }

        Path seedDir = Files.createTempDirectory("activity-git-seed");

        try (Git seed = Git.cloneRepository()
                .setURI(bareRepoDir.toUri().toString())
                .setDirectory(seedDir.toFile())
                .call()) {
            Files.write(seedDir.resolve("README.md"), "seed".getBytes(StandardCharsets.UTF_8));
            seed.add().addFilepattern("README.md").call();
            seed.commit().setMessage("seed").call();
            seed.push().call();
        }

        project = new Project();
        project.setId(1L);
        project.setName("Test");

        GitSyncConfig config = new GitSyncConfig();
        config.setProject(project);
        config.setRepoUrl(bareRepoDir.toUri().toString());
        config.setBranch("master");
        config.setLocalClonePath(localCloneDir.toString());
        config.setEnabled(true);

        service = new ActivityGitRepositoryService(new StubGitSyncConfigDAO(config));
    }

    @Test
    public void syncClonesRepositoryOnFirstUse() throws Exception
    {
        service.sync(project);

        assertTrue(new File(localCloneDir.toFile(), ".git").exists());
        assertTrue(new File(localCloneDir.toFile(), "README.md").exists());
    }

    @Test
    public void writeActivitySourceCommitsAndPushesAndReadActivitySourceReadsItBack() throws Exception
    {
        service.sync(project);

        service.writeActivitySource(project, "activities/test-activity.groovy", "class TestActivity {}", "Add test activity");

        assertEquals("class TestActivity {}", service.readActivitySource(project, "activities/test-activity.groovy"));

        // verify the commit actually reached the (bare) remote, not just the local working copy
        Path verifyCloneDir = Files.createTempDirectory("activity-git-verify");

        try (Git ignored = Git.cloneRepository()
                .setURI(bareRepoDir.toUri().toString())
                .setDirectory(verifyCloneDir.toFile())
                .call()) {
            String pushedContent = new String(
                    Files.readAllBytes(verifyCloneDir.resolve("activities/test-activity.groovy")),
                    StandardCharsets.UTF_8);

            assertEquals("class TestActivity {}", pushedContent);
        }
    }

    @Test
    public void uniqueSlugAppendsSuffixOnCollision() throws Exception
    {
        service.sync(project);
        service.writeFile(project, "activities/my-activity.groovy", "class A {}");

        assertEquals("my-activity-2", service.uniqueSlug(project, "my-activity", "activities"));
    }

    @Test
    public void slugifyNormalizesName()
    {
        assertEquals("us-executive-orders", service.slugify("US Executive Orders!"));
    }

    private static class StubGitSyncConfigDAO implements GitSyncConfigDAO
    {
        private GitSyncConfig config;

        StubGitSyncConfigDAO(GitSyncConfig config)
        {
            this.config = config;
        }

        @Override
        public GitSyncConfig getGitSyncConfigForProject(Long projectId)
        {
            return config;
        }

        @Override
        public List<GitSyncConfig> getAllGitSyncConfigs()
        {
            return Collections.singletonList(config);
        }

        @Override
        public void saveOrUpdateGitSyncConfig(GitSyncConfig gitSyncConfig)
        {
            this.config = gitSyncConfig;
        }
    }
}
