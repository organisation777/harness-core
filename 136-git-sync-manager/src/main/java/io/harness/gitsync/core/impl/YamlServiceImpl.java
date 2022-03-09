/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.core.service.YamlService;

import java.util.List;

@OwnedBy(DX)
public class YamlServiceImpl implements YamlService {
  @Override
  public void processChangeSet(List<GitFileChange> gitFileChanges) {
    // todo(abhinav): implement this
  }
}