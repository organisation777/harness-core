package software.wings.service.impl.workflow;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.RESUMED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.ExecutionScope.WORKFLOW;
import static software.wings.beans.ExecutionScope.WORKFLOW_PHASE;
import static software.wings.beans.FailureNotification.Builder.aFailureNotification;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_NOTIFICATION;
import static software.wings.sm.StateType.PHASE;
import static software.wings.utils.Misc.getDurationString;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.context.ContextElementType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionScope;
import software.wings.beans.InformationNotification;
import software.wings.beans.Notification;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.NotificationRule.NotificationRuleBuilder;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.security.UserGroup;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.PhaseSubWorkflow;

import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Created by anubhaw on 4/7/17.
 */
@Singleton
@Slf4j
public class WorkflowNotificationHelper {
  @Inject private WorkflowService workflowService;
  @Inject private NotificationService notificationService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private MainConfiguration configuration;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private Clock clock;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private UserGroupService userGroupService;

  private final DateFormat dateFormat = new SimpleDateFormat("MMM d");
  private final DateFormat timeFormat = new SimpleDateFormat("HH:mm z");

  public void sendWorkflowStatusChangeNotification(ExecutionContext context, ExecutionStatus status) {
    List<NotificationRule> notificationRules =
        obtainNotificationApplicableToScope((ExecutionContextImpl) context, WORKFLOW, status);
    if (isEmpty(notificationRules)) {
      return;
    }

    Application app = ((ExecutionContextImpl) context).getApp();
    Environment env = ((ExecutionContextImpl) context).getEnv();

    Map<String, String> placeHolderValues = getPlaceholderValues(context, app, env, status, null);

    Notification notification;
    if (status == SUCCESS || status == PAUSED || status == RESUMED) {
      notification = anInformationNotification()
                         .withAccountId(app.getAccountId())
                         .withAppId(context.getAppId())
                         .withEntityId(context.getWorkflowExecutionId())
                         .withEntityType(EntityType.ORCHESTRATED_DEPLOYMENT)
                         .withNotificationTemplateId(WORKFLOW_NOTIFICATION.name())
                         .withNotificationTemplateVariables(placeHolderValues)
                         .build();
    } else {
      notification = aFailureNotification()
                         .withAccountId(app.getAccountId())
                         .withAppId(app.getUuid())
                         .withEnvironmentId(BUILD.equals(context.getOrchestrationWorkflowType()) ? null : env.getUuid())
                         .withEntityId(context.getWorkflowExecutionId())
                         .withEntityType(EntityType.ORCHESTRATED_DEPLOYMENT)
                         .withEntityName("Deployment")
                         .withNotificationTemplateId(WORKFLOW_NOTIFICATION.name())
                         .withNotificationTemplateVariables(placeHolderValues)
                         .withExecutionId(context.getWorkflowExecutionId())
                         .build();
    }

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    if (workflowStandardParams.isNotifyTriggeredUserOnly()) {
      notificationService.sendNotificationToTriggeredByUserOnly(notification, workflowStandardParams.getCurrentUser());
    } else {
      notificationService.sendNotificationAsync(notification, notificationRules);
    }
  }

  public void sendWorkflowPhaseStatusChangeNotification(
      ExecutionContext context, ExecutionStatus status, PhaseSubWorkflow phaseSubWorkflow) {
    // TODO:: use phaseSubworkflow to send rollback notifications

    List<NotificationRule> notificationRules =
        obtainNotificationApplicableToScope((ExecutionContextImpl) context, WORKFLOW_PHASE, status);
    if (isEmpty(notificationRules)) {
      return;
    }

    Environment env = ((ExecutionContextImpl) context).getEnv();
    Application app = ((ExecutionContextImpl) context).getApp();

    Map<String, String> placeHolderValues = getPlaceholderValues(context, app, env, status, phaseSubWorkflow);

    Notification notification;
    if (status.equals(SUCCESS) || status.equals(PAUSED)) {
      notification = anInformationNotification()
                         .withAccountId(app.getAccountId())
                         .withAppId(context.getAppId())
                         .withEntityId(context.getWorkflowExecutionId())
                         .withEntityType(EntityType.ORCHESTRATED_DEPLOYMENT)
                         .withNotificationTemplateId(WORKFLOW_NOTIFICATION.name())
                         .withNotificationTemplateVariables(placeHolderValues)
                         .build();
    } else if (status.equals(FAILED)) {
      notification = aFailureNotification()
                         .withAccountId(app.getAccountId())
                         .withAppId(app.getUuid())
                         .withEnvironmentId(env.getUuid())
                         .withEntityId(context.getWorkflowExecutionId())
                         .withEntityType(EntityType.ORCHESTRATED_DEPLOYMENT)
                         .withEntityName("Deployment")
                         .withNotificationTemplateId(WORKFLOW_NOTIFICATION.name())
                         .withNotificationTemplateVariables(placeHolderValues)
                         .withExecutionId(context.getWorkflowExecutionId())
                         .build();
    } else {
      logger.info("No template found for workflow status " + status);
      return;
    }

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    if (workflowStandardParams.isNotifyTriggeredUserOnly()) {
      notificationService.sendNotificationToTriggeredByUserOnly(notification, workflowStandardParams.getCurrentUser());
    } else {
      notificationService.sendNotificationAsync(notification, notificationRules);
    }
  }

