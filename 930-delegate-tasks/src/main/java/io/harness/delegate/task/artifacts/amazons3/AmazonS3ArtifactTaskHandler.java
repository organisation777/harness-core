package io.harness.delegate.task.artifacts.amazons3;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.delegate.task.artifacts.DelegateArtifactTaskHandler;
import io.harness.delegate.task.artifacts.mappers.JenkinsRequestResponseMapper;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.security.encryption.SecretDecryptionService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import software.wings.helpers.ext.jenkins.JobDetails;

import java.util.List;


@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject}))
public class AmazonS3ArtifactTaskHandler extends DelegateArtifactTaskHandler<AmazonS3ArtifactDelegateRequest> {
    private static final int ARTIFACT_RETENTION_SIZE = 25;
    private final SecretDecryptionService secretDecryptionService;

    @Override
    public ArtifactTaskExecutionResponse getBuckets(AmazonS3ArtifactDelegateRequest artifactDelegateRequest) {
        return null;
    }

    @Override
    public void decryptRequestDTOs(AmazonS3ArtifactDelegateRequest artifactDelegateRequest) {
        if (artifactDelegateRequest.getAwsConnectorDTO().getCredential() != null) {
            secretDecryptionService.decrypt(
                    artifactDelegateRequest.getAwsConnectorDTO().getCredential(),
                    artifactDelegateRequest.getEncryptedDataDetails());
        }
    }
}
