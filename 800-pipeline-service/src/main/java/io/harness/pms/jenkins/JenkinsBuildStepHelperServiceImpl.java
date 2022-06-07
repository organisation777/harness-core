/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.jenkins;

import io.harness.beans.EnvironmentType;
import io.harness.beans.IdentifierRef;
import io.harness.common.NGTaskType;
import io.harness.common.NGTimeConversionHelper;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.jenkins.JenkinsConnectorDTO;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.jenkins.JenkinsArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.jenkins.JenkinsArtifactDelegateRequest.JenkinsArtifactDelegateRequestBuilder;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.delegate.task.jenkins.JenkinsBuildTaskNGResponse;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.remote.client.NGRestUtils;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepUtils;
import io.harness.steps.jenkins.jenkinsstep.JenkinsBuildOutcome;
import io.harness.steps.jenkins.jenkinsstep.JenkinsBuildOutcome.JenkinsBuildOutcomeBuilder;
import io.harness.steps.jenkins.jenkinsstep.JenkinsBuildStepHelperService;
import io.harness.supplier.ThrowingSupplier;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

public class JenkinsBuildStepHelperServiceImpl implements JenkinsBuildStepHelperService {
  private final ConnectorResourceClient connectorResourceClient;
  private final SecretManagerClientService secretManagerClientService;
  private final KryoSerializer kryoSerializer;
  private final LogStreamingStepClientFactory logStreamingStepClientFactory;

  @Inject
  public JenkinsBuildStepHelperServiceImpl(ConnectorResourceClient connectorResourceClient,
      @Named("PRIVILEGED") SecretManagerClientService secretManagerClientService, KryoSerializer kryoSerializer,
      LogStreamingStepClientFactory logStreamingStepClientFactory) {
    this.connectorResourceClient = connectorResourceClient;
    this.secretManagerClientService = secretManagerClientService;
    this.kryoSerializer = kryoSerializer;
    this.logStreamingStepClientFactory = logStreamingStepClientFactory;
  }

  @Override
  public TaskRequest prepareTaskRequest(JenkinsArtifactDelegateRequestBuilder paramsBuilder, Ambiance ambiance,
      String connectorRef, String timeStr, String taskName) {
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        connectorRef, ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
    Optional<ConnectorDTO> connectorDTOOptional = NGRestUtils.getResponse(
        connectorResourceClient.get(identifierRef.getIdentifier(), identifierRef.getAccountIdentifier(),
            identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier()));
    if (!connectorDTOOptional.isPresent()) {
      throw new InvalidRequestException(
          String.format("Connector not found for identifier: [%s]", connectorRef), WingsException.USER);
    }

    ConnectorConfigDTO configDTO = connectorDTOOptional.get().getConnectorInfo().getConnectorConfig();
    if (!(configDTO instanceof JenkinsConnectorDTO)) {
      throw new InvalidRequestException(
          String.format("Connector [%s] is not a jenkins connector", connectorRef), WingsException.USER);
    }

    JenkinsConnectorDTO connectorDTO = (JenkinsConnectorDTO) configDTO;
    paramsBuilder.jenkinsConnectorDTO(connectorDTO);
    paramsBuilder.encryptedDataDetails(
        secretManagerClientService.getEncryptionDetails(ngAccess, connectorDTO.getAuth().getCredentials()));
    JenkinsArtifactDelegateRequest params = paramsBuilder.build();
    ArtifactTaskParameters artifactTaskParameters = ArtifactTaskParameters.builder()
                                                        .accountId(ngAccess.getAccountIdentifier())
                                                        .artifactTaskType(ArtifactTaskType.JENKINS_BUILD)
                                                        .attributes(params)
                                                        .build();
    TaskData taskData = TaskData.builder()
                            .async(true)
                            .timeout(NGTimeConversionHelper.convertTimeStringToMilliseconds(timeStr))
                            .taskType(NGTaskType.JENKINS_ARTIFACT_TASK_NG.name())
                            .parameters(new Object[] {artifactTaskParameters})
                            .build();
    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer, TaskCategory.DELEGATE_TASK_V2,
        Collections.singletonList("Execute"), true, taskName,
        params.getDelegateSelectors()
            .stream()
            .map(s -> TaskSelector.newBuilder().setSelector(s).build())
            .collect(Collectors.toList()),
        Scope.PROJECT, EnvironmentType.ALL);
  }

  @Override
  public StepResponse prepareStepResponse(ThrowingSupplier<ArtifactTaskResponse> responseSupplier) throws Exception {
    ArtifactTaskResponse taskResponse = responseSupplier.get();
    JenkinsBuildTaskNGResponse jenkinsBuildTaskNGResponse =
        taskResponse.getArtifactTaskExecutionResponse().getJenkinsBuildTaskNGResponse();
    JenkinsBuildOutcomeBuilder jenkinsBuildOutcomeBuilder =
        JenkinsBuildOutcome.builder()
            .buildDisplayName(jenkinsBuildTaskNGResponse.getBuildDisplayName())
            .buildFullDisplayName(jenkinsBuildTaskNGResponse.getBuildFullDisplayName())
            .buildNumber(jenkinsBuildTaskNGResponse.getBuildNumber())
            .envVars(jenkinsBuildTaskNGResponse.getEnvVars());
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(
            StepResponse.StepOutcome.builder().name("build").outcome(jenkinsBuildOutcomeBuilder.build()).build())
        .build();
  }
}
