/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.visitor.helpers.cdstepinfo;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.gitops.MergePRStepInfo;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

import static io.harness.annotations.dev.HarnessTeam.GITOPS;

@OwnedBy(GITOPS)
public class MergePRStepVisitorHelper implements ConfigValidator {
    @Override
    public Object createDummyVisitableElement(Object originalElement) {
        return MergePRStepInfo.builder().build();
    }

    @Override
    public void validate(Object object, ValidationVisitor visitor) {}

}