  public void sendApprovalNotification(String accountId, NotificationMessageType notificationMessageType,
      Map<String, String> placeHolderValues, ExecutionContextImpl context) {
    List<NotificationRule> rules = new LinkedList<>();

    Objects.requireNonNull(context, "Context can't be null. accountId=" + accountId);
    Objects.requireNonNull(context.getWorkflowType(), "workflow type can't be null. accountId=" + accountId);

    switch (context.getWorkflowType()) {
      case ORCHESTRATION:
        rules = obtainNotificationApplicableToScope(context, WORKFLOW, PAUSED);
        break;

      case PIPELINE:
        UserGroup defaultUserGroup = userGroupService.getDefaultUserGroup(accountId);
        if (null == defaultUserGroup) {
          logger.error("There is no default user group. accountId={}", accountId);
        } else {
          NotificationRule rule = NotificationRuleBuilder.aNotificationRule()
                                      .withUserGroupIds(Collections.singletonList(defaultUserGroup.getUuid()))
                                      .build();

          rules.add(rule);
        }
        break;

      default:
        throw new IllegalArgumentException("Uknown workflow type: " + context.getWorkflowType());
    }

    InformationNotification notification = anInformationNotification()
                                               .withAppId(GLOBAL_APP_ID)
                                               .withAccountId(accountId)
                                               .withNotificationTemplateId(notificationMessageType.name())
                                               .withNotificationTemplateVariables(placeHolderValues)
                                               .build();

    notificationService.sendNotificationAsync(notification, rules);
  }

