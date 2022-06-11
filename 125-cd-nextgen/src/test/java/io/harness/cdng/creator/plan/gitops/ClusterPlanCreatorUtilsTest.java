package io.harness.cdng.creator.plan.gitops;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cdng.environment.yaml.EnvironmentPlanCreatorConfig;
import io.harness.cdng.gitops.steps.ClusterStepParameters;
import io.harness.cdng.gitops.steps.EnvClusterRefs;
import io.harness.cdng.gitops.steps.GitopsClustersStep;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.util.Collections;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class ClusterPlanCreatorUtilsTest {
  @Test
  @Category(UnitTests.class)
  @Owner(developers = OwnerRule.YOGESH)
  @Parameters(method = "getData")
  public void testGetGitopsClustersStepPlanNodeBuilder(EnvironmentPlanCreatorConfig input, StepParameters output) {
    final String nodeUuid = "foobar";
    PlanNode expected = ClusterPlanCreatorUtils.getGitopsClustersStepPlanNodeBuilder(nodeUuid, input).build();
    assertThat(expected.getStepParameters()).isEqualTo(output);
    assertThat(expected.getFacilitatorObtainments()).isNotNull();
    assertThat(expected.getIdentifier()).isEqualTo("GitopsClusters");
    assertThat(expected.getUuid()).isEqualTo(nodeUuid);
    assertThat(expected.getName()).isEqualTo("GitopsClusters");
    assertThat(expected.getStepType()).isEqualTo(GitopsClustersStep.STEP_TYPE);
  }
  @Test
  @Category(UnitTests.class)
  @Owner(developers = OwnerRule.YOGESH)
  public void testGetGitopsClustersStepPlanNodeBuilderEnvGroup() {}

  // Method to provide parameters to test
  private Object[][] getData() {
    EnvironmentPlanCreatorConfig i1 = EnvironmentPlanCreatorConfig.builder()
                                          .environmentRef(ParameterField.<String>builder().value("myenv").build())
                                          .deployToAll(true)
                                          .build();
    StepParameters o1 = ClusterStepParameters.builder()
                            .envClusterRefs(Collections.singletonList(
                                EnvClusterRefs.builder().envRef("myenv").deployToAll(true).build()))
                            .build();

    EnvironmentPlanCreatorConfig i2 = EnvironmentPlanCreatorConfig.builder()
                                          .environmentRef(ParameterField.<String>builder().value("myenv").build())
                                          .deployToAll(false)
                                          .gitOpsClusterRefs(asList("c1", "c2", "c3"))
                                          .build();
    StepParameters o2 = ClusterStepParameters.builder()
                            .envClusterRefs(Collections.singletonList(EnvClusterRefs.builder()
                                                                          .envRef("myenv")
                                                                          .deployToAll(false)
                                                                          .clusterRefs(asList("c1", "c2", "c3"))
                                                                          .build()))
                            .build();
    return new Object[][] {{i1, o1}, {i2, o2}};
  }
}