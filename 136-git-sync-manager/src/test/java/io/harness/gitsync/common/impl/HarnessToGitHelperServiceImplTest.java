/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.impl;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.ChangeType;
import io.harness.gitsync.FileInfo;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.ng.core.EntityDetail;
import io.harness.rule.Owner;

import com.google.protobuf.StringValue;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

@OwnedBy(HarnessTeam.PL)
@RunWith(PowerMockRunner.class)
public class HarnessToGitHelperServiceImplTest extends GitSyncTestBase {
  @InjectMocks HarnessToGitHelperServiceImpl harnessToGitHelperService;
  @Mock GitEntityService gitEntityService;

  String baseBranch = "baseBranch";
  String branch = "branch";
  String commitId = "commitId";

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testIfConflictCommitIdPresent() {
    String lastCommitId = harnessToGitHelperService.fetchLastCommitIdForFile(
        getFileInfoDefault(commitId, ChangeType.MODIFY), getEntityDetailDefault());
    assertThat(lastCommitId).isEqualTo(commitId);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testFetchLastCommitIdForFileForAddChangeType() {
    String lastCommitId = harnessToGitHelperService.fetchLastCommitIdForFile(
        getFileInfoDefault(commitId, ChangeType.ADD), getEntityDetailDefault());
    assertThat(lastCommitId).isEqualTo(commitId);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testFetchLastCommitIdForFileUpdateCase() {
    ArgumentCaptor<String> branchArgumentCaptor = ArgumentCaptor.forClass(String.class);
    when(gitEntityService.get(any(), any(), any())).thenReturn(getGitSyncEntityDTODefault());
    String lastCommitId = harnessToGitHelperService.fetchLastCommitIdForFile(
        getFileInfoDefault("", ChangeType.MODIFY), getEntityDetailDefault());
    verify(gitEntityService, times(1)).get(any(), any(), branchArgumentCaptor.capture());
    assertThat(branchArgumentCaptor.getValue()).isEqualTo(branch);
    assertThat(lastCommitId).isEqualTo(commitId);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testFetchLastCommitIdForFileUpdateToNewBranchCase() {
    ArgumentCaptor<String> branchArgumentCaptor = ArgumentCaptor.forClass(String.class);
    when(gitEntityService.get(any(), any(), any())).thenReturn(getGitSyncEntityDTODefault());
    String lastCommitId = harnessToGitHelperService.fetchLastCommitIdForFile(
        getFileInfoDefault("", ChangeType.MODIFY, true), getEntityDetailDefault());
    verify(gitEntityService, times(1)).get(any(), any(), branchArgumentCaptor.capture());
    assertThat(branchArgumentCaptor.getValue()).isEqualTo(baseBranch);
    assertThat(lastCommitId).isEqualTo(commitId);
  }

  private FileInfo getFileInfoDefault(String commitId, ChangeType changeType) {
    return getFileInfoDefault(commitId, changeType, false);
  }

  private FileInfo getFileInfoDefault(String commitId, ChangeType changeType, boolean isNewBranch) {
    return FileInfo.newBuilder()
        .setBaseBranch(StringValue.newBuilder().setValue(baseBranch).build())
        .setBranch(branch)
        .setCommitId(commitId)
        .setChangeType(changeType)
        .setIsNewBranch(isNewBranch)
        .build();
  }

  private GitSyncEntityDTO getGitSyncEntityDTODefault() {
    return GitSyncEntityDTO.builder().lastCommitId(commitId).build();
  }

  private EntityDetail getEntityDetailDefault() {
    return EntityDetail.builder().build();
  }
}
