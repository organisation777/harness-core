/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.helm;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CDP)
public class HelmFetchFileResult implements Serializable {
  private List<String> valuesFileContents;

  public void addAllFrom(HelmFetchFileResult helmFetchFileResult) {
    if (isNotEmpty(helmFetchFileResult.getValuesFileContents())) {
      this.valuesFileContents.addAll(helmFetchFileResult.getValuesFileContents());
    }
  }
}
