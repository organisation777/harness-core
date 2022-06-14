package io.harness.pms.ngpipeline.inputset.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.gitaware.helper.GitAwareContextHelper;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.inputset.InputSetErrorWrapperDTOPMS;
import io.harness.pms.merger.helpers.InputSetYamlHelper;
import io.harness.pms.ngpipeline.inputset.helpers.InputSetErrorsHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PipelineCRUDErrorResponse;

import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class InputSetValidator {
  public InputSetErrorWrapperDTOPMS validateInputSetDuringCreate(PMSPipelineService pmsPipelineService,
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml) {
    return validateInputSet(
        pmsPipelineService, accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, true);
  }

  public InputSetErrorWrapperDTOPMS validateInputSet(PMSPipelineService pmsPipelineService, String accountId,
      String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml) {
    return validateInputSet(
        pmsPipelineService, accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, false);
  }

  InputSetErrorWrapperDTOPMS validateInputSet(PMSPipelineService pmsPipelineService, String accountId,
      String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml,
      boolean checkForStoreType) {
    validateIdentifyingFieldsInYAML(orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    PipelineEntity pipelineEntity =
        getPipelineEntity(pmsPipelineService, accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);
    if (checkForStoreType) {
      StoreType storeTypeInContext = GitAwareContextHelper.getGitRequestParamsInfo().getStoreType();
      if (pipelineEntity.getStoreType() != storeTypeInContext) {
        throw new InvalidRequestException("Input Set should have the same Store Type as the Pipeline it is for");
      }
    }
    String pipelineYAML = pipelineEntity.getYaml();
    return InputSetErrorsHelper.getErrorMap(pipelineYAML, yaml);
  }

  PipelineEntity getPipelineEntity(PMSPipelineService pmsPipelineService, String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier) {
    Optional<PipelineEntity> optionalPipelineEntity;
    if (GitContextHelper.isUpdateToNewBranch()) {
      String baseBranch = Objects.requireNonNull(GitContextHelper.getGitEntityInfo()).getBaseBranch();
      GitSyncBranchContext branchContext =
          GitSyncBranchContext.builder().gitBranchInfo(GitEntityInfo.builder().branch(baseBranch).build()).build();
      try (PmsGitSyncBranchContextGuard ignored = new PmsGitSyncBranchContextGuard(branchContext, true)) {
        optionalPipelineEntity =
            pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
      }
    } else {
      optionalPipelineEntity =
          pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    }
    if (!optionalPipelineEntity.isPresent()) {
      throw new InvalidRequestException(PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(
          orgIdentifier, projectIdentifier, pipelineIdentifier));
    }
    return optionalPipelineEntity.get();
  }

  public InputSetErrorWrapperDTOPMS validateInputSetForOldGitSync(PMSPipelineService pmsPipelineService,
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml,
      String pipelineBranch, String pipelineRepoID) {
    validateIdentifyingFieldsInYAML(orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    String pipelineYaml = getPipelineYamlForOldGitSyncFlow(pmsPipelineService, accountId, orgIdentifier,
        projectIdentifier, pipelineIdentifier, pipelineBranch, pipelineRepoID);
    return InputSetErrorsHelper.getErrorMap(pipelineYaml, yaml);
  }

  String getPipelineYamlForOldGitSyncFlow(PMSPipelineService pmsPipelineService, String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String pipelineBranch, String pipelineRepoID) {
    GitSyncBranchContext gitSyncBranchContext =
        GitSyncBranchContext.builder()
            .gitBranchInfo(GitEntityInfo.builder().branch(pipelineBranch).yamlGitConfigId(pipelineRepoID).build())
            .build();

    String pipelineYaml;
    try (PmsGitSyncBranchContextGuard ignored = new PmsGitSyncBranchContextGuard(gitSyncBranchContext, true)) {
      Optional<PipelineEntity> pipelineEntity =
          pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
      if (pipelineEntity.isPresent()) {
        pipelineYaml = pipelineEntity.get().getYaml();
      } else {
        throw new InvalidRequestException(PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(
            orgIdentifier, projectIdentifier, pipelineIdentifier));
      }
    }
    return pipelineYaml;
  }

  void validateIdentifyingFieldsInYAML(
      String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml) {
    String identifier = InputSetYamlHelper.getStringField(yaml, "identifier", "inputSet");
    if (EmptyPredicate.isEmpty(identifier)) {
      throw new InvalidRequestException("Identifier cannot be empty");
    }
    if (identifier.length() > 63) {
      throw new InvalidRequestException("Input Set identifier length cannot be more that 63 characters.");
    }
    InputSetYamlHelper.confirmPipelineIdentifierInInputSet(yaml, pipelineIdentifier);
    InputSetYamlHelper.confirmOrgAndProjectIdentifier(yaml, "inputSet", orgIdentifier, projectIdentifier);
  }
}
