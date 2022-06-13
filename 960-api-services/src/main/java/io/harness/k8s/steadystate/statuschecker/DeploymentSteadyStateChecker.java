/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.steadystate.statuschecker;

import io.harness.concurrent.HTimeLimiter;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.KubernetesRolloutStatusDTO;
import io.harness.k8s.model.KubernetesStatusResponse;
import io.harness.logging.LogCallback;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Watch;
import java.io.IOException;
import java.time.Duration;

@Singleton
public class DeploymentSteadyStateChecker implements AbstractSteadyStateChecker {
  @Inject private KubernetesHelperService kubernetesHelperService;
  @Inject private DeploymentStatusViewer deploymentStatusViewer;
  @Inject private TimeLimiter timeLimiter;

  @Override
  public boolean rolloutStatus(KubernetesRolloutStatusDTO kubernetesRolloutStatusDTO) throws Exception {
    ApiClient apiClient = kubernetesHelperService.getApiClient(kubernetesRolloutStatusDTO.getKubernetesConfig());
    AppsV1Api appsV1Api = new AppsV1Api(apiClient);
    KubernetesResourceId workload = kubernetesRolloutStatusDTO.getWorkload();
    LogCallback executionLogCallback = kubernetesRolloutStatusDTO.getLogCallback();

    HTimeLimiter.callInterruptible(
        timeLimiter, Duration.ofMillis(kubernetesRolloutStatusDTO.getTimeoutInMillis()), () -> {
          while (true) {
            try (Watch<V1Deployment> watch = Watch.createWatch(apiClient,
                     appsV1Api.listNamespacedDeploymentCall(
                         workload.getNamespace(), null, true, null, null, null, null, null, null, 300, true, null),
                     new TypeToken<Watch.Response<V1Deployment>>() {}.getType())) {
              for (Watch.Response<V1Deployment> event : watch) {
                V1Deployment deployment = event.object;
                V1ObjectMeta meta = deployment.getMetadata();
                if (meta != null && !workload.getName().equals(meta.getName())) {
                  continue;
                }
                switch (event.type) {
                  case "ADDED":
                  case "MODIFIED":
                    KubernetesStatusResponse rolloutStatus = deploymentStatusViewer.extractRolloutStatus(deployment);
                    executionLogCallback.saveExecutionLog(rolloutStatus.getMessage());
                    if (rolloutStatus.isDone()) {
                      return true;
                    }
                    break;
                  case "DELETED":
                    throw new InvalidRequestException("object has been deleted");
                  default:
                    throw new InvalidRequestException(String.format("unexpected k8s event %s", event.type));
                }
              }
            } catch (IOException | ApiException e) {
              throw new InvalidRequestException("failed to do status check.", e);
            }
          }
        });
    return true;
  }
}
