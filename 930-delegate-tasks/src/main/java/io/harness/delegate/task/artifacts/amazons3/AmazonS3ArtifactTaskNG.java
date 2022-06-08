package io.harness.delegate.task.artifacts.amazons3;

import com.google.inject.Inject;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class AmazonS3ArtifactTaskNG extends AbstractDelegateRunnableTask {

    @Inject
    AmazonS3ArtifactTaskHelper amazonS3ArtifactTaskHelper;

    public AmazonS3ArtifactTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
        super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
    }

    @Override
    public boolean isSupportingErrorFramework() {
        return true;
    }

    @Override
    public DelegateResponseData run(Object[] parameters) {
        throw new NotImplementedException("not implemented");
    }

    @Override
    public DelegateResponseData run(TaskParameters parameters) throws IOException, JoseException {
        ArtifactTaskParameters taskParameters = (ArtifactTaskParameters) parameters;
        return amazonS3ArtifactTaskHelper.getArtifactCollectResponse(taskParameters);
    }
}
