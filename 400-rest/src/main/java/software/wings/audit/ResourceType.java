package software.wings.audit;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public enum ResourceType {
  CLOUD_PROVIDER,
  ARTIFACT_SERVER,
  COLLABORATION_PROVIDER,
  VERIFICATION_PROVIDER,
  SOURCE_REPO_PROVIDER,
  LOAD_BALANCER,
  CONNECTION_ATTRIBUTES,
  SETTING,
  APPLICATION,
  SERVICE,
  ENVIRONMENT,
  WORKFLOW,
  PIPELINE,
  ROLE,
  ENCRYPTED_RECORDS,
  PROVISIONER,
  TRIGGER,
  TEMPLATE,
  TEMPLATE_FOLDER,
  USER_GROUP,
  DEPLOYMENT_FREEZE,
  TAG,
  CUSTOM_DASHBOARD,
  SECRET_MANAGER,
  SSO_SETTINGS,
  USER,
  USER_INVITE,
  DELEGATE,
  DELEGATE_SCOPE,
  DELEGATE_PROFILE,
  API_KEY,
  WHITELISTED_IP,
  CE_CONNECTOR
}
