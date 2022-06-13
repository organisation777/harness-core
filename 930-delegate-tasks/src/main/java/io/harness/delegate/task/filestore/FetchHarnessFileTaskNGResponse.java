package io.harness.delegate.task.filestore;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FetchHarnessFileTaskNGResponse implements DelegateTaskNotifyResponseData {
    private DelegateMetaInfo delegateMetaInfo;
    private Object file;
    private boolean isConnectionSuccessful;
    private String errorMessage;
}
