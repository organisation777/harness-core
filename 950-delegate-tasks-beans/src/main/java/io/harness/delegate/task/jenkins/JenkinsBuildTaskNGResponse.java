/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.jenkins;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.sm.states.FilePathAssertionEntry;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JenkinsBuildTaskNGResponse implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String jenkinsResult;
  private String errorMessage;
  private String jobUrl;
  private List<FilePathAssertionEntry> filePathAssertionMap = Lists.newArrayList();
  private String buildNumber;
  private Map<String, String> metadata;
  private Map<String, String> jobParameters;
  private Map<String, String> envVars;
  private String description;
  private String buildDisplayName;
  private String buildFullDisplayName;
  private String queuedBuildUrl;
  private String activityId;
  private Long timeElapsed; // time taken for task completion
  @Override
  public DelegateMetaInfo getDelegateMetaInfo() {
    return null;
  }

  @Override
  public void setDelegateMetaInfo(DelegateMetaInfo metaInfo) {
    return;
  }
}
