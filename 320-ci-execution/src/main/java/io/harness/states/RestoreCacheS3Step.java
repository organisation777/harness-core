package io.harness.states;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.RestoreCacheS3StepInfo;
import io.harness.pms.contracts.steps.StepType;

@OwnedBy(HarnessTeam.CI)
public class RestoreCacheS3Step extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = RestoreCacheS3StepInfo.STEP_TYPE;
}
