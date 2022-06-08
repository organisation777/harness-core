package io.harness.cdng.artifact.resources.amazons3.mappers;

import io.harness.cdng.artifact.resources.amazons3.dtos.BucketsResponseDTO;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AmazonS3ResourceMapper {
    public BucketsResponseDTO toAmazonS3BucketsDTO(ArtifactTaskExecutionResponse artifactTaskExecutionResponse) {
        return BucketsResponseDTO.builder().buckets(artifactTaskExecutionResponse.getBuckets()).build();
    }
}
