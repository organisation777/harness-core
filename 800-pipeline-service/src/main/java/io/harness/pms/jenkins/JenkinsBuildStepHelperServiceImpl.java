/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.jenkins;

import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.delegate.task.jira.JiraTaskNGResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.jenkins.jenkinsstep.JenkinsBuildStepHelperService;
import io.harness.supplier.ThrowingSupplier;

public class JenkinsBuildStepHelperServiceImpl implements JenkinsBuildStepHelperService {
  @Override
  public TaskRequest prepareTaskRequest(JiraTaskNGParameters.JiraTaskNGParametersBuilder paramsBuilder,
      Ambiance ambiance, String connectorRef, String timeStr, String taskName) {
    return null;
  }

  @Override
  public StepResponse prepareStepResponse(ThrowingSupplier<JiraTaskNGResponse> responseSupplier) throws Exception {
    return null;
  }
}
