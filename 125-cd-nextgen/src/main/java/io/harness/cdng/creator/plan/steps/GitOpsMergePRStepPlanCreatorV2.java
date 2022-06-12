package io.harness.cdng.creator.plan.steps;

import static io.harness.annotations.dev.HarnessTeam.GITOPS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.gitops.MergePRStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(GITOPS)
public class GitOpsMergePRStepPlanCreatorV2 extends CDPMSStepPlanCreatorV2<MergePRStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.GITOPS_MERGE_PR);
  }

  @Override
  public Class<MergePRStepNode> getFieldClass() {
    return MergePRStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, MergePRStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
