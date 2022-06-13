package io.harness.delegate.task.filestore;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Value;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
@Value
@Builder
public class FetchFileTaskNgParams implements TaskParameters {
    private String accountIdentifier;
    private String orgIdentifier;
    private String projectIdentifier;
    private String identifier;
}
