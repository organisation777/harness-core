package io.harness.gitsync.common.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.dtos.GitSyncConfigDTO;
import io.harness.gitsync.common.dtos.GitSyncFolderConfigDTO;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

@OwnedBy(DX)
public class YamlGitConfigResourceTest extends GitSyncTestBase {
  @Inject YamlGitConfigResource yamlGitConfigResource;
  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String ORG_ID = "ORG_ID";
  private final String PROJECT_ID = "PROJECT_ID";
  private final String IDENTIFIER = "ID";
  private final String IDENTIFIER_1 = "ID_1";
  private final String CONNECTOR_ID = "CONNECTOR_ID";
  private final String CONNECTOR_ID_1 = "CONNECTOR_ID_1";
  private final String REPO = "REPO";
  private final String BRANCH = "BRANCH";
  private final String ROOT_FOLDER = "ROOT_FOLDER/.harness/";
  private final String ROOT_FOLDER_ID = "ROOT_FOLDER_ID";
  private final String ROOT_FOLDER_1 = "ROOT_FOLDER_1";
  private final String ROOT_FOLDER_ID_1 = "ROOT_FOLDER_ID_1/.harness/";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void test_save() {
    GitSyncFolderConfigDTO rootFolder =
        GitSyncFolderConfigDTO.builder().isDefault(true).rootFolder(ROOT_FOLDER).build();
    GitSyncConfigDTO gitSyncConfigDTO =
        buildGitSyncDTO(Collections.singletonList(rootFolder), CONNECTOR_ID, REPO, BRANCH, IDENTIFIER);
    GitSyncConfigDTO ret = yamlGitConfigResource.create(ACCOUNT_ID, gitSyncConfigDTO);
    assertThat(ret).isEqualTo(gitSyncConfigDTO);
  }

  private GitSyncConfigDTO buildGitSyncDTO(
      List<GitSyncFolderConfigDTO> rootFolder, String connectorId, String repo, String branch, String identifier) {
    return GitSyncConfigDTO.builder()
        .orgIdentifier(ORG_ID)
        .projectIdentifier(PROJECT_ID)
        .repo(repo)
        .branch(branch)
        .gitConnectorRef(connectorId)
        .identifier(identifier)
        .gitSyncFolderConfigDTOs(rootFolder)
        .build();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testConnectorUpdate() {
    GitSyncFolderConfigDTO rootFolder = GitSyncFolderConfigDTO.builder()
                                            .isDefault(true)
                                            .rootFolder(ROOT_FOLDER)
                                            .identifier(ROOT_FOLDER_ID)
                                            .enabled(true)
                                            .build();
    GitSyncConfigDTO gitSyncConfigDTO =
        saveYamlGitConfig(Collections.singletonList(rootFolder), CONNECTOR_ID, REPO, BRANCH, IDENTIFIER);
    gitSyncConfigDTO.setGitConnectorRef(CONNECTOR_ID_1);
    GitSyncConfigDTO ret = yamlGitConfigResource.update(ACCOUNT_ID, gitSyncConfigDTO);
    assertThat(ret.getGitConnectorRef()).isEqualTo(CONNECTOR_ID_1);
  }

  private GitSyncFolderConfigDTO getDefault(GitSyncConfigDTO gitSyncConfigDTOS) {
    List<GitSyncFolderConfigDTO> gitSyncFolderConfigDTOs = gitSyncConfigDTOS.getGitSyncFolderConfigDTOs();
    Optional<GitSyncFolderConfigDTO> defaultGitSync =
        gitSyncFolderConfigDTOs.stream().filter(GitSyncFolderConfigDTO::getIsDefault).findFirst();
    return defaultGitSync.orElse(null);
  }

  private GitSyncConfigDTO saveYamlGitConfig(
      List<GitSyncFolderConfigDTO> rootFolder, String connectorId, String repo, String branch, String identifier) {
    GitSyncConfigDTO gitSyncConfigDTO = buildGitSyncDTO(rootFolder, connectorId, repo, branch, identifier);
    return yamlGitConfigResource.create(ACCOUNT_ID, gitSyncConfigDTO);
  }
}
