package ch.ksfx.services.git;

import ch.ksfx.dao.CodeLibDAO;
import ch.ksfx.dao.GitSyncConfigDAO;
import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.publishing.PublishingResourceDAO;
import ch.ksfx.model.GitSyncConfig;
import ch.ksfx.model.activity.Activity;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link GitSyncReconciliationService} against a local temporary bare repository -
 * no real network access, no Spring context. The four entity DAOs are mocked (KSFX's database
 * is the master for entries, so the test only needs to control what the DB "knows").
 */
public class GitSyncReconciliationServiceTest
{
    private Path bareRepoDir;
    private Path localCloneDir;
    private ActivityGitRepositoryService gitService;
    private ActivityDAO activityDAO;
    private CodeLibDAO codeLibDAO;
    private PublishingConfigurationDAO publishingConfigurationDAO;
    private PublishingResourceDAO publishingResourceDAO;
    private GitSyncReconciliationService reconciliationService;

    @BeforeEach
    public void setUp() throws Exception
    {
        bareRepoDir = Files.createTempDirectory("gitsync-reconcile-bare");
        localCloneDir = Files.createTempDirectory("gitsync-reconcile-clone");
        Files.delete(localCloneDir);

        try (Git ignored = Git.init().setDirectory(bareRepoDir.toFile()).setBare(true).call()) {
        }

        Path seedDir = Files.createTempDirectory("gitsync-reconcile-seed");

        try (Git seed = Git.cloneRepository()
                .setURI(bareRepoDir.toUri().toString())
                .setDirectory(seedDir.toFile())
                .call()) {
            Files.createDirectories(seedDir.resolve("activities"));
            Files.write(seedDir.resolve("activities/orphan-activity.groovy"), "class Orphan {}".getBytes(StandardCharsets.UTF_8));
            Files.write(seedDir.resolve("activities/old-name.groovy"), "class Foo {}".getBytes(StandardCharsets.UTF_8));
            seed.add().addFilepattern("activities").call();
            seed.commit().setMessage("seed").call();
            seed.push().call();
        }

        GitSyncConfig config = new GitSyncConfig();
        config.setRepoUrl(bareRepoDir.toUri().toString());
        config.setBranch("master");
        config.setLocalClonePath(localCloneDir.toString());
        config.setEnabled(true);

        GitSyncConfigDAO gitSyncConfigDAO = new StubGitSyncConfigDAO(config);
        gitService = new ActivityGitRepositoryService(gitSyncConfigDAO);

        activityDAO = mock(ActivityDAO.class);
        codeLibDAO = mock(CodeLibDAO.class);
        publishingConfigurationDAO = mock(PublishingConfigurationDAO.class);
        publishingResourceDAO = mock(PublishingResourceDAO.class);

        reconciliationService = new GitSyncReconciliationService(activityDAO, codeLibDAO, publishingConfigurationDAO, publishingResourceDAO, gitService);
    }

    @Test
    public void reconcileDeletesOrphansRenamesRenamedEntitiesAndRecreatesMissingFiles() throws Exception
    {
        Activity renamedActivity = new Activity();
        renamedActivity.setId(1L);
        renamedActivity.setName("New Name");
        renamedActivity.setGitPath("activities/old-name.groovy");
        renamedActivity.setGroovyCode("class Foo {}");

        Activity missingFileActivity = new Activity();
        missingFileActivity.setId(2L);
        missingFileActivity.setName("Missing");
        missingFileActivity.setGitPath("activities/missing.groovy");
        missingFileActivity.setGroovyCode("class Recreated {}");

        when(activityDAO.getAllActivities()).thenReturn(Arrays.asList(renamedActivity, missingFileActivity));

        String result = reconciliationService.reconcile();

        assertEquals("1 Datei(en) gelöscht, 1 umbenannt, 1 wiederhergestellt."
                + " Gelöscht: activities/orphan-activity.groovy."
                + " Umbenannt: activities/old-name.groovy -> activities/new-name.groovy."
                + " Wiederhergestellt: activities/missing.groovy.", result);

        verify(activityDAO).saveOrUpdateActivity(any(Activity.class));
        assertEquals("activities/new-name.groovy", renamedActivity.getGitPath());

        Path verifyCloneDir = Files.createTempDirectory("gitsync-reconcile-verify");

        try (Git ignored = Git.cloneRepository()
                .setURI(bareRepoDir.toUri().toString())
                .setDirectory(verifyCloneDir.toFile())
                .call()) {
            assertFalse(Files.exists(verifyCloneDir.resolve("activities/orphan-activity.groovy")), "orphan file should have been deleted");
            assertFalse(Files.exists(verifyCloneDir.resolve("activities/old-name.groovy")), "old path should no longer exist after rename");

            assertTrue(Files.exists(verifyCloneDir.resolve("activities/new-name.groovy")), "renamed file should exist at the new path");
            assertEquals("class Foo {}", new String(Files.readAllBytes(verifyCloneDir.resolve("activities/new-name.groovy")), StandardCharsets.UTF_8));

            assertTrue(Files.exists(verifyCloneDir.resolve("activities/missing.groovy")), "missing file should have been recreated from the DB cache");
            assertEquals("class Recreated {}", new String(Files.readAllBytes(verifyCloneDir.resolve("activities/missing.groovy")), StandardCharsets.UTF_8));
        }
    }

    private static class StubGitSyncConfigDAO implements GitSyncConfigDAO
    {
        private GitSyncConfig config;

        StubGitSyncConfigDAO(GitSyncConfig config)
        {
            this.config = config;
        }

        @Override
        public GitSyncConfig getGitSyncConfig()
        {
            return config;
        }

        @Override
        public void saveOrUpdateGitSyncConfig(GitSyncConfig gitSyncConfig)
        {
            this.config = gitSyncConfig;
        }
    }
}
