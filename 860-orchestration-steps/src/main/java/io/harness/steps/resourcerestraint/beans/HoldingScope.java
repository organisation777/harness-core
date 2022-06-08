/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.resourcerestraint.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.steps.resourcerestraint.ResourceRestraintConstants.YAML_NAME_PIPELINE;
import static io.harness.steps.resourcerestraint.ResourceRestraintConstants.YAML_NAME_PLAN;
import static io.harness.steps.resourcerestraint.ResourceRestraintConstants.YAML_NAME_STAGE;
import static io.harness.steps.resourcerestraint.ResourceRestraintConstants.YAML_NAME_STEP_GROUP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

@OwnedBy(CDC)
@RecasterAlias("io.harness.steps.resourcerestraint.beans.HoldingScope")
public enum HoldingScope {
  // KEEP FOR BACKWARD COMPATIBILITY AND HIDDEN IN JSON
  @JsonProperty(YAML_NAME_PLAN) @JsonIgnore @Deprecated PLAN(YAML_NAME_PLAN),
  @JsonProperty(YAML_NAME_PIPELINE) PIPELINE(YAML_NAME_PIPELINE),
  @JsonProperty(YAML_NAME_STAGE) STAGE(YAML_NAME_STAGE),
  @JsonProperty(YAML_NAME_STEP_GROUP) STEP_GROUP(YAML_NAME_STEP_GROUP);

  /** The name to show in yaml file */
  private final String yamlName;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static HoldingScope getHoldingScope(@JsonProperty("scope") String yamlName) {
    return Arrays.stream(HoldingScope.values())
        .filter(hs -> hs.yamlName.equalsIgnoreCase(yamlName))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid value: " + yamlName));
  }

  HoldingScope(String yamlName) {
    this.yamlName = yamlName;
  }

  @JsonValue
  public String getYamlName() {
    return yamlName;
  }
}
