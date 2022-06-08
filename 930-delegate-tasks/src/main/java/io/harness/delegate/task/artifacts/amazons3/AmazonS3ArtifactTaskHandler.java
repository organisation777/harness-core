package io.harness.delegate.task.artifacts.amazons3;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.delegate.task.artifacts.DelegateArtifactTaskHandler;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;


@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject}))
public class AmazonS3ArtifactTaskHandler extends DelegateArtifactTaskHandler<AmazonS3ArtifactDelegateRequest> {
    @Override
    public void decryptRequestDTOs(AmazonS3ArtifactDelegateRequest dto) {

    }
}
