/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.jenkins;

import io.harness.artifacts.jenkins.beans.JenkinsInternalConfig;
import io.harness.beans.EnvironmentType;
import io.harness.beans.IdentifierRef;
import io.harness.common.NGTaskType;
import io.harness.common.NGTimeConversionHelper;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.jenkins.JenkinsBearerTokenDTO;
import io.harness.delegate.beans.connector.jenkins.JenkinsConnectorDTO;
import io.harness.delegate.beans.connector.jenkins.JenkinsConstant;
import io.harness.delegate.beans.connector.jenkins.JenkinsUserNamePasswordDTO;
import io.harness.delegate.task.artifacts.ArtifactDelegateRequestUtils;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.jenkins.JenkinsArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.jenkins.JenkinsArtifactDelegateRequest.JenkinsArtifactDelegateRequestBuilder;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.jenkins.JenkinsBuildTaskNGParameters;
import io.harness.delegate.task.jenkins.JenkinsBuildTaskNGResponse;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.remote.client.NGRestUtils;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepUtils;
import io.harness.steps.jenkins.jenkinsstep.JenkinsBuildOutcome;
import io.harness.steps.jenkins.jenkinsstep.JenkinsBuildStepHelperService;
import io.harness.supplier.ThrowingSupplier;
import io.harness.utils.FieldWithPlainTextOrSecretValueHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JenkinsBuildStepHelperServiceImpl implements JenkinsBuildStepHelperService {
  private final ConnectorResourceClient connectorResourceClient;
  private final SecretManagerClientService secretManagerClientService;
  private final KryoSerializer kryoSerializer;

  @Inject
  public JenkinsBuildStepHelperServiceImpl(ConnectorResourceClient connectorResourceClient,
      @Named("PRIVILEGED") SecretManagerClientService secretManagerClientService, KryoSerializer kryoSerializer) {
    this.connectorResourceClient = connectorResourceClient;
    this.secretManagerClientService = secretManagerClientService;
    this.kryoSerializer = kryoSerializer;
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
          String.format("Connector [%s] is not a jira connector", connectorRef), WingsException.USER);
    }
    JenkinsConnectorDTO connectorDTO = (JenkinsConnectorDTO) configDTO;
    paramsBuilder.jenkinsConnectorDTO(connectorDTO);
    paramsBuilder.encryptedDataDetails(secretManagerClientService.getEncryptionDetails(ngAccess, connectorDTO));
    JenkinsArtifactDelegateRequest params = paramsBuilder.build();
    List<EncryptedDataDetail> encryptionDetails =
        secretManagerClientService.getEncryptionDetails(ngAccess, connectorDTO);
    JenkinsArtifactDelegateRequest jenkinsRequest = ArtifactDelegateRequestUtils.getJenkinsDelegateRequest(
        connectorRef, connectorDTO, encryptionDetails, ArtifactSourceType.JENKINS, null, null, null, null);
    ArtifactTaskParameters artifactTaskParameters = ArtifactTaskParameters.builder()
                                                        .accountId(ngAccess.getAccountIdentifier())
                                                        .artifactTaskType(ArtifactTaskType.JENKINS_BUILD)
                                                        .attributes(jenkinsRequest)
                                                        .build();
    TaskData taskData = TaskData.builder()
                            .async(true)
                            .timeout(NGTimeConversionHelper.convertTimeStringToMilliseconds(timeStr))
                            .taskType(NGTaskType.JENKINS_ARTIFACT_TASK_NG.name())
                            .parameters(new Object[] {artifactTaskParameters})
                            .build();
    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer, TaskCategory.DELEGATE_TASK_V2,
        Collections.emptyList(), false, taskName,
        params.getDelegateSelectors()
            .stream()
            .map(s -> TaskSelector.newBuilder().setSelector(s).build())
            .collect(Collectors.toList()),
        Scope.PROJECT, EnvironmentType.ALL);
  }

  @Override
  public StepResponse prepareStepResponse(ThrowingSupplier<JenkinsBuildTaskNGResponse> responseSupplier)
      throws Exception {
    JenkinsBuildTaskNGResponse taskResponse = responseSupplier.get();
    JenkinsBuildOutcome.JenkinsBuildOutcomeBuilder jenkinsBuildOutcomeBuilder =
        JenkinsBuildOutcome.builder()
            .buildDisplayName(taskResponse.getBuildDisplayName())
            .buildFullDisplayName(taskResponse.getBuildFullDisplayName())
            .buildNumber(taskResponse.getBuildNumber())
            .envVars(taskResponse.getEnvVars());
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(
            StepResponse.StepOutcome.builder().name("build").outcome(jenkinsBuildOutcomeBuilder.build()).build())
        .build();
  }

  public JenkinsInternalConfig toJenkinsInternalConfigMapper(JenkinsConnectorDTO jenkinsConnectorDTO) {
    String password = "";
    String username = "";
    String token = "";
    if (jenkinsConnectorDTO.getAuth() != null && jenkinsConnectorDTO.getAuth().getCredentials() != null) {
      if (jenkinsConnectorDTO.getAuth().getAuthType().getDisplayName() == JenkinsConstant.BEARER_TOKEN) {
        JenkinsBearerTokenDTO jenkinsBearerTokenDTO =
            (JenkinsBearerTokenDTO) jenkinsConnectorDTO.getAuth().getCredentials();
        if (jenkinsBearerTokenDTO.getTokenRef() != null) {
          token = EmptyPredicate.isNotEmpty(jenkinsBearerTokenDTO.getTokenRef().getDecryptedValue())
              ? new String(jenkinsBearerTokenDTO.getTokenRef().getDecryptedValue())
              : null;
        }
      } else if (jenkinsConnectorDTO.getAuth().getAuthType().getDisplayName() == JenkinsConstant.USERNAME_PASSWORD) {
        JenkinsUserNamePasswordDTO credentials =
            (JenkinsUserNamePasswordDTO) jenkinsConnectorDTO.getAuth().getCredentials();
        if (credentials.getPasswordRef() != null) {
          password = EmptyPredicate.isNotEmpty(credentials.getPasswordRef().getDecryptedValue())
              ? new String(credentials.getPasswordRef().getDecryptedValue())
              : null;
        }
        if (credentials.getPasswordRef() != null) {
          password = EmptyPredicate.isNotEmpty(credentials.getPasswordRef().getDecryptedValue())
              ? new String(credentials.getPasswordRef().getDecryptedValue())
              : null;
        }
        username = FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef(
            credentials.getUsername(), credentials.getUsernameRef());
      }
    }
    return JenkinsInternalConfig.builder()
        .jenkinsUrl(jenkinsConnectorDTO.getJenkinsUrl())
        .authMechanism(jenkinsConnectorDTO.getAuth().getAuthType().getDisplayName())
        .username(username)
        .password(password.toCharArray())
        .token(token.toCharArray())
        .build();
  }
}
