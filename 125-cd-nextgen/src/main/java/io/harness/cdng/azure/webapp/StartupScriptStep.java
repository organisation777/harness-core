/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.azure.webapp;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.cdng.manifest.yaml.harness.HarnessStoreConstants.HARNESS_STORE_TYPE;
import static io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants.STARTUP_SCRIPT;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.azure.config.StartupScriptOutcome;
import io.harness.cdng.manifest.yaml.harness.HarnessStore;
import io.harness.cdng.manifest.yaml.harness.HarnessStoreFile;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.common.ParameterFieldHelper;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.utils.ConnectorUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.filestore.dto.node.FileStoreNodeDTO;
import io.harness.filestore.service.FileStoreService;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.api.NGEncryptedDataService;
import io.harness.ng.core.entities.NGEncryptedData;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Singleton
@Slf4j
public class StartupScriptStep implements SyncExecutable<StartupScriptParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.STARTUP_SCRIPT.getName())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Named(DEFAULT_CONNECTOR_SERVICE) @Inject private ConnectorService connectorService;
  @Inject private FileStoreService fileStoreService;
  @Inject private NGEncryptedDataService ngEncryptedDataService;

  @Override
  public Class<StartupScriptParameters> getStepParametersClass() {
    return StartupScriptParameters.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, StartupScriptParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    validateStoreReferences(stepParameters.getStartupScript(), ambiance);
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(
            StepResponse.StepOutcome.builder()
                .name(STARTUP_SCRIPT)
                .outcome(StartupScriptOutcome.builder().store(stepParameters.getStartupScript().getSpec()).build())
                .group(StepOutcomeGroup.STAGE.name())
                .build())
        .build();
  }

  private void validateStoreReferences(StoreConfigWrapper storeConfigWrapper, Ambiance ambiance) {
    StoreConfig storeConfig = storeConfigWrapper.getSpec();
    String storeKind = storeConfig.getKind();
    if (HARNESS_STORE_TYPE.equals(storeKind)) {
      validateFileRefs((HarnessStore) storeConfig, ambiance);
    } else {
      validateConnectorByRef(storeConfig, ambiance);
    }
  }

  private void validateFileRefs(HarnessStore harnessStore, Ambiance ambiance) {
    List<ParameterField<String>> fileReferences = harnessStore.getFileReferences();

    if (isEmpty(fileReferences)) {
      throw new InvalidRequestException(
          format("Cannot find any file reference for startup script, store kind: %s", harnessStore.getKind()));
    }
    if (fileReferences.size() > 1) {
      throw new InvalidRequestException(
          format("Only one startup script should be provided, store kind: %s", harnessStore.getKind()));
    }
    validateFileByRef(harnessStore, ambiance, harnessStore.getFiles().getValue().get(0));
  }

  private void validateFileByRef(HarnessStore harnessStore, Ambiance ambiance, HarnessStoreFile file) {
    if (ParameterField.isNull(file.getRef())) {
      throw new InvalidRequestException(
          format("File ref not found for one for startup script, store kind: %s", harnessStore.getKind()));
    }

    if (file.getRef().isExpression()) {
      return;
    }

    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    IdentifierRef fileRef = IdentifierRefHelper.getIdentifierRef(file.getRef().getValue(),
        ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());

    if (ParameterFieldHelper.getBooleanParameterFieldValue(file.getIsEncrypted()) == Boolean.TRUE) {
      NGEncryptedData ngEncryptedData = ngEncryptedDataService.get(fileRef.getAccountIdentifier(),
          fileRef.getOrgIdentifier(), fileRef.getProjectIdentifier(), fileRef.getIdentifier());
      if (ngEncryptedData == null) {
        throw new InvalidRequestException(
            format("Startup script not found in Encrypted Store with ref: [%s]", file.getRef()));
      }
    } else {
      Optional<FileStoreNodeDTO> startupScript = fileStoreService.get(fileRef.getAccountIdentifier(),
          fileRef.getOrgIdentifier(), fileRef.getProjectIdentifier(), fileRef.getIdentifier(), false);

      if (!startupScript.isPresent()) {
        throw new InvalidRequestException(
            format("Startup script not found in File Store with ref : [%s]", file.getRef()));
      }
    }
  }

  private void validateConnectorByRef(StoreConfig storeConfig, Ambiance ambiance) {
    if (ParameterField.isNull(storeConfig.getConnectorReference())) {
      throw new InvalidRequestException(
          format("Connector ref field not present in startup script, store kind: %s ", storeConfig.getKind()));
    }

    if (storeConfig.getConnectorReference().isExpression()) {
      return;
    }

    String connectorIdentifierRef = storeConfig.getConnectorReference().getValue();
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    IdentifierRef connectorRef = IdentifierRefHelper.getIdentifierRef(connectorIdentifierRef,
        ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());

    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(connectorRef.getAccountIdentifier(),
        connectorRef.getOrgIdentifier(), connectorRef.getProjectIdentifier(), connectorRef.getIdentifier());
    if (!connectorDTO.isPresent()) {
      throw new InvalidRequestException(format("Connector not found with identifier: [%s]", connectorIdentifierRef));
    }

    ConnectorUtils.checkForConnectorValidityOrThrow(connectorDTO.get());
  }
}
