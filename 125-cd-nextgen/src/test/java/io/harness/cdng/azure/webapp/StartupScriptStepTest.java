/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.azure.webapp;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.TMACARI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGTestBase;
import io.harness.cdng.azure.config.StartupScriptOutcome;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.harness.HarnessStore;
import io.harness.cdng.manifest.yaml.harness.HarnessStoreFile;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.exception.InvalidRequestException;
import io.harness.filestore.dto.node.FileNodeDTO;
import io.harness.filestore.dto.node.FileStoreNodeDTO;
import io.harness.filestore.service.FileStoreService;
import io.harness.gitsync.sdk.EntityValidityDetails;
import io.harness.ng.core.api.NGEncryptedDataService;
import io.harness.ng.core.filestore.FileUsage;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDP)
public class StartupScriptStepTest extends CDNGTestBase {
  private static final String FILE_PATH = "file/path";
  private static final String FILE_REFERENCE_WITH_ACCOUNT_SCOPE = "account.fileReference";
  private static final String FILE_REFERENCE = "fileReference";
  private static final String MASTER = "master";
  private static final String COMMIT_ID = "commitId";
  private static final String CONNECTOR_REF = "connectorRef";
  private static final String REPO_NAME = "repoName";
  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";
  private static final String CONNECTOR_NAME = "connectorName";
  private static final String CONFIG_FILE_NAME = "configFileName";
  private static final String CONFIG_FILE_IDENTIFIER = "configFileIdentifier";
  private static final String CONFIG_FILE_PARENT_IDENTIFIER = "configFileParentIdentifier";

  @Mock private ConnectorService connectorService;
  @Mock private FileStoreService fileStoreService;
  @Mock private NGEncryptedDataService ngEncryptedDataService;

