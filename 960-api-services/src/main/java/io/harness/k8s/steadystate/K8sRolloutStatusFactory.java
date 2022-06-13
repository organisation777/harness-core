/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.steadystate;

import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.steadystate.statuschecker.AbstractSteadyStateChecker;
import io.harness.k8s.steadystate.statuschecker.DeploymentSteadyStateChecker;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class K8sRolloutStatusFactory {
  @Inject private KubernetesHelperService kubernetesHelperService;
  @Inject private DeploymentSteadyStateChecker deploymentSteadyStateChecker;
  public AbstractSteadyStateChecker getStatusViewer(String kind) {
    switch (kind) {
      case "Deployment":
        return deploymentSteadyStateChecker;
      case "StatefulSet":
        throw new IllegalStateException("Kind not supported yet: " + kind);
      case "DaemonSet":
        throw new IllegalStateException("Kind not supported yet: " + kind);
      default:
        throw new IllegalStateException("Unexpected value for workload kind: " + kind);
    }
  }
}
