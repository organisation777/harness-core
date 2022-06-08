package io.harness.delegate.task.artifacts.amazons3;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.context.MdcGlobalContextData;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.task.artifacts.jenkins.JenkinsArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionMetadataKeys;
import io.harness.exception.runtime.JenkinsServerRuntimeException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.manage.GlobalContextManager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

import static io.harness.delegate.task.artifacts.ArtifactTaskType.*;

@OwnedBy(HarnessTeam.CDC)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject}))
@Slf4j
@Singleton
public class AmazonS3ArtifactTaskHelper {
    private  AmazonS3ArtifactTaskHandler amazonS3ArtifactTaskHandler;

    public ArtifactTaskResponse getArtifactCollectResponse(ArtifactTaskParameters taskParameters, LogCallback executionLogCallback) {
        return null;
    }

    private ArtifactTaskResponse getSuccessTaskResponse(ArtifactTaskExecutionResponse taskExecutionResponse) {
        return ArtifactTaskResponse.builder()
                .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                .artifactTaskExecutionResponse(taskExecutionResponse)
                .build();
    }

    private void saveLogs(LogCallback executionLogCallback, String message) {
        if (executionLogCallback != null) {
            executionLogCallback.saveExecutionLog(message);
        }
    }
    public ArtifactTaskResponse getArtifactCollectResponse(ArtifactTaskParameters artifactTaskParameters) {
        return getArtifactCollectResponse(artifactTaskParameters, null);
    }
}
