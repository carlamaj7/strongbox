package org.carlspring.strongbox.providers.repository;

import org.carlspring.strongbox.config.Maven2LayoutProviderCronTasksTestConfig;
import org.carlspring.strongbox.data.CacheManagerTestExecutionListener;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.providers.layout.LayoutProvider;
import org.carlspring.strongbox.providers.layout.Maven2LayoutProvider;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.MutableRepository;
import org.carlspring.strongbox.storage.repository.Repository;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.codehaus.plexus.util.StringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Przemyslaw Fusik
 */
@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "test")
@ContextConfiguration(classes = Maven2LayoutProviderCronTasksTestConfig.class)
@TestExecutionListeners(listeners = { CacheManagerTestExecutionListener.class },
                        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class WhenRepositoryIsAliveCleanExpiredArtifactsTestIT
        extends BaseLocalStorageProxyRepositoryExpiredArtifactsCleanerTest
{
    private static final String REPOSITORY_ID = "maven-central-alive";

    @Test
    public void expiredArtifactsCleanerShouldCleanupDatabaseAndStorage()
            throws Exception
    {
        ArtifactEntry artifactEntry = downloadAndSaveArtifactEntry();

        synchronizeCleanupExpiredArtifacts(true, artifactEntry);

        Optional<ArtifactEntry> artifactEntryOptional = Optional.ofNullable(
                artifactEntryService.findOneArtifact(STORAGE_ID,
                                                     REPOSITORY_ID,
                                                     getPath()));
        assertThat(artifactEntryOptional, CoreMatchers.equalTo(Optional.empty()));

        final Storage storage = getConfiguration().getStorage(artifactEntry.getStorageId());
        final Repository repository = storage.getRepository(artifactEntry.getRepositoryId());
        final LayoutProvider layoutProvider = layoutProviderRegistry.getProvider(repository.getLayout());

        assertFalse(layoutProvider.containsPath(repositoryPathResolver.resolve(repository, getPath())));
        assertTrue(layoutProvider.containsPath(repositoryPathResolver.resolve(repository, StringUtils.replace(getPath(),
                                                                                                              "1.3/maven-commons-1.3.jar",
                                                                                                              "maven-metadata.xml"))));
    }

    private static Set<MutableRepository> getRepositoriesToClean()
    {
        Set<MutableRepository> repositories = new LinkedHashSet<>();
        repositories.add(createRepositoryMock(STORAGE_ID, REPOSITORY_ID, Maven2LayoutProvider.ALIAS));
        return repositories;
    }

    @BeforeAll
    public static void cleanUp()
            throws Exception
    {
        cleanUp(getRepositoriesToClean());
    }

    @BeforeEach
    public void init()
            throws Exception
    {
        createProxyRepository(STORAGE_ID,
                              REPOSITORY_ID,
                              REMOTE_URL);
    }

    @AfterEach
    public void removeRepositories()
            throws Exception
    {
        removeRepositories(getRepositoriesToClean());
    }

    @Override
    protected String getRepositoryId()
    {
        return REPOSITORY_ID;
    }

    @Override
    protected String getPath()
    {
        return "org/carlspring/maven/maven-commons/1.3/maven-commons-1.3.jar";
    }

    @Override
    protected String getVersion()
    {
        return "1.3";
    }
}
