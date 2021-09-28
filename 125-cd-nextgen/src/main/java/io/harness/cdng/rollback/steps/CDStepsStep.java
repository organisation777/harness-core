package io.harness.cdng.rollback.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.steps.common.NGSectionStepWithRollbackInfo;

@OwnedBy(HarnessTeam.PIPELINE)
public class CDStepsStep extends NGSectionStepWithRollbackInfo {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.CD_STEPS_STEP.name()).setStepCategory(StepCategory.STEP).build();
}
