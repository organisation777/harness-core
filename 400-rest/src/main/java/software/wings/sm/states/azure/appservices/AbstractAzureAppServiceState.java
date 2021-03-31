package software.wings.sm.states.azure.appservices;

import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.TaskType.GIT_FETCH_FILES_TASK;
import static software.wings.delegatetasks.GitFetchFilesTask.GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT;
import static software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupContextElement.SWEEPING_OUTPUT_APP_SERVICE;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MINUTES;

import io.harness.azure.model.AzureConstants;
import io.harness.beans.Cd1SetupFields;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutput;
import io.harness.data.algorithm.HashGenerator;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.ResponseData;

import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AzureWebAppInfrastructureMapping;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.impl.azure.manager.AzureTaskExecutionRequest;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.State;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;
import software.wings.sm.states.azure.AzureSweepingOutputServiceHelper;
import software.wings.sm.states.azure.AzureVMSSStateHelper;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupExecutionData.AzureAppServiceSlotSetupExecutionDataBuilder;
import software.wings.sm.states.azure.appservices.manifest.AzureAppServiceManifestUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public abstract class AbstractAzureAppServiceState extends State {
  @Inject protected DelegateService delegateService;
  @Inject protected AzureVMSSStateHelper azureVMSSStateHelper;
  @Inject protected AzureSweepingOutputServiceHelper azureSweepingOutputServiceHelper;
  @Inject protected AzureAppServiceManifestUtils azureAppServiceManifestUtils;
  @Inject protected ServiceTemplateHelper serviceTemplateHelper;
  @Inject protected ActivityService activityService;
  @Inject private SettingsService settingsService;
  @Inject private GitFileConfigHelperService gitFileConfigHelperService;
  @Inject private GitConfigHelperService gitConfigHelperService;
  @Inject private SecretManager secretManager;

  public AbstractAzureAppServiceState(String name, StateType stateType) {
    super(name, stateType.name());
  }

  @Override
  @SchemaIgnore
  public Integer getTimeoutMillis(ExecutionContext context) {
    return getTimeout(context);
  }

  @NotNull
  protected Integer getTimeout(ExecutionContext context) {
    int timeOut = AzureConstants.DEFAULT_AZURE_VMSS_TIMEOUT_MIN;
    SweepingOutput setupElementFromSweepingOutput =
        azureSweepingOutputServiceHelper.getInfoFromSweepingOutput(context, SWEEPING_OUTPUT_APP_SERVICE);
    if (setupElementFromSweepingOutput != null) {
      AzureAppServiceSlotSetupContextElement setupContextElement =
          (AzureAppServiceSlotSetupContextElement) setupElementFromSweepingOutput;
      Integer appServiceSlotSetupTimeOut = setupContextElement.getAppServiceSlotSetupTimeOut();
      timeOut = appServiceSlotSetupTimeOut != null ? appServiceSlotSetupTimeOut
                                                   : AzureConstants.DEFAULT_AZURE_VMSS_TIMEOUT_MIN;
    }
    return Ints.checkedCast(TimeUnit.MINUTES.toMillis(timeOut));
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    if (!shouldExecute(context)) {
      return ExecutionResponse.builder().executionStatus(SKIPPED).errorMessage(skipMessage()).build();
    }
    Activity activity;
    if (supportRemoteManifest() && !isGitFetchDone(context)) {
      Map<String, ApplicationManifest> appServiceConfigurationRemoteManifests =
          azureAppServiceManifestUtils.getAppServiceConfigurationManifests(context);
      Map<String, ApplicationManifest> remoteManifest =
          azureAppServiceManifestUtils.filterOutRemoteManifest(appServiceConfigurationRemoteManifests);
      if (!isEmpty(remoteManifest)) {
        activity = azureVMSSStateHelper.createAndSaveActivity(
            context, null, getStateType(), commandType(), commandUnitType(), commandUnits(true));
        return executeRemoteGITFetchTask(context, activity, appServiceConfigurationRemoteManifests, remoteManifest);
      }
    }
    activity = azureVMSSStateHelper.createAndSaveActivity(
        context, null, getStateType(), commandType(), commandUnitType(), commandUnits(false));

    AzureAppServiceStateData azureAppServiceStateData = azureVMSSStateHelper.populateAzureAppServiceData(context);
    AzureTaskExecutionRequest executionRequest = buildTaskExecutionRequest(context, azureAppServiceStateData, activity);
    StateExecutionData stateExecutionData =
        createAndEnqueueDelegateTask(activity, context, azureAppServiceStateData, executionRequest);
    return successResponse(activity, stateExecutionData);
  }

  private ExecutionResponse executeRemoteGITFetchTask(ExecutionContext context, Activity activity,
      Map<String, ApplicationManifest> remoteManifests,
      Map<String, ApplicationManifest> appServiceConfigurationManifests) {
    Map<String, GitFetchFilesConfig> filesConfigMap = new HashMap<>();
    remoteManifests.forEach(
        (key, value) -> filesConfigMap.put(key, createGitFetchFilesConfig(value.getGitFileConfig(), context)));

    AzureAppServiceSlotSetupExecutionDataBuilder builder = AzureAppServiceSlotSetupExecutionData.builder();
    builder.taskType(GIT_FETCH_FILES_TASK);
    builder.appServiceConfigurationManifests(appServiceConfigurationManifests);
    GitFetchFilesTaskParams taskParams = GitFetchFilesTaskParams.builder()
                                             .activityId(activity.getUuid())
                                             .accountId(context.getAccountId())
                                             .appId(context.getAppId())
                                             .executionLogName(AzureConstants.FETCH_FILES)
                                             .isFinalState(true)
                                             .gitFetchFilesConfigMap(filesConfigMap)
                                             .containerServiceParams(null)
                                             .isBindTaskFeatureSet(false)
                                             .build();

    DelegateTask delegateTask =
        DelegateTask.builder()
            .uuid(generateUuid())
            .accountId(context.getAccountId())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, context.getAppId())
            .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, context.fetchRequiredEnvironment().getUuid())
            .setupAbstraction(Cd1SetupFields.ENV_TYPE_FIELD, context.getEnvType())
            .data(TaskData.builder()
                      .async(true)
                      .taskType(GIT_FETCH_FILES_TASK.name())
                      .parameters(new Object[] {taskParams})
                      .timeout(TimeUnit.MINUTES.toMillis(GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT))
                      .build())
            .build();
    delegateService.queueTask(delegateTask);
    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(singletonList(delegateTask.getUuid()))
        .stateExecutionData(builder.activityId(activity.getUuid()).build())
        .build();
  }

  private GitFetchFilesConfig createGitFetchFilesConfig(GitFileConfig gitFileConfigRaw, ExecutionContext context) {
    GitFileConfig gitFileConfig = gitFileConfigHelperService.renderGitFileConfig(context, gitFileConfigRaw);
    GitConfig gitConfig = settingsService.fetchGitConfigFromConnectorId(gitFileConfig.getConnectorId());
    notNullCheck("Git config not found", gitConfig);
    gitConfigHelperService.convertToRepoGitConfig(gitConfig, gitFileConfig.getRepoName());
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(gitConfig, context.getAppId(), context.getWorkflowExecutionId());
    return GitFetchFilesConfig.builder()
        .gitConfig(gitConfig)
        .gitFileConfig(gitFileConfig)
        .encryptedDataDetails(encryptionDetails)
        .build();
  }

  private boolean isGitFetchDone(ExecutionContext context) {
    StateExecutionData stateExecutionData = context.getStateExecutionData();
    if (stateExecutionData instanceof AzureAppServiceSlotSetupExecutionData) {
      return ((AzureAppServiceSlotSetupExecutionData) stateExecutionData).isGitFetchDone();
    }
    return false;
  }

  private StateExecutionData createAndEnqueueDelegateTask(Activity activity, ExecutionContext context,
      AzureAppServiceStateData azureAppServiceStateData, AzureTaskExecutionRequest executionRequest) {
    StateExecutionData stateExecutionData = buildPreStateExecutionData(activity, context, azureAppServiceStateData);
    Application application = azureAppServiceStateData.getApplication();
    int expressionFunctorToken = HashGenerator.generateIntegerHash();
    AzureWebAppInfrastructureMapping infrastructureMapping = azureAppServiceStateData.getInfrastructureMapping();
    String serviceTemplateId =
        infrastructureMapping == null ? null : serviceTemplateHelper.fetchServiceTemplateId(infrastructureMapping);

    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(application.getAccountId())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, application.getUuid())
            .waitId(activity.getUuid())
            .data(TaskData.builder()
                      .async(true)
                      .taskType(TaskType.AZURE_APP_SERVICE_TASK.name())
                      .parameters(new Object[] {executionRequest})
                      .timeout(MINUTES.toMillis(getTimeoutMillis(context)))
                      .expressionFunctorToken(expressionFunctorToken)
                      .build())
            .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, azureAppServiceStateData.getEnvironment().getUuid())
            .setupAbstraction(
                Cd1SetupFields.ENV_TYPE_FIELD, azureAppServiceStateData.getEnvironment().getEnvironmentType().name())
            .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD,
                azureAppServiceStateData.getInfrastructureMapping().getUuid())
            .setupAbstraction(
                Cd1SetupFields.SERVICE_ID_FIELD, azureAppServiceStateData.getInfrastructureMapping().getServiceId())
            .setupAbstraction(Cd1SetupFields.SERVICE_TEMPLATE_ID_FIELD, serviceTemplateId)
            .build();
    StateExecutionContext stateExecutionContext = StateExecutionContext.builder()
                                                      .stateExecutionData(stateExecutionData)
                                                      .adoptDelegateDecryption(true)
                                                      .expressionFunctorToken(expressionFunctorToken)
                                                      .build();
    renderDelegateTask(context, delegateTask, stateExecutionContext);

    delegateService.queueTask(delegateTask);
    return stateExecutionData;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    AzureTaskExecutionResponse executionResponse = (AzureTaskExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = azureVMSSStateHelper.getAppServiceExecutionStatus(executionResponse);
    updateActivityStatus(response, context.getAppId(), executionStatus);
    return processDelegateResponse(executionResponse, context, executionStatus);
  }

  protected ExecutionResponse processDelegateResponse(
      AzureTaskExecutionResponse executionResponse, ExecutionContext context, ExecutionStatus executionStatus) {
    return prepareExecutionResponse(executionResponse, context, executionStatus);
  }

  protected ExecutionResponse prepareExecutionResponse(
      AzureTaskExecutionResponse executionResponse, ExecutionContext context, ExecutionStatus executionStatus) {
    if (executionStatus == ExecutionStatus.FAILED) {
      return ExecutionResponse.builder()
          .executionStatus(executionStatus)
          .errorMessage(executionResponse.getErrorMessage())
          .build();
    }
    StateExecutionData stateExecutionData = buildPostStateExecutionData(context, executionResponse, executionStatus);
    emitAnyDataForExternalConsumption(context, executionResponse);
    ContextElement contextElement = buildContextElement(context, executionResponse);
    ExecutionResponseBuilder responseBuilder = ExecutionResponse.builder();
    if (contextElement != null) {
      responseBuilder.contextElement(contextElement);
      responseBuilder.notifyElement(contextElement);
    }
    return responseBuilder.executionStatus(executionStatus)
        .errorMessage(executionResponse.getErrorMessage())
        .stateExecutionData(stateExecutionData)
        .build();
  }

  protected void emitAnyDataForExternalConsumption(
      ExecutionContext context, AzureTaskExecutionResponse executionResponse) {
    log.info(String.format("Nothing to save for external consumption - [%s]", getName()));
  }

  protected boolean verifyIfContextElementExist(ExecutionContext context) {
    SweepingOutput setupElementFromSweepingOutput =
        azureSweepingOutputServiceHelper.getInfoFromSweepingOutput(context, SWEEPING_OUTPUT_APP_SERVICE);
    if (!(setupElementFromSweepingOutput instanceof AzureAppServiceSlotSetupContextElement)) {
      if (isRollback()) {
        return false;
      }
      throw new InvalidRequestException("Did not find Setup element of class AzureAppServiceSlotSetupContextElement");
    }
    return true;
  }

  protected AzureAppServiceSlotSetupContextElement readContextElement(ExecutionContext context) {
    return (AzureAppServiceSlotSetupContextElement) azureSweepingOutputServiceHelper.getInfoFromSweepingOutput(
        context, SWEEPING_OUTPUT_APP_SERVICE);
  }

  protected abstract boolean supportRemoteManifest();

  protected abstract boolean shouldExecute(ExecutionContext context);

  protected abstract AzureTaskExecutionRequest buildTaskExecutionRequest(
      ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData, Activity activity);

  protected abstract StateExecutionData buildPreStateExecutionData(
      Activity activity, ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData);

  protected abstract StateExecutionData buildPostStateExecutionData(
      ExecutionContext context, AzureTaskExecutionResponse executionResponse, ExecutionStatus executionStatus);

  protected ContextElement buildContextElement(ExecutionContext context, AzureTaskExecutionResponse executionResponse) {
    return null;
  }

  protected abstract List<CommandUnit> commandUnits(boolean isGitFetch);

  @NotNull protected abstract CommandUnitType commandUnitType();

  protected abstract String commandType();

  public String skipMessage() {
    return "No Azure App service setup context element found. Skipping current step";
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // Do nothing on abort
  }

  private ExecutionResponse successResponse(Activity activity, StateExecutionData executionData) {
    return ExecutionResponse.builder()
        .async(true)
        .stateExecutionData(executionData)
        .executionStatus(ExecutionStatus.SUCCESS)
        .correlationId(activity.getUuid())
        .build();
  }

  private void updateActivityStatus(Map<String, ResponseData> response, String appId, ExecutionStatus executionStatus) {
    if (response.keySet().iterator().hasNext()) {
      String activityId = response.keySet().iterator().next();
      azureVMSSStateHelper.updateActivityStatus(appId, activityId, executionStatus);
    }
  }
}
