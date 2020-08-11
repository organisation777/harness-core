package software.wings.delegatetasks.terraform;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.COMMIT_REFERENCE;
import static software.wings.utils.WingsTestConstants.ENTITY_ID;
import static software.wings.utils.WingsTestConstants.SOURCE_REPO_SETTINGS_ID;
import static software.wings.utils.WingsTestConstants.WORKSPACE;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.FileData;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateFile;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.service.DelegateAgentFileService;
import io.harness.filesystem.FileIo;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.WingsBaseTest;
import software.wings.api.TerraformExecutionData;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.GitConfig;
import software.wings.beans.GitOperationContext;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.TerraformProvisionTask;
import software.wings.service.impl.yaml.GitClientHelper;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;
import software.wings.utils.WingsTestConstants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class TerraformProvisionTaskTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private GitClient gitClient;
  @Mock private DelegateLogService logService;
  @Mock private GitClientHelper gitClientHelper;
  @Mock private DelegateFileManager delegateFileManager;

  private static final String GIT_BRANCH = "test/git_branch";
  private static final String GIT_REPO_DIRECTORY = "repository/terraformTest";

  private GitConfig gitConfig;
  private Map<String, EncryptedDataDetail> encryptedBackendConfigs;
  private EncryptedDataDetail encryptedDataDetail;
  private List<EncryptedDataDetail> sourceRepoEncryptyonDetails;

  TerraformProvisionTask terraformProvisionTask =
      new TerraformProvisionTask(DelegateTaskPackage.builder()
                                     .delegateId(WingsTestConstants.DELEGATE_ID)
                                     .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                     .build(),
          delegateTaskResponse -> {}, () -> true);

  private TerraformProvisionTask terraformProvisionTaskSpy;

  @Before
  public void setUp() throws Exception {
    on(terraformProvisionTask).set("encryptionService", mockEncryptionService);
    on(terraformProvisionTask).set("gitClient", gitClient);
    on(terraformProvisionTask).set("logService", logService);
    on(terraformProvisionTask).set("gitClientHelper", gitClientHelper);
    on(terraformProvisionTask).set("delegateFileManager", delegateFileManager);

    gitConfig = GitConfig.builder().branch(GIT_BRANCH).build();
    gitConfig.setReference(COMMIT_REFERENCE);

    encryptedBackendConfigs = new HashMap<>();
    encryptedDataDetail = EncryptedDataDetail.builder()
                              .encryptedData(EncryptedRecordData.builder().uuid(WingsTestConstants.UUID).build())
                              .build();
    encryptedBackendConfigs.put("var2", encryptedDataDetail);

    sourceRepoEncryptyonDetails = new ArrayList<>();
    sourceRepoEncryptyonDetails.add(EncryptedDataDetail.builder().build());

    doReturn(GIT_REPO_DIRECTORY).when(gitClientHelper).getRepoDirectory(any(GitOperationContext.class));

    terraformProvisionTaskSpy = spy(terraformProvisionTask);

    doReturn(0)
        .when(terraformProvisionTaskSpy)
        .executeShellCommand(
            anyString(), anyString(), any(TerraformProvisionParameters.class), any(LogOutputStream.class));
    doReturn("latestCommit")
        .when(terraformProvisionTaskSpy)
        .getLatestCommitSHAFromLocalRepo(any(GitOperationContext.class));
    doReturn(new ArrayList<String>()).when(terraformProvisionTaskSpy).getWorkspacesList(anyString(), anyLong());
    doReturn(new char[] {'v', 'a', 'l', '2'}).when(mockEncryptionService).getDecryptedValue(encryptedDataDetail);

    when(delegateFileManager.upload(any(DelegateFile.class), any(InputStream.class))).thenReturn(new DelegateFile());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void getTargetsArgsTest() {
    assertThat(terraformProvisionTask.getTargetArgs(null)).isEqualTo("");
    assertThat(terraformProvisionTask.getTargetArgs(Collections.EMPTY_LIST)).isEqualTo("");

    List<String> targets = new ArrayList<>(Arrays.asList("target1", "target2"));

    assertThat(terraformProvisionTask.getTargetArgs(targets)).isEqualTo("-target=target1 -target=target2 ");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testParseOutput() {
    String workspaceCommandOutput = "* w1\n  w2\n w3";
    assertThat(Arrays.asList("w1", "w2", "w3").equals(terraformProvisionTask.parseOutput(workspaceCommandOutput)))
        .isTrue();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGenerateInlineCommandVars() throws Exception {
    doReturn(new char[] {'v', '2'}).when(mockEncryptionService).getDecryptedValue(any());
    TerraformProvisionParameters parameters =
        TerraformProvisionParameters.builder()
            .variables(ImmutableMap.of("k1", "v1"))
            .encryptedVariables(ImmutableMap.of("k2", EncryptedDataDetail.builder().build()))
            .build();
    StringBuilder inlineCommandBuffer = new StringBuilder();
    StringBuilder inlineUILogBuffer = new StringBuilder();
    terraformProvisionTask.getCommandLineVariableParams(parameters, null, inlineCommandBuffer, inlineUILogBuffer);
    String varParams = inlineCommandBuffer.toString();
    String uiLogs = inlineUILogBuffer.toString();
    assertThat(varParams).isEqualTo(" -var='k1=v1'  -var='k2=v2' ");
    assertThat(uiLogs).isEqualTo(" -var='k1=v1'  -var='k2=HarnessSecret:[k2]' ");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testRunApply() throws IOException, TimeoutException, InterruptedException {
    FileIo.createDirectoryIfDoesNotExist(GIT_REPO_DIRECTORY.concat("/scriptPath"));
    FileIo.writeFile(GIT_REPO_DIRECTORY.concat("/scriptPath/backend_configs-ENTITY_ID"), new byte[] {});
    FileIo.writeFile(GIT_REPO_DIRECTORY.concat("/scriptPath/terraform-ENTITY_ID.tfvars"), new byte[] {});

    TerraformProvisionParameters terraformProvisionParameters = createTerraformProvisionParameters(false, false, null,
        TerraformProvisionParameters.TerraformCommandUnit.Apply, TerraformProvisionParameters.TerraformCommand.APPLY);
    TerraformExecutionData terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.APPLY, 1);

    FileIo.deleteDirectoryAndItsContentIfExists(GIT_REPO_DIRECTORY);
    FileIo.deleteDirectoryAndItsContentIfExists("./terraform-working-dir");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testSaveAndGetTerraformPlanFile() throws IOException {
    byte[] planContent = "terraformPlanContent".getBytes();
    String scriptDirectory = "repository/terraformTest";
    String workspacePath = "workspace";
    TerraformProvisionParameters terraformProvisionParameters =
        TerraformProvisionParameters.builder()
            .workspace(workspacePath)
            .terraformPlan(planContent)
            .command(TerraformProvisionParameters.TerraformCommand.APPLY)
            .build();
    terraformProvisionTask.saveTerraformPlanContentToFile(terraformProvisionParameters, scriptDirectory);
    List<FileData> fileDataList = FileIo.getFilesUnderPath(scriptDirectory);
    assertThat(fileDataList.size()).isEqualTo(1);
    assertThat(fileDataList.get(0).getFileBytes()).isEqualTo(planContent);

    byte[] retrievedTerraformPlanContent =
        terraformProvisionTask.getTerraformPlanFile(scriptDirectory, terraformProvisionParameters);
    assertThat(retrievedTerraformPlanContent).isEqualTo(planContent);

    FileIo.deleteDirectoryAndItsContentIfExists(scriptDirectory);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testRunDestroy() throws IOException, TimeoutException, InterruptedException {
    FileIo.createDirectoryIfDoesNotExist(GIT_REPO_DIRECTORY.concat("/scriptPath"));
    FileIo.writeFile(GIT_REPO_DIRECTORY.concat("/scriptPath/backend_configs-ENTITY_ID"), new byte[] {});
    FileIo.writeFile(GIT_REPO_DIRECTORY.concat("/scriptPath/terraform-ENTITY_ID.tfvars"), new byte[] {});

    doReturn(new ByteArrayInputStream(new byte[] {}))
        .when(delegateFileManager)
        .downloadByFileId(any(DelegateAgentFileService.FileBucket.class), anyString(), anyString());

    // regular destroy with no plan exported
    TerraformProvisionParameters terraformProvisionParameters = createTerraformProvisionParameters(false, false, null,
        TerraformProvisionParameters.TerraformCommandUnit.Destroy,
        TerraformProvisionParameters.TerraformCommand.DESTROY);
    TerraformExecutionData terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.DESTROY, 1);

    // use exported plan
    byte[] terraformDestroyPlan = "terraformDestroyPlan".getBytes();
    terraformProvisionParameters = createTerraformProvisionParameters(false, false, terraformDestroyPlan,
        TerraformProvisionParameters.TerraformCommandUnit.Destroy,
        TerraformProvisionParameters.TerraformCommand.DESTROY);
    terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.DESTROY, 2);

    // run destroy plan only
    terraformProvisionParameters =
        createTerraformProvisionParameters(true, true, null, TerraformProvisionParameters.TerraformCommandUnit.Destroy,
            TerraformProvisionParameters.TerraformCommand.DESTROY);
    doReturn(terraformDestroyPlan)
        .when(terraformProvisionTaskSpy)
        .getTerraformPlanFile(anyString(), any(TerraformProvisionParameters.class));
    terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.DESTROY, 3);
    assertThat(terraformExecutionData.getTfPlanFile()).isEqualTo(terraformDestroyPlan);

    FileIo.deleteDirectoryAndItsContentIfExists(GIT_REPO_DIRECTORY);
    FileIo.deleteDirectoryAndItsContentIfExists("./terraform-working-dir");
  }

  private void verify(
      TerraformExecutionData terraformExecutionData, TerraformProvisionParameters.TerraformCommand command, int i) {
    Mockito.verify(mockEncryptionService, times(i)).decrypt(gitConfig, sourceRepoEncryptyonDetails);
    Mockito.verify(gitClient, times(i)).ensureRepoLocallyClonedAndUpdated(any(GitOperationContext.class));
    Mockito.verify(gitClientHelper, times(i)).getRepoDirectory(any(GitOperationContext.class));
    int uploadTimes = TerraformProvisionParameters.TerraformCommand.DESTROY.equals(command) ? i : i + 1;
    Mockito.verify(delegateFileManager, times(uploadTimes)).upload(any(DelegateFile.class), any(InputStream.class));
    assertThat(terraformExecutionData.getWorkspace()).isEqualTo(WORKSPACE);
    assertThat(terraformExecutionData.getEntityId()).isEqualTo(ENTITY_ID);
    assertThat(terraformExecutionData.getPlanLogFileId()).isEqualTo(null);
    assertThat(terraformExecutionData.getCommandExecuted()).isEqualTo(command);
    assertThat(terraformExecutionData.getSourceRepoReference()).isEqualTo("latestCommit");
    assertThat(terraformExecutionData.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  private TerraformProvisionParameters createTerraformProvisionParameters(boolean runPlanOnly,
      boolean exportPlanToApplyStep, byte[] terraformPlan,
      TerraformProvisionParameters.TerraformCommandUnit commandUnit,
      TerraformProvisionParameters.TerraformCommand command) {
    Map<String, String> backendConfigs = new HashMap<>();
    backendConfigs.put("var1", "value1");

    Map<String, String> variables = new HashMap<>();
    variables.put("var3", "val3");

    List<String> tfVarFiles = Arrays.asList("tfVarFile");

    return TerraformProvisionParameters.builder()
        .sourceRepo(gitConfig)
        .sourceRepoSettingId(SOURCE_REPO_SETTINGS_ID)
        .sourceRepoEncryptionDetails(sourceRepoEncryptyonDetails)
        .scriptPath("scriptPath")
        .command(command)
        .commandUnit(commandUnit)
        .accountId(ACCOUNT_ID)
        .workspace(WORKSPACE)
        .entityId(ENTITY_ID)
        .backendConfigs(backendConfigs)
        .encryptedBackendConfigs(encryptedBackendConfigs)
        .terraformPlan(terraformPlan)
        .runPlanOnly(runPlanOnly)
        .exportPlanToApplyStep(exportPlanToApplyStep)
        .variables(variables)
        .tfVarFiles(tfVarFiles)
        .build();
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testSaveAndGetTerraformPlanFileWorkspaceEmpty() throws IOException {
    byte[] planContent = "terraformPlanContent".getBytes();
    String scriptDirectory = "repository/terraformTest";
    TerraformProvisionParameters terraformProvisionParameters =
        TerraformProvisionParameters.builder()
            .terraformPlan(planContent)
            .command(TerraformProvisionParameters.TerraformCommand.APPLY)
            .build();
    terraformProvisionTask.saveTerraformPlanContentToFile(terraformProvisionParameters, scriptDirectory);
    List<FileData> fileDataList = FileIo.getFilesUnderPath(scriptDirectory);
    assertThat(fileDataList.size()).isEqualTo(1);
    assertThat(fileDataList.get(0).getFileBytes()).isEqualTo(planContent);

    TerraformProvisionParameters provisionParameters =
        TerraformProvisionParameters.builder().command(TerraformProvisionParameters.TerraformCommand.APPLY).build();
    byte[] retrievedTerraformPlanContent =
        terraformProvisionTask.getTerraformPlanFile(scriptDirectory, provisionParameters);
    assertThat(retrievedTerraformPlanContent).isEqualTo(planContent);

    FileIo.deleteDirectoryAndItsContentIfExists(scriptDirectory);
  }
}