  @InjectMocks private StartupScriptStep startupScriptStep;

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetStepParametersClass() {
    assertThat(startupScriptStep.getStepParametersClass()).isEqualTo(StartupScriptParameters.class);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testExecuteSyncHarnessStore() {
    Ambiance ambiance = getAmbiance();
    StoreConfigWrapper storeConfigWrapper = getStoreConfigWrapper();
    when(fileStoreService.get(ACCOUNT_IDENTIFIER, null, null, FILE_REFERENCE, false))
        .thenReturn(Optional.of(getFileStoreNode()));

    StartupScriptParameters stepParameters =
        StartupScriptParameters.builder().startupScript(storeConfigWrapper).build();
    StepResponse response =
        startupScriptStep.executeSync(ambiance, stepParameters, getStepInputPackage(), getPassThroughData());

    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(response.getStepOutcomes()).hasSize(1);

    StepResponse.StepOutcome[] stepOutcomes = response.getStepOutcomes().toArray(new StepResponse.StepOutcome[1]);
    StartupScriptOutcome startupScriptOutcome = (StartupScriptOutcome) stepOutcomes[0].getOutcome();
    assertThat(startupScriptOutcome.getStore()).isEqualTo(storeConfigWrapper.getSpec());

    assertThat(startupScriptOutcome.getStore().getKind()).isEqualTo(StoreConfigType.HARNESS.getDisplayName());
    HarnessStore store = (HarnessStore) startupScriptOutcome.getStore();
    HarnessStoreFile harnessStoreFile = store.getFiles().getValue().get(0);

    assertThat(harnessStoreFile.getPath().getValue()).isEqualTo(FILE_PATH);
    assertThat(harnessStoreFile.getRef().getValue()).isEqualTo(FILE_REFERENCE_WITH_ACCOUNT_SCOPE);
    assertThat(harnessStoreFile.getIsEncrypted().getValue()).isEqualTo(Boolean.FALSE);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testExecuteSyncHarnessStoreNoFilesRefs() {
    Ambiance ambiance = getAmbiance();
    StartupScriptParameters stepParameters =
        StartupScriptParameters.builder()
            .startupScript(StoreConfigWrapper.builder().spec(HarnessStore.builder().build()).build())
            .build();
    assertThatThrownBy(
        () -> startupScriptStep.executeSync(ambiance, stepParameters, getStepInputPackage(), getPassThroughData()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Cannot find any file reference for startup script, store kind:");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testExecuteSyncHarnessStoreMoreThanOneFileProvided() {
    Ambiance ambiance = getAmbiance();
    StartupScriptParameters stepParameters =
        StartupScriptParameters.builder()
            .startupScript(
                StoreConfigWrapper.builder()
                    .spec(HarnessStore.builder()
                              .files(ParameterField.createValueField(Arrays.asList(getHarnessFile(), getHarnessFile())))
                              .build())
                    .build())
            .build();
    assertThatThrownBy(
        () -> startupScriptStep.executeSync(ambiance, stepParameters, getStepInputPackage(), getPassThroughData()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Only one startup script should be provided, store kind:");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testExecuteSyncHarnessStoreFileRefNotFound() {
    Ambiance ambiance = getAmbiance();
    StartupScriptParameters stepParameters =
        StartupScriptParameters.builder()
            .startupScript(StoreConfigWrapper.builder()
                               .spec(HarnessStore.builder()
                                         .files(ParameterField.createValueField(Arrays.asList(
                                             HarnessStoreFile.builder()
                                                 .path(ParameterField.createValueField(FILE_PATH))
                                                 .isEncrypted(ParameterField.createValueField(Boolean.FALSE))
                                                 .build())))
                                         .build())
                               .build())
            .build();
    assertThatThrownBy(
        () -> startupScriptStep.executeSync(ambiance, stepParameters, getStepInputPackage(), getPassThroughData()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("File ref not found for one for startup script, store kind: ");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testExecuteSyncHarnessStoreEncrypted() {
    Ambiance ambiance = getAmbiance();
    StartupScriptParameters stepParameters =
        StartupScriptParameters.builder()
            .startupScript(
                StoreConfigWrapper.builder()
                    .spec(HarnessStore.builder()
                              .files(ParameterField.createValueField(Arrays.asList(
                                  HarnessStoreFile.builder()
                                      .path(ParameterField.createValueField(FILE_PATH))
                                      .ref(ParameterField.createValueField(FILE_REFERENCE_WITH_ACCOUNT_SCOPE))
                                      .isEncrypted(ParameterField.createValueField(Boolean.TRUE))
                                      .build())))
                              .build())
                    .build())
            .build();
    assertThatThrownBy(
        () -> startupScriptStep.executeSync(ambiance, stepParameters, getStepInputPackage(), getPassThroughData()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Startup script file not found in Encrypted Store with ref:");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testExecuteSyncGitStore() {
    Ambiance ambiance = getAmbiance();
    when(connectorService.get(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, CONNECTOR_REF))
        .thenReturn(Optional.of(
            ConnectorResponseDTO.builder()
                .connector(ConnectorInfoDTO.builder().identifier(CONNECTOR_REF).name(CONNECTOR_NAME).build())
                .entityValidityDetails(EntityValidityDetails.builder().valid(true).build())
                .build()));

    StoreConfigWrapper storeConfigWrapper = getStoreConfigWrapperWithGitStore();
    StartupScriptParameters stepParameters =
        StartupScriptParameters.builder().startupScript(storeConfigWrapper).build();

    StepResponse response =
        startupScriptStep.executeSync(ambiance, stepParameters, getStepInputPackage(), getPassThroughData());

    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(response.getStepOutcomes()).hasSize(1);

    StepResponse.StepOutcome[] stepOutcomes = response.getStepOutcomes().toArray(new StepResponse.StepOutcome[1]);
    StartupScriptOutcome startupScriptOutcome = (StartupScriptOutcome) stepOutcomes[0].getOutcome();
    assertThat(startupScriptOutcome.getStore()).isEqualTo(storeConfigWrapper.getSpec());

    assertThat(startupScriptOutcome.getStore().getKind()).isEqualTo(StoreConfigType.GIT.getDisplayName());
    GitStore store = (GitStore) startupScriptOutcome.getStore();
    assertThat(store.getBranch().getValue()).isEqualTo(MASTER);
    assertThat(store.getCommitId().getValue()).isEqualTo(COMMIT_ID);
    assertThat(store.getConnectorRef().getValue()).isEqualTo(CONNECTOR_REF);
    assertThat(store.getRepoName().getValue()).isEqualTo(REPO_NAME);
  }

  private FileStoreNodeDTO getFileStoreNode() {
    return FileNodeDTO.builder()
        .name(CONFIG_FILE_NAME)
        .identifier(CONFIG_FILE_IDENTIFIER)
        .fileUsage(FileUsage.CONFIG)
        .parentIdentifier(CONFIG_FILE_PARENT_IDENTIFIER)
        .build();
  }

  private Ambiance getAmbiance() {
    return Ambiance.newBuilder()
        .putSetupAbstractions(SetupAbstractionKeys.accountId, ACCOUNT_IDENTIFIER)
        .putSetupAbstractions(SetupAbstractionKeys.orgIdentifier, ORG_IDENTIFIER)
        .putSetupAbstractions(SetupAbstractionKeys.projectIdentifier, PROJECT_IDENTIFIER)
        .build();
  }

  private StepInputPackage getStepInputPackage() {
    return StepInputPackage.builder().build();
  }

  private StepExceptionPassThroughData getPassThroughData() {
    return StepExceptionPassThroughData.builder().build();
  }

  private StoreConfigWrapper getStoreConfigWrapper() {
    return StoreConfigWrapper.builder()
        .type(StoreConfigType.HARNESS)
        .spec(HarnessStore.builder().files(getFiles()).build())
        .build();
  }

  private ParameterField<List<HarnessStoreFile>> getFiles() {
    return ParameterField.createValueField(Collections.singletonList(getHarnessFile()));
  }

  private HarnessStoreFile getHarnessFile() {
    return HarnessStoreFile.builder()
        .path(ParameterField.createValueField(FILE_PATH))
        .ref(ParameterField.createValueField(FILE_REFERENCE_WITH_ACCOUNT_SCOPE))
        .isEncrypted(ParameterField.createValueField(Boolean.FALSE))
        .build();
  }

  private StoreConfigWrapper getStoreConfigWrapperWithGitStore() {
    return StoreConfigWrapper.builder()
        .type(StoreConfigType.GIT)
        .spec(GitStore.builder()
                  .branch(ParameterField.createValueField(MASTER))
                  .commitId(ParameterField.createValueField(COMMIT_ID))
                  .connectorRef(ParameterField.createValueField(CONNECTOR_REF))
                  .repoName(ParameterField.createValueField(REPO_NAME))
                  .build())
        .build();
  }
}
