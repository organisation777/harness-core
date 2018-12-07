package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.ABORTED;
import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.common.Constants.DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_EXPIRED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_NEEDED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_STATE_CHANGE_NOTIFICATION;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.BUILD_JOB_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.NOTIFICATION_GROUP_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.PageResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.WorkflowElement;
import software.wings.beans.Notification;
import software.wings.beans.NotificationRule;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.beans.WorkflowType;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 11/3/16.
 */
public class ApprovalStateTest extends WingsBaseTest {
  private static final WorkflowStandardParams WORKFLOW_STANDARD_PARAMS =
      aWorkflowStandardParams().withArtifactIds(asList(ARTIFACT_ID)).build();
  private static final String USER_NAME_1_KEY = "UserName1";

  @Mock private ExecutionContextImpl context;
  @Mock private AlertService alertService;
  @Mock private NotificationService notificationService;
  @Mock private NotificationSetupService notificationSetupService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private NotificationMessageResolver notificationMessageResolver;

  @InjectMocks private ApprovalState approvalState = new ApprovalState("ApprovalState");
  @Captor private ArgumentCaptor<List<NotificationRule>> notificationRuleArgumentCaptor;

  @Before
  public void setUp() throws Exception {
    when(context.getApp()).thenReturn(anApplication().withAccountId(ACCOUNT_ID).withUuid(APP_ID).build());
    when(context.getAppId()).thenReturn(APP_ID);
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(WORKFLOW_STANDARD_PARAMS);
    when(context.getWorkflowExecutionId()).thenReturn(PIPELINE_WORKFLOW_EXECUTION_ID);
    when(context.getWorkflowExecutionName()).thenReturn(BUILD_JOB_NAME);

    when(workflowExecutionService.getWorkflowExecution(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID))
        .thenReturn(WorkflowExecutionBuilder.aWorkflowExecution()
                        .withAppId(APP_ID)
                        .withTriggeredBy(EmbeddedUser.builder().name(USER_NAME).uuid(USER_NAME).build())
                        .withCreatedAt(70L)
                        .build());

    when(notificationSetupService.listDefaultNotificationGroup(any()))
        .thenReturn(asList(aNotificationGroup()
                               .withName(USER_NAME)
                               .withUuid(NOTIFICATION_GROUP_ID)
                               .withAccountId(ACCOUNT_ID)
                               .build()));

    Map<String, String> placeholders = new HashMap<>();
    when(notificationMessageResolver.getPlaceholderValues(
             any(), any(), any(Long.class), any(Long.class), any(), any(), any(), any(), any()))
        .thenReturn(placeholders);
  }

