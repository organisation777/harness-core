package io.harness.delegate.task.artifacts.amazons3;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.artifacts.ArtifactSourceDelegateRequest;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import software.wings.helpers.ext.jenkins.JobDetails;

import java.util.List;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@OwnedBy(HarnessTeam.CDC)
public class AmazonS3ArtifactDelegateRequest implements ArtifactSourceDelegateRequest {
    String buildRegex;
    List<String> artifactPaths;
    String connectorRef;
    List<JobDetails> jobDetails;
    String parentJobName;
    String jobName;
    AwsConnectorDTO awsConnectorDTO;
    List<EncryptedDataDetail> encryptedDataDetails;
    ArtifactSourceType sourceType;

    @Override
    public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
        return null;
    }
}