  List<NotificationRule> obtainNotificationApplicableToScope(
      ExecutionContextImpl context, ExecutionScope executionScope, ExecutionStatus status) {
    if (ExecutionStatus.isNegativeStatus(status)) {
      status = FAILED;
    } else if (status == RESUMED) {
      status = PAUSED;
    }

    List<NotificationRule> filteredNotificationRules = new ArrayList<>();
    final Workflow workflow = workflowService.readWorkflow(context.getAppId(), context.getWorkflowId());

    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();

    if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
      List<NotificationRule> notificationRules = orchestrationWorkflow.getNotificationRules();
      for (NotificationRule notificationRule : notificationRules) {
        boolean shouldNotify = executionScope.equals(notificationRule.getExecutionScope())
            && notificationRule.getConditions() != null && notificationRule.getConditions().contains(status);

        if (shouldNotify) {
          filteredNotificationRules.add(renderExpressions(context, notificationRule));
        }
      }
    }
    return filteredNotificationRules;
  }

  public NotificationRule renderExpressions(ExecutionContextImpl context, NotificationRule notificationRule) {
    if (notificationRule.isNotificationGroupAsExpression()) {
      renderNotificationGroups(context, notificationRule);
    }

    if (notificationRule.isUserGroupAsExpression()) {
      renderUserGroups(context, notificationRule);
    }

    return notificationRule;
  }

  private void renderNotificationGroups(ExecutionContextImpl context, NotificationRule notificationRule) {
    if (!notificationRule.isNotificationGroupAsExpression()) {
      return;
    }

    List<NotificationGroup> renderedNotificationGroups = new ArrayList<>();
    List<NotificationGroup> notificationGroups = notificationRule.getNotificationGroups();
    for (NotificationGroup notificationGroup : notificationGroups) {
      for (String notificationGroupName : context.renderExpression(notificationGroup.getName()).split(",")) {
        NotificationGroup renderedNotificationGroup = notificationSetupService.readNotificationGroupByName(
            context.getApp().getAccountId(), notificationGroupName.trim());
        if (renderedNotificationGroup != null) {
          renderedNotificationGroups.add(renderedNotificationGroup);
        }
      }
    }
    notificationRule.setNotificationGroups(renderedNotificationGroups);
  }

  private void renderUserGroups(ExecutionContextImpl context, NotificationRule notificationRule) {
    if (!notificationRule.isUserGroupAsExpression()) {
      return;
    }

    String accountId = context.getApp().getAccountId();
    if (StringUtils.isEmpty(accountId)) {
      logger.error("Could not find accountId in context. User Groups can't be rendered. Context: {}", context.asMap());
    }

    String expr = notificationRule.getUserGroupExpression();
    String renderedExpression = context.renderExpression(expr);

    if (StringUtils.isEmpty(renderedExpression)) {
      logger.error("[EMPTY_EXPRESSION] Rendered express is: {}. Original Expression: {}, Context: {}",
          renderedExpression, expr, context.asMap());
      return;
    }

    List<String> userGroupNames =
        Arrays.stream(renderedExpression.split(",")).map(String::trim).collect(Collectors.toList());

    List<UserGroup> userGroups = userGroupService.listByName(accountId, userGroupNames);
    List<String> userGroupIds = userGroups.stream().map(UserGroup::getUuid).collect(Collectors.toList());
    notificationRule.setUserGroupIds(userGroupIds);
  }

  private void logMissingNames(String accountId, List<String> userGroupNames, List<UserGroup> userGroups) {
    Map<String, UserGroup> groupsByName =
        userGroups.stream().collect(Collectors.toMap(UserGroup::getName, (UserGroup ug) -> ug));

    for (String userGroupName : userGroupNames) {
      UserGroup group = groupsByName.get(userGroupName);
      if (null == group) {
        logger.info(
            "[LIKELY_USER_ERROR] No user group by name: {}. This can happen if the expression resolves to a user group name that does not exist. accountId={}",
            userGroupName, accountId);
      }
    }
  }

  private Map<String, String> getPlaceholderValues(ExecutionContext context, Application app, Environment env,
      ExecutionStatus status, @Nullable PhaseSubWorkflow phaseSubWorkflow) {
    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetails(app.getUuid(), context.getWorkflowExecutionId(), true, emptySet());
    String triggeredBy = workflowExecution.getTriggeredBy().getName();
    if (triggeredBy.equalsIgnoreCase("Deployment trigger")) {
      triggeredBy = triggeredBy.toLowerCase();
    }
    long startTs = Optional.ofNullable(workflowExecution.getStartTs()).orElse(workflowExecution.getCreatedAt());
    long endTs = Optional.ofNullable(workflowExecution.getEndTs()).orElse(startTs);

    if (phaseSubWorkflow != null) {
      StateExecutionInstance stateExecutionInstance =
          wingsPersistence.createQuery(StateExecutionInstance.class)
              .filter("executionUuid", workflowExecution.getUuid())
              .filter("stateType", PHASE.name())
              .filter(StateExecutionInstanceKeys.displayName, phaseSubWorkflow.getName())
              .get();
      if (stateExecutionInstance != null) {
        startTs =
            Optional.ofNullable(stateExecutionInstance.getStartTs()).orElse(stateExecutionInstance.getCreatedAt());
        endTs =
            Optional.ofNullable(stateExecutionInstance.getEndTs()).orElse(stateExecutionInstance.getLastUpdatedAt());
      }
    }

    if (endTs == startTs) {
      endTs = clock.millis();
    }

    String workflowUrl = calculateWorkflowUrl(context.getWorkflowExecutionId(), context.getOrchestrationWorkflowType(),
        app.getAccountId(), app.getUuid(), env == null ? null : env.getUuid());

    String pipelineMsg = "";
    if (workflowExecution.getPipelineExecutionId() != null) {
      String pipelineName = workflowExecution.getPipelineSummary().getPipelineName();
      if (isNotBlank(pipelineName)) {
        String pipelineUrl = buildAbsoluteUrl(
            format("/account/%s/app/%s/pipeline-execution/%s/workflow-execution/%s/details", app.getAccountId(),
                app.getUuid(), workflowExecution.getPipelineExecutionId(), context.getWorkflowExecutionId()));
        pipelineMsg = format(" as part of <<<%s|-|%s>>> pipeline", pipelineUrl, pipelineName);
      }
    }

    String startTime = format("%s at %s", dateFormat.format(new Date(startTs)), timeFormat.format(new Date(startTs)));
    String endTime = timeFormat.format(new Date(endTs));

    Map<String, String> placeHolderValues = new HashMap<>();
    placeHolderValues.put("WORKFLOW_NAME", context.getWorkflowExecutionName());
    placeHolderValues.put("WORKFLOW_URL", workflowUrl);
    placeHolderValues.put("VERB", getStatusVerb(status));
    placeHolderValues.put("USER_NAME", triggeredBy);
    placeHolderValues.put("PIPELINE", pipelineMsg);
    placeHolderValues.put("APP_NAME", app.getName());
    placeHolderValues.put("START_TS_SECS", Long.toString(startTs / 1000L));
    placeHolderValues.put("END_TS_SECS", Long.toString(endTs / 1000L));
    placeHolderValues.put("START_DATE", startTime);
    placeHolderValues.put("END_DATE", endTime);
    placeHolderValues.put("DURATION", getDurationString(startTs, endTs));
    placeHolderValues.put(
        "ENV_NAME", BUILD.equals(context.getOrchestrationWorkflowType()) ? "no environment" : env.getName());
    if (phaseSubWorkflow != null) {
      placeHolderValues.put("PHASE_NAME", phaseSubWorkflow.getName() + " of ");
      placeHolderValues.put(
          "ARTIFACTS", getArtifactsMessage(context, workflowExecution, WORKFLOW_PHASE, phaseSubWorkflow));
    } else {
      placeHolderValues.put("PHASE_NAME", "");
      placeHolderValues.put("ARTIFACTS", getArtifactsMessage(context, workflowExecution, WORKFLOW, null));
    }
    return placeHolderValues;
  }

  public String calculateWorkflowUrl(String workflowExecutionId, OrchestrationWorkflowType type, String accountId,
      String appId, String environmentId) {
    return buildAbsoluteUrl(format("/account/%s/app/%s/env/%s/executions/%s/details", accountId, appId,
        BUILD.equals(type) ? "build" : environmentId, workflowExecutionId));
  }

  private String buildAbsoluteUrl(String fragment) {
    String baseUrl = configuration.getPortal().getUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    try {
      URIBuilder uriBuilder = new URIBuilder(baseUrl);
      uriBuilder.setFragment(fragment);
      return uriBuilder.toString();
    } catch (URISyntaxException e) {
      logger.error("Bad URI syntax", e);
      return baseUrl;
    }
  }

  private String getStatusVerb(ExecutionStatus status) {
    switch (status) {
      case SUCCESS:
        return "completed";
      case FAILED:
      case ERROR:
        return "failed";
      case PAUSED:
        return "paused";
      case RESUMED:
        return "resumed";
      case ABORTED:
        return "aborted";
      case REJECTED:
        return "rejected";
      case EXPIRED:
        return "expired";
      default:
        unhandled(status);
        return "failed";
    }
  }

  public String getArtifactsMessage(ExecutionContext context, WorkflowExecution workflowExecution, ExecutionScope scope,
      PhaseSubWorkflow phaseSubWorkflow) {
    List<String> serviceIds = new ArrayList<>();
    if (scope == WORKFLOW_PHASE) {
      serviceIds.add(phaseSubWorkflow.getServiceId());
    } else if (isNotEmpty(workflowExecution.getServiceIds())) {
      serviceIds.addAll(workflowExecution.getServiceIds());
    }

    Map<String, Artifact> serviceIdArtifacts = new HashMap<>();

    List<Artifact> artifacts = ((ExecutionContextImpl) context).getArtifacts();
    if (isNotEmpty(artifacts)) {
      for (Artifact artifact : artifacts) {
        for (String serviceId : artifact.getServiceIds()) {
          serviceIdArtifacts.put(serviceId, artifact);
        }
      }
    }

    List<String> serviceMsgs = new ArrayList<>();
    for (String serviceId : serviceIds) {
      StringBuilder serviceMsg = new StringBuilder();
      Service service = serviceResourceService.get(context.getAppId(), serviceId, false);
      notNullCheck("Service might have been deleted", service, USER);
      serviceMsg.append(service.getName()).append(": ");
      if (serviceIdArtifacts.containsKey(serviceId)) {
        Artifact artifact = serviceIdArtifacts.get(serviceId);
        serviceMsg.append(artifact.getArtifactSourceName())
            .append(" (build# ")
            .append(artifact.getBuildNo().replaceAll("\\*", "٭"))
            .append(')');
      } else {
        serviceMsg.append("no artifact");
      }
      serviceMsgs.add(serviceMsg.toString());
    }

    String artifactsMsg = "no services";
    if (isNotEmpty(serviceMsgs)) {
      artifactsMsg = Joiner.on(", ").join(serviceMsgs);
    }
    return artifactsMsg;
  }
}