  @Test
  public void shouldExecute() {
    PageResponse pageResponse = new PageResponse();
    pageResponse.setResponse(asList(User.Builder.anUser().build()));
    when(context.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().withExecutionType(WorkflowType.PIPELINE).build());

    ExecutionResponse executionResponse = approvalState.execute(context);
    verify(alertService)
        .openAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), any(ApprovalNeededAlert.class));
    assertThat(executionResponse.isAsync()).isTrue();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(PAUSED);

    verifyNotificationArguments(APPROVAL_NEEDED_NOTIFICATION);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(PAUSED);
  }

  @Test
  public void shouldSkipDisabledStep() {
    PageResponse pageResponse = new PageResponse();
    pageResponse.setResponse(asList(User.Builder.anUser().build()));
    approvalState.setDisable(true);
    ExecutionResponse executionResponse = approvalState.execute(context);
    verify(alertService, times(0))
        .openAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), any(ApprovalNeededAlert.class));
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SKIPPED);
    assertThat(executionResponse.getErrorMessage()).isNotEmpty();
  }

  @Test
  public void shouldGetTimeout() {
    Integer timeoutMillis = approvalState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo(DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS);
  }

  @Test
  public void shouldGetSetTimeout() {
    approvalState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));
    Integer timeoutMillis = approvalState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo((int) (0.6 * TimeUnit.HOURS.toMillis(1)));
  }

  @Test
  public void shouldHandlePipelineAbortWithTimeoutMsg() {
    approvalState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));

    when(context.getStateExecutionInstance())
        .thenReturn(
            aStateExecutionInstance().withExecutionType(WorkflowType.PIPELINE).withUuid(generateUuid()).build());

    when(notificationMessageResolver.getApprovalType(any())).thenReturn(WorkflowType.PIPELINE.name());

    ApprovalStateExecutionData approvalStateExecutionData =
        ApprovalStateExecutionData.builder().approvalId("APPROVAL_ID").build();
    approvalStateExecutionData.setStartTs((long) (System.currentTimeMillis() - (0.6 * TimeUnit.HOURS.toMillis(1))));
    when(context.getStateExecutionData()).thenReturn(approvalStateExecutionData);

    approvalState.handleAbortEvent(context);
    assertThat(context.getStateExecutionData()).isNotNull();
    assertThat(context.getStateExecutionData().getErrorMsg()).contains("Pipeline was not approved within 36m");

    verifyNotificationArguments(APPROVAL_EXPIRED_NOTIFICATION);
  }

  @Test
  public void shouldHandleWorkflowAbortWithTimeoutMsg() {
    approvalState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));

    when(context.getStateExecutionInstance())
        .thenReturn(
            aStateExecutionInstance().withExecutionType(WorkflowType.ORCHESTRATION).withUuid(generateUuid()).build());

    when(notificationMessageResolver.getApprovalType(any())).thenReturn(WorkflowType.ORCHESTRATION.name());

    ApprovalStateExecutionData approvalStateExecutionData =
        ApprovalStateExecutionData.builder().approvalId("APPROVAL_ID").build();
    approvalStateExecutionData.setStartTs((long) (System.currentTimeMillis() - (0.6 * TimeUnit.HOURS.toMillis(1))));
    when(context.getStateExecutionData()).thenReturn(approvalStateExecutionData);
    approvalState.handleAbortEvent(context);
    assertThat(context.getStateExecutionData()).isNotNull();
    assertThat(context.getStateExecutionData().getErrorMsg()).contains("Workflow was not approved within 36m");

    verifyNotificationArguments(APPROVAL_EXPIRED_NOTIFICATION);
  }

  @Test
  public void shouldHandlePipelineAbortWithAbortMsg() {
    approvalState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));

    when(context.getStateExecutionInstance())
        .thenReturn(
            aStateExecutionInstance().withExecutionType(WorkflowType.PIPELINE).withUuid(generateUuid()).build());

    when(notificationMessageResolver.getApprovalType(any())).thenReturn(WorkflowType.PIPELINE.name());

    ApprovalStateExecutionData approvalStateExecutionData =
        ApprovalStateExecutionData.builder().approvalId("APPROVAL_ID").build();
    approvalStateExecutionData.setStartTs(System.currentTimeMillis());

    when(context.getStateExecutionData()).thenReturn(approvalStateExecutionData);
    approvalState.handleAbortEvent(context);
    assertThat(context.getStateExecutionData()).isNotNull();
    assertThat(context.getStateExecutionData().getErrorMsg()).contains("Pipeline was aborted");
    verifyNotificationArguments(APPROVAL_STATE_CHANGE_NOTIFICATION);
  }

  @Test
  public void shouldHandleWorkflowAbortWithAbortMsg() {
    approvalState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));

    when(context.getStateExecutionInstance())
        .thenReturn(
            aStateExecutionInstance().withExecutionType(WorkflowType.ORCHESTRATION).withUuid(generateUuid()).build());

    when(notificationMessageResolver.getApprovalType(any())).thenReturn(WorkflowType.ORCHESTRATION.name());

    ApprovalStateExecutionData approvalStateExecutionData =
        ApprovalStateExecutionData.builder().approvalId("APPROVAL_ID").build();
    approvalStateExecutionData.setStartTs(System.currentTimeMillis());

    when(context.getStateExecutionData()).thenReturn(approvalStateExecutionData);
    approvalState.handleAbortEvent(context);
    assertThat(context.getStateExecutionData()).isNotNull();
    assertThat(context.getStateExecutionData().getErrorMsg()).contains("Workflow was aborted");
    verifyNotificationArguments(APPROVAL_STATE_CHANGE_NOTIFICATION);
  }

  @Test
  public void testGetPlaceholderValues() {
    ApprovalStateExecutionData approvalStateExecutionData = ApprovalStateExecutionData.builder().build();
    approvalStateExecutionData.setStartTs(100L);

    when(context.getStateExecutionData()).thenReturn(approvalStateExecutionData);

    approvalState.getPlaceholderValues(context, USER_NAME_1_KEY, ABORTED);
    verify(notificationMessageResolver)
        .getPlaceholderValues(any(), eq(USER_NAME_1_KEY), eq(100L), any(Long.class), eq(""), eq("aborted"), any(),
            eq(ABORTED), eq(AlertType.ApprovalNeeded));

    approvalState.getPlaceholderValues(context, USER_NAME_1_KEY, PAUSED);
    verify(notificationMessageResolver)
        .getPlaceholderValues(any(), eq(USER_NAME), eq(70L), any(Long.class), eq(""), eq("paused"), any(), eq(PAUSED),
            eq(AlertType.ApprovalNeeded));

    approvalState.getPlaceholderValues(context, USER_NAME_1_KEY, SUCCESS);
    verify(notificationMessageResolver)
        .getPlaceholderValues(any(), eq(USER_NAME_1_KEY), any(Long.class), any(Long.class), eq(""), eq("approved"),
            any(), eq(SUCCESS), eq(AlertType.ApprovalNeeded));
  }

  @Test
  public void testApprovalNeededAlertParamsForWorkflow() {
    when(context.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().withExecutionType(WorkflowType.ORCHESTRATION).build());
    when(context.getEnv()).thenReturn(anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).build());

    approvalState.execute(context);
    ArgumentCaptor<ApprovalNeededAlert> argumentCaptor = ArgumentCaptor.forClass(ApprovalNeededAlert.class);
    verify(alertService).openAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), argumentCaptor.capture());

    ApprovalNeededAlert approvalNeededAlert = argumentCaptor.getValue();
    assertThat(approvalNeededAlert.getEnvId()).isEqualTo(ENV_ID);
    assertThat(approvalNeededAlert.getWorkflowType()).isEqualTo(WorkflowType.ORCHESTRATION);
    assertThat(approvalNeededAlert.getWorkflowExecutionId()).isEqualTo(PIPELINE_WORKFLOW_EXECUTION_ID);
    assertThat(approvalNeededAlert.getPipelineExecutionId()).isEqualTo(null);
  }

  @Test
  public void testApprovalNeededAlertParamsForPipelineWithApproval() {
    when(context.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().withExecutionType(WorkflowType.PIPELINE).build());

    approvalState.execute(context);
    ArgumentCaptor<ApprovalNeededAlert> argumentCaptor = ArgumentCaptor.forClass(ApprovalNeededAlert.class);
    verify(alertService).openAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), argumentCaptor.capture());

    ApprovalNeededAlert approvalNeededAlert = argumentCaptor.getValue();
    assertThat(approvalNeededAlert.getEnvId()).isEqualTo(null);
    assertThat(approvalNeededAlert.getWorkflowType()).isEqualTo(WorkflowType.PIPELINE);
    assertThat(approvalNeededAlert.getWorkflowExecutionId()).isEqualTo(null);
    assertThat(approvalNeededAlert.getPipelineExecutionId()).isEqualTo(PIPELINE_WORKFLOW_EXECUTION_ID);
  }

  @Test
  public void testApprovalNeededAlertParamsForPipelineWithWorkflowApproval() {
    when(context.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().withExecutionType(WorkflowType.ORCHESTRATION).build());
    when(context.getEnv()).thenReturn(anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).build());
    when(context.getContextElement(ContextElementType.STANDARD))
        .thenReturn(
            aWorkflowStandardParams()
                .withWorkflowElement(WorkflowElement.builder().pipelineDeploymentUuid(PIPELINE_EXECUTION_ID).build())
                .build());

    approvalState.execute(context);
    ArgumentCaptor<ApprovalNeededAlert> argumentCaptor = ArgumentCaptor.forClass(ApprovalNeededAlert.class);
    verify(alertService).openAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), argumentCaptor.capture());

    ApprovalNeededAlert approvalNeededAlert = argumentCaptor.getValue();
    assertThat(approvalNeededAlert.getEnvId()).isEqualTo(ENV_ID);
    assertThat(approvalNeededAlert.getWorkflowType()).isEqualTo(WorkflowType.ORCHESTRATION);
    assertThat(approvalNeededAlert.getWorkflowExecutionId()).isEqualTo(PIPELINE_WORKFLOW_EXECUTION_ID);
    assertThat(approvalNeededAlert.getPipelineExecutionId()).isEqualTo(PIPELINE_EXECUTION_ID);
  }

  private void verifyNotificationArguments(NotificationMessageType notificationMessageType) {
    verify(notificationService).sendNotificationAsync(any(Notification.class), singletonList(any()));

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), notificationRuleArgumentCaptor.capture());

    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification.getNotificationTemplateId()).isEqualTo(notificationMessageType.name());
    assertThat(notification.getAccountId()).isEqualTo(ACCOUNT_ID);

    NotificationRule notificationRule = notificationRuleArgumentCaptor.getValue().get(0);
    assertThat(notificationRule.getNotificationGroups().get(0).getName()).isEqualTo(USER_NAME);
    assertThat(notificationRule.getNotificationGroups().get(0).getUuid()).isEqualTo(NOTIFICATION_GROUP_ID);
    assertThat(notificationRule.getNotificationGroups().get(0).getAccountId()).isEqualTo(ACCOUNT_ID);
  }
}
