package io.harness.cdng.creator.plan.envGroup;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.cdng.envGroup.services.EnvironmentGroupService;
import io.harness.cdng.envGroup.yaml.EnvGroupPlanCreatorConfig;
import io.harness.cdng.envgroup.yaml.EnvironmentGroupYaml;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.YamlUpdates;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.utils.YamlPipelineUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@OwnedBy(CDP)
@Singleton
public class EnvGroupPlanCreatorHelper {
  @Inject private EnvironmentGroupService environmentGroupService;
  @Inject private EnvironmentService environmentService;
  @Inject private KryoSerializer kryoSerializer;

  public EnvGroupPlanCreatorConfig createEnvGroupPlanCreatorConfig(
      PlanCreationContextValue metadata, EnvironmentGroupYaml envGroupYaml) {
    final String accountIdentifier = metadata.getAccountIdentifier();
    final String orgIdentifier = metadata.getOrgIdentifier();
    final String projectIdentifier = metadata.getProjectIdentifier();
    final String envGroupIdentifier = envGroupYaml.getEnvGroupRef().getValue();

    final Optional<EnvironmentGroupEntity> entity =
        environmentGroupService.get(accountIdentifier, orgIdentifier, projectIdentifier, envGroupIdentifier, false);

    if (!entity.isPresent()) {
      throw new InvalidRequestException(
          String.format("No environment group found with %s identifier in %s project in %s org", envGroupIdentifier,
              projectIdentifier, orgIdentifier));
    }

    return EnvGroupPlanCreatorConfig.builder()
        .identifier(entity.get().getIdentifier())
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .environmentGroupRef(envGroupYaml.getEnvGroupRef())
        .deployToAll(envGroupYaml.isDeployToAll())
        .build();
  }

  public void addEnvironmentGroupDependency(LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap,
      EnvGroupPlanCreatorConfig envGroupPlanCreatorConfig, YamlField originalEnvGroupField, boolean gitOpsEnabled,
      String envGroupUuid, String infraSectionUuid, String serviceSpecNodeUuid) throws IOException {
    final YamlField yamlField;
    try {
      String yamlString = YamlPipelineUtils.getYamlString(envGroupPlanCreatorConfig);
      YamlField withUuid = YamlUtils.injectUuidInYamlField(yamlString);
      yamlField = new YamlField(YamlTypes.ENVIRONMENT_YAML,
          new YamlNode(YamlTypes.ENVIRONMENT_YAML, withUuid.getNode().getCurrJsonNode(),
              originalEnvGroupField.getNode().getParentNode()));
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid environment group yaml", e);
    }

    Map<String, YamlField> fieldMap = ImmutableMap.of(envGroupUuid, yamlField);

    // preparing meta data
    final Dependency envDependency = Dependency.newBuilder()
                                         .putAllMetadata(prepareMetadata(envGroupUuid, infraSectionUuid,
                                             serviceSpecNodeUuid, gitOpsEnabled, kryoSerializer))
                                         .build();

    planCreationResponseMap.put(envGroupUuid,
        PlanCreationResponse.builder()
            .dependencies(DependenciesUtils.toDependenciesProto(fieldMap)
                              .toBuilder()
                              .putDependencyMetadata(envGroupUuid, envDependency)
                              .build())
            .yamlUpdates(
                YamlUpdates.newBuilder()
                    .putFqnToYaml(yamlField.getYamlPath(), YamlUtils.writeYamlString(yamlField).replace("---\n", ""))
                    .build())
            .build());
  }

  private Map<String, ByteString> prepareMetadata(String serviceSpecNodeId, String infraSectionUuid,
      String environmentUuid, boolean gitOpsEnabled, KryoSerializer kryoSerializer) {
    return ImmutableMap.<String, ByteString>builder()
        .put(YamlTypes.NEXT_UUID, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(serviceSpecNodeId)))
        .put(YamlTypes.INFRA_SECTION_UUID, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(infraSectionUuid)))
        .put(YamlTypes.UUID, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(environmentUuid)))
        .put(YAMLFieldNameConstants.GITOPS_ENABLED, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(gitOpsEnabled)))
        .build();
  }
}