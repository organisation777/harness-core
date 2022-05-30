package io.harness.cdng.creator.plan.envGroup;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.envGroup.services.EnvironmentGroupService;
import io.harness.cdng.envgroup.yaml.EnvironmentGroupYaml;
import io.harness.cdng.environment.yaml.EnvironmentPlanCreatorConfig;
import io.harness.ng.core.environment.services.EnvironmentService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CDP)
@Singleton
public class EnvGroupPlanCreatorHelper {
  @Inject private EnvironmentGroupService environmentGroupService;
  @Inject private EnvironmentService environmentService;

  public EnvironmentPlanCreatorConfig getResolvedEnvRefs(
      PlanCreationContextValue metadata, EnvironmentGroupYaml envGroupYaml, boolean gitOpsEnabled, String serviceRef) {
    String accountIdentifier = metadata.getAccountIdentifier();
    String orgIdentifier = metadata.getOrgIdentifier();
    String projectIdentifier = metadata.getProjectIdentifier();

    return null;
  }
}