/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.resourcerestraint.beans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HoldingScopeTest {
  @Test
  @Owner(developers = OwnerRule.FERNANDOD)
  @Category(UnitTests.class)
  public void shouldGetHoldingScopePipeline() {
    assertThat(HoldingScope.PIPELINE).isEqualTo(HoldingScope.getHoldingScope("Pipeline"));
    assertThat(HoldingScope.PIPELINE).isEqualTo(HoldingScope.getHoldingScope("PipeLine"));
    assertThat(HoldingScope.PIPELINE).isEqualTo(HoldingScope.getHoldingScope("PIPELINE"));
  }

  @Test
  @Owner(developers = OwnerRule.FERNANDOD)
  @Category(UnitTests.class)
  public void shouldGetHoldingScopeStage() {
    assertThat(HoldingScope.STAGE).isEqualTo(HoldingScope.getHoldingScope("Stage"));
    assertThat(HoldingScope.STAGE).isEqualTo(HoldingScope.getHoldingScope("StaGE"));
    assertThat(HoldingScope.STAGE).isEqualTo(HoldingScope.getHoldingScope("STAGE"));
  }

  @Test
  @Owner(developers = OwnerRule.FERNANDOD)
  @Category(UnitTests.class)
  public void shouldGetHoldingScopeStepGroup() {
    assertThat(HoldingScope.STEP_GROUP).isEqualTo(HoldingScope.getHoldingScope("StepGroup"));
    assertThat(HoldingScope.STEP_GROUP).isEqualTo(HoldingScope.getHoldingScope("stepGROup"));
    assertThat(HoldingScope.STEP_GROUP).isEqualTo(HoldingScope.getHoldingScope("STEPGROUP"));
  }

  @Test
  @Owner(developers = OwnerRule.FERNANDOD)
  @Category(UnitTests.class)
  public void shouldNotGetHoldingScope() {
    assertThatCode(() -> HoldingScope.getHoldingScope("N/A"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid value: N/A");
    assertThatCode(() -> HoldingScope.getHoldingScope("Pipeline "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid value: Pipeline ");
  }
}
