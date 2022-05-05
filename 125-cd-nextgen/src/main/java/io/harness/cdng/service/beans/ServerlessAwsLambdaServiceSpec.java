/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.service.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSetWrapper;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSetWrapper;
import io.harness.cdng.service.ServiceSpec;
import io.harness.cdng.variables.beans.NGVariableOverrideSetWrapper;
import io.harness.cdng.visitor.helpers.serviceconfig.ServerlessAwsLambdaServiceSpecVisitorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.k8s.ServiceSpecType;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.VariableExpression;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Value
@Builder
@JsonTypeName(ServiceSpecType.SERVERLESS_AWS_LAMBDA)
@SimpleVisitorHelper(helperClass = ServerlessAwsLambdaServiceSpecVisitorHelper.class)
@TypeAlias("serverlessAwsLambdaServiceSpec")
@RecasterAlias("io.harness.cdng.service.beans.ServerlessAwsLambdaServiceSpec")
public class ServerlessAwsLambdaServiceSpec implements ServiceSpec, Visitable {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  String uuid;
  List<NGVariable> variables;
  ArtifactListConfig artifacts;
  List<ManifestConfigWrapper> manifests;

  @VariableExpression(skipVariableExpression = true) List<NGVariableOverrideSetWrapper> variableOverrideSets;
  @VariableExpression(skipVariableExpression = true) List<ArtifactOverrideSetWrapper> artifactOverrideSets;
  @VariableExpression(skipVariableExpression = true) List<ManifestOverrideSetWrapper> manifestOverrideSets;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public String getType() {
    return ServiceDefinitionType.SERVERLESS_AWS_LAMBDA.getYamlName();
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    if (EmptyPredicate.isNotEmpty(variables)) {
      variables.forEach(ngVariable -> children.add("variables", ngVariable));
    }

    children.add("artifacts", artifacts);
    if (EmptyPredicate.isNotEmpty(artifactOverrideSets)) {
      artifactOverrideSets.forEach(artifactOverrideSet -> children.add("artifactOverrideSets", artifactOverrideSet));
    }
    if (EmptyPredicate.isNotEmpty(manifests)) {
      manifests.forEach(manifest -> children.add("manifests", manifest));
    }
    if (EmptyPredicate.isNotEmpty(manifestOverrideSets)) {
      manifestOverrideSets.forEach(manifestOverrideSet -> children.add("manifestOverrideSets", manifestOverrideSet));
    }
    return children;
  }
}
