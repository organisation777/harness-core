/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.steadystate.statuschecker;

import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.KubernetesStatusResponse;

import com.google.inject.Singleton;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentCondition;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1DeploymentStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import java.util.List;
import java.util.Optional;

@Singleton
public class DeploymentStatusViewer {
  public KubernetesStatusResponse extractRolloutStatus(V1Deployment deployment) {
    V1ObjectMeta meta = deployment.getMetadata();
    if (meta != null && meta.getGeneration() != null && deployment.getStatus() != null
        && deployment.getStatus().getObservedGeneration() != null
        && meta.getGeneration() <= deployment.getStatus().getObservedGeneration()) {
      V1DeploymentStatus deploymentStatus = deployment.getStatus();

      List<V1DeploymentCondition> deploymentConditionList = deploymentStatus.getConditions();
      if (deploymentConditionList != null) {
        Optional<V1DeploymentCondition> deploymentConditionOptional =
            deploymentConditionList.stream()
                .filter(deploymentCondition -> deploymentCondition.getType().equalsIgnoreCase("Progressing"))
                .findFirst();
        if (deploymentConditionOptional.isPresent()) {
          V1DeploymentCondition deploymentCondition = deploymentConditionOptional.get();
          if (deploymentCondition.getReason() != null
              && deploymentCondition.getReason().equalsIgnoreCase("ProgressDeadlineExceeded")) {
            throw new InvalidRequestException(
                String.format("deployment %s exceeded its progress deadline", meta.getName()));
          }
        }
      }

      V1DeploymentSpec deploymentSpec = deployment.getSpec();
      initializeNullFieldsInDeploymentStatus(deploymentStatus);

      if (deploymentSpec != null && deploymentSpec.getReplicas() != null
          && deploymentStatus.getUpdatedReplicas() < deploymentSpec.getReplicas()) {
        return KubernetesStatusResponse.builder()
            .isDone(false)
            .message(String.format(
                "Waiting for deployment %s rollout to finish: %s out of %s new replicas have been updated...%n",
                meta.getName(), deploymentStatus.getUpdatedReplicas(), deploymentSpec.getReplicas()))
            .build();
      }
      if (deploymentStatus.getReplicas() > deploymentStatus.getUpdatedReplicas()) {
        return KubernetesStatusResponse.builder()
            .isDone(false)
            .message(String.format(
                "Waiting for deployment %s rollout to finish: %s old replicas are pending termination...%n",
                meta.getName(), deploymentStatus.getReplicas() - deploymentStatus.getUpdatedReplicas()))
            .build();
      }
      if (deploymentStatus.getAvailableReplicas() < deploymentStatus.getUpdatedReplicas()) {
        return KubernetesStatusResponse.builder()
            .isDone(false)
            .message(String.format(
                "Waiting for deployment %s rollout to finish: %s of %s updated replicas are available...%n",
                meta.getName(), deploymentStatus.getAvailableReplicas(), deploymentStatus.getUpdatedReplicas()))
            .build();
      }
      return KubernetesStatusResponse.builder()
          .isDone(true)
          .message(String.format("deployment %s successfully rolled out%n", meta.getName()))
          .build();
    }
    return KubernetesStatusResponse.builder()
        .isDone(false)
        .message(String.format("Waiting for deployment spec update to be observed...%n"))
        .build();
  }

  private void initializeNullFieldsInDeploymentStatus(V1DeploymentStatus deploymentStatus) {
    if (deploymentStatus.getAvailableReplicas() == null) {
      deploymentStatus.setAvailableReplicas(0);
    }
    if (deploymentStatus.getUpdatedReplicas() == null) {
      deploymentStatus.setUpdatedReplicas(0);
    }
    if (deploymentStatus.getReplicas() == null) {
      deploymentStatus.setReplicas(0);
    }
    if (deploymentStatus.getReadyReplicas() == null) {
      deploymentStatus.setReadyReplicas(0);
    }
    if (deploymentStatus.getUnavailableReplicas() == null) {
      deploymentStatus.setUnavailableReplicas(0);
    }
  }
}
