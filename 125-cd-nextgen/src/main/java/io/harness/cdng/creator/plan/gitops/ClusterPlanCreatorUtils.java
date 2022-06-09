/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.gitops;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.envGroup.yaml.EnvGroupPlanCreatorConfig;
import io.harness.cdng.environment.yaml.EnvironmentPlanCreatorConfig;
import io.harness.cdng.gitops.steps.ClusterStepParameters;
import io.harness.cdng.gitops.steps.GitopsClustersStep;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.yaml.ParameterField;

import java.util.HashSet;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.GITOPS)
@UtilityClass
public class ClusterPlanCreatorUtils {
  public PlanNode.PlanNodeBuilder getGitopsClustersStepPlanNodeBuilder(
      String infraSectionUuid, EnvironmentPlanCreatorConfig envConfig) {
    return PlanNode.builder()
        .uuid(infraSectionUuid)
        .name(PlanCreatorConstants.GITOPS_INFRA_NODE_NAME)
        .identifier(PlanCreatorConstants.GITOPS_INFRA_NODE_NAME)
        .stepType(GitopsClustersStep.STEP_TYPE)
        .stepParameters(getStepParams(envConfig))
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                .build());
  }

  public PlanNode.PlanNodeBuilder getGitopsClustersStepPlanNodeBuilder(
      String infraSectionUuid, EnvGroupPlanCreatorConfig envConfig) {
    return PlanNode.builder()
        .uuid(infraSectionUuid)
        .name(PlanCreatorConstants.GITOPS_INFRA_NODE_NAME)
        .identifier(PlanCreatorConstants.GITOPS_INFRA_NODE_NAME)
        .stepType(GitopsClustersStep.STEP_TYPE)
        .stepParameters(getStepParams(envConfig))
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                .build());
  }

  private ClusterStepParameters getStepParams(EnvironmentPlanCreatorConfig envConfig) {
    checkNotNull(envConfig, "environment must be present");

    final String envRef = fetchEnvRef(envConfig);
    if (envConfig.isDeployToAll()) {
      return ClusterStepParameters.WithEnv(envRef);
    }

    checkArgument(isNotEmpty(envConfig.getGitOpsClusterRefs()),
        "list of gitops clusterRefs must be provided when not deploying to all clusters");

    return ClusterStepParameters.WithEnvAndClusterRefs(envRef, getClusterRefs(envConfig));
  }

  private ClusterStepParameters getStepParams(EnvGroupPlanCreatorConfig config) {
    checkNotNull(config, "environment group must be present");

    final String envGroupRef = fetchEnvGroupRef(config);
    if (config.isDeployToAll()) {
      return ClusterStepParameters.WithEnvGroupRef(envGroupRef);
    }

    checkArgument(isNotEmpty(config.getEnvironmentPlanCreatorConfigs()),
        "list of environments must be provided when not deploying to all clusters");

    return ClusterStepParameters.WithEnvAndClusterRefs(envGroupRef, getClusterRefs(config));
  }

  private Set<String> getClusterRefs(EnvironmentPlanCreatorConfig config) {
    return new HashSet<>(config.getGitOpsClusterRefs());
  }

  private Set<String> getClusterRefs(EnvGroupPlanCreatorConfig config) {
    //    return new HashSet<>(config.getGitOpsClusterRefs());
    return new HashSet<>();
  }

  private String fetchEnvRef(EnvironmentPlanCreatorConfig config) {
    final ParameterField<String> environmentRef = config.getEnvironmentRef();
    checkNotNull(environmentRef, "environment ref must be present");
    checkArgument(!environmentRef.isExpression(), "environment ref not resolved yet");
    return (String) environmentRef.fetchFinalValue();
  }

  private String fetchEnvGroupRef(EnvGroupPlanCreatorConfig config) {
    final ParameterField<String> ref = config.getEnvironmentGroupRef();
    checkNotNull(ref, "environment group ref must be present");
    checkArgument(!ref.isExpression(), "environment group ref not resolved yet");
    return (String) ref.fetchFinalValue();
  }
}
