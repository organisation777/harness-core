package io.harness.cdng.creator.plan.envGroup;

import static java.util.Collections.singleton;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.creator.plan.environment.EnvironmentMapper;
import io.harness.cdng.creator.plan.environment.steps.EnvironmentStepV2;
import io.harness.cdng.envGroup.yaml.EnvGroupPlanCreatorConfig;
import io.harness.cdng.environment.steps.EnvironmentStepParameters;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class EnvGroupPlanCreator implements PartialPlanCreator<EnvGroupPlanCreatorConfig> {
  @Inject KryoSerializer kryoSerializer;
  @Override
  public Class<EnvGroupPlanCreatorConfig> getFieldClass() {
    return EnvGroupPlanCreatorConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YamlTypes.ENVIRONMENT_GROUP_YAML, singleton(PlanCreatorUtils.ANY_TYPE));
  }

  @Override
  public PlanCreationResponse createPlanForField(
      PlanCreationContext ctx, EnvGroupPlanCreatorConfig environmentPlanCreatorConfig) {
    EnvironmentStepParameters environmentStepParameters =
        EnvironmentMapper.toEnvironmentStepParameters(environmentPlanCreatorConfig);

    String serviceSpecNodeUuid = (String) kryoSerializer.asInflatedObject(
        ctx.getDependency().getMetadataMap().get(YamlTypes.NEXT_UUID).toByteArray());

    String uuid = (String) kryoSerializer.asInflatedObject(
        ctx.getDependency().getMetadataMap().get(YamlTypes.UUID).toByteArray());

    ByteString advisorParameters = ByteString.copyFrom(
        kryoSerializer.asBytes(OnSuccessAdviserParameters.builder().nextNodeId(serviceSpecNodeUuid).build()));

    return PlanCreationResponse.builder()
        .planNode(
            PlanNode.builder()
                .uuid(uuid)
                .stepType(EnvironmentStepV2.STEP_TYPE)
                .name(PlanCreatorConstants.ENVIRONMENT_NODE_NAME)
                .identifier(YamlTypes.ENVIRONMENT_YAML)
                .stepParameters(environmentStepParameters)
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                        .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(advisorParameters)
                        .build())
                .skipExpressionChain(false)
                .build())
        .build();
  }
}
