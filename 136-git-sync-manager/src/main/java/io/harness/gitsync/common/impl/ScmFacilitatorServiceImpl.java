package io.harness.gitsync.common.impl;

import static io.harness.gitsync.GitSyncModule.SCM_ON_DELEGATE;
import static io.harness.gitsync.GitSyncModule.SCM_ON_MANAGER;

import io.harness.gitsync.common.service.ScmClientFacilitatorService;
import io.harness.gitsync.common.service.ScmFacilitatorService;
import io.harness.ng.beans.PageRequest;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;

public class ScmFacilitatorServiceImpl implements ScmFacilitatorService {
  ScmClientFacilitatorService scmThroughManagerService;
  ScmClientFacilitatorService scmThroughDelegateService;

  @Inject
  public ScmFacilitatorServiceImpl(@Named(SCM_ON_MANAGER) ScmClientFacilitatorService scmThroughManagerService,
      @Named(SCM_ON_DELEGATE) ScmClientFacilitatorService scmThroughDelegateService) {
    this.scmThroughManagerService = scmThroughManagerService;
    this.scmThroughDelegateService = scmThroughDelegateService;
  }

  @Override
  public List<String> listBranchesUsingConnector(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifierRef, String repoURL, PageRequest pageRequest,
      String searchTerm) {
    return null;
  }
}
