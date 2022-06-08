/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.resources.amazons3.service;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;

import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;

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
  @Inject AwsS3HelperServiceDelegate awsS3HelperServiceDelegate;
  private static final int FETCH_FILE_COUNT_IN_BUCKET = 500;
  private static final int MAX_FILES_TO_SHOW_IN_UI = 1000;

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

    return null;
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
