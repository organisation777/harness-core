/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.resources.amazons3.service;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.exception.WingsException.USER;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import com.google.common.annotations.VisibleForTesting;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.resources.jenkins.mappers.JenkinsResourceMapper;
import io.harness.common.NGTaskType;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.task.artifacts.ArtifactDelegateRequestUtils;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.amazons3.AmazonS3ArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.exception.*;
import io.harness.exception.exceptionmanager.exceptionhandler.DocumentLinksConstants;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;

import io.harness.service.DelegateGrpcClientWrapper;
import software.wings.service.intfc.aws.delegate.AwsS3HelperServiceDelegate;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class AmazonS3ResourceServiceImpl implements AmazonS3ResourceService {
  private final ConnectorService connectorService;
  private final SecretManagerClientService secretManagerClientService;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Inject AwsS3HelperServiceDelegate awsS3HelperServiceDelegate;
  private static final int FETCH_FILE_COUNT_IN_BUCKET = 500;
  private static final int MAX_FILES_TO_SHOW_IN_UI = 1000;
  @VisibleForTesting
  static final int timeoutInSecs = 30;

  public AmazonS3ResourceServiceImpl(@Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService connectorService,
      SecretManagerClientService secretManagerClientService) {
    this.connectorService = connectorService;
    this.secretManagerClientService = secretManagerClientService;
  }

  @Override
  public Map<String, String> getBuckets(
      IdentifierRef connectorIdentifier, String accountId, String orgId, String projId) {

    AwsConnectorDTO amazonS3Connector = getConnector(connectorIdentifier);

    BaseNGAccess baseNGAccess = getBaseNGAccess(accountId, orgId, projId);

    List<EncryptedDataDetail> encryptionDetails = getEncryptionDetails(amazonS3Connector, baseNGAccess);

    AmazonS3ArtifactDelegateRequest amazonS3ArtifactDelegateRequest =
            ArtifactDelegateRequestUtils.getAmazonS3DelegateRequest(connectorIdentifier.getIdentifier(), amazonS3Connector,
                    encryptionDetails, ArtifactSourceType.JENKINS, null, null, null, null);

    try {

      ArtifactTaskExecutionResponse artifactTaskExecutionResponse = executeSyncTask(
              amazonS3ArtifactDelegateRequest, ArtifactTaskType.GET_PLANS, baseNGAccess, "AmazonS3 Get Buckets task failure due to error.");

      return artifactTaskExecutionResponse.getBuckets();

    } catch (DelegateServiceDriverException ex) {
      throw new HintException(
              String.format(HintException.DELEGATE_NOT_AVAILABLE, DocumentLinksConstants.DELEGATE_INSTALLATION_LINK),
              new DelegateNotAvailableException(ex.getCause().getMessage(), WingsException.USER));
    }
  }

  private ArtifactTaskExecutionResponse executeSyncTask(AmazonS3ArtifactDelegateRequest artifactDelegateRequest,
                                                        ArtifactTaskType taskType, BaseNGAccess ngAccess, String ifFailedMessage) {
    DelegateResponseData responseData = getResponseData(ngAccess, artifactDelegateRequest, taskType);
    return getTaskExecutionResponse(responseData, ifFailedMessage);
  }

  private DelegateResponseData getResponseData(
          BaseNGAccess ngAccess, AmazonS3ArtifactDelegateRequest delegateRequest, ArtifactTaskType artifactTaskType) {

    ArtifactTaskParameters artifactTaskParameters = ArtifactTaskParameters.builder()
            .accountId(ngAccess.getAccountIdentifier())
            .artifactTaskType(artifactTaskType)
            .attributes(delegateRequest)
            .build();

    final DelegateTaskRequest delegateTaskRequest =
            DelegateTaskRequest.builder()
                    .accountId(ngAccess.getAccountIdentifier())
                    .taskType(NGTaskType.AMAZONS3_ARTIFACT_TASK_NG.name())
                    .taskParameters(artifactTaskParameters)
                    .executionTimeout(java.time.Duration.ofSeconds(timeoutInSecs))
                    .taskSetupAbstraction("orgIdentifier", ngAccess.getOrgIdentifier())
                    .taskSetupAbstraction("ng", "true")
                    .taskSetupAbstraction("owner", ngAccess.getOrgIdentifier() + "/" + ngAccess.getProjectIdentifier())
                    .taskSetupAbstraction("projectIdentifier", ngAccess.getProjectIdentifier())
                    .taskSelectors(delegateRequest.getAwsConnectorDTO().getDelegateSelectors())
                    .build();

    return delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
  }

  private ArtifactTaskExecutionResponse getTaskExecutionResponse(
          DelegateResponseData responseData, String ifFailedMessage) {
    if (responseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
      throw new ArtifactServerException(ifFailedMessage + " - " + errorNotifyResponseData.getErrorMessage());
    } else if (responseData instanceof RemoteMethodReturnValueData) {
      RemoteMethodReturnValueData remoteMethodReturnValueData = (RemoteMethodReturnValueData) responseData;
      if (remoteMethodReturnValueData.getException() instanceof InvalidRequestException) {
        throw(InvalidRequestException)(remoteMethodReturnValueData.getException());
      } else {
        throw new ArtifactServerException(
                "Unexpected error during authentication" + remoteMethodReturnValueData.getReturnValue(),
                USER);
      }
    }
    ArtifactTaskResponse artifactTaskResponse = (ArtifactTaskResponse) responseData;
    if (artifactTaskResponse.getCommandExecutionStatus() != SUCCESS) {
      throw new ArtifactServerException(ifFailedMessage + " - " + artifactTaskResponse.getErrorMessage()
              + " with error code: " + artifactTaskResponse.getErrorCode());
    }
    return artifactTaskResponse.getArtifactTaskExecutionResponse();
  }

  private AwsConnectorDTO getConnector(IdentifierRef connectorRef) {
    Optional<ConnectorResponseDTO> amazonS3ConnectorDTO = connectorService.get(connectorRef.getAccountIdentifier(),
        connectorRef.getOrgIdentifier(), connectorRef.getProjectIdentifier(), connectorRef.getIdentifier());

    if (!amazonS3ConnectorDTO.isPresent() || !isAnAWSConnector(amazonS3ConnectorDTO.get())) {
      throw new InvalidRequestException(String.format("Connector not found for identifier : [%s] with scope: [%s]",
                                            connectorRef.getIdentifier(), connectorRef.getScope()),
          WingsException.USER);
    }
    ConnectorInfoDTO connectors = amazonS3ConnectorDTO.get().getConnector();
    return (AwsConnectorDTO) connectors.getConnectorConfig();
  }

  private boolean isAnAWSConnector(@Valid @NotNull ConnectorResponseDTO connectorResponseDTO) {
    return ConnectorType.AWS == (connectorResponseDTO.getConnector().getConnectorType());
  }

  private BaseNGAccess getBaseNGAccess(String accountId, String orgIdentifier, String projectIdentifier) {
    return BaseNGAccess.builder()
        .accountIdentifier(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }

  private List<EncryptedDataDetail> getEncryptionDetails(
      @Nonnull AwsConnectorDTO awsConnectorDTO, @Nonnull NGAccess ngAccess) {
    if (awsConnectorDTO.getCredential() != null && awsConnectorDTO.getCredential() != null) {
      return secretManagerClientService.getEncryptionDetails(ngAccess, awsConnectorDTO.getCredential());
    }
    return new ArrayList<>();
  }

  @Override
  public List<String> getArtifactPaths(
      IdentifierRef connectorIdentifier, String accountId, String orgId, String projId) {
    return null;
  }
}
