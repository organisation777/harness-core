package io.harness.cdng.gitops;

import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;

import java.util.List;
import lombok.Builder;

public class MergePRStepParams extends MergePRBaseStepInfo implements GitOpsSpecParameters {
  @Builder(builderMethodName = "infoBuilder")
  public MergePRStepParams(ParameterField<List<TaskSelectorYaml>> delegateSelectors) {
    super(delegateSelectors);
  }
}
