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
