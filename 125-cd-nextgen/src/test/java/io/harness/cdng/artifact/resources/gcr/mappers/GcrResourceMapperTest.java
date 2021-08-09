package io.harness.cdng.artifact.resources.gcr.mappers;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.resources.gcr.dtos.GcrBuildDetailsDTO;
import io.harness.cdng.artifact.resources.gcr.dtos.GcrResponseDTO;
import io.harness.delegate.task.artifacts.gcr.GcrArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GcrResourceMapperTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testToGcrResponse() {
    List<GcrArtifactDelegateResponse> gcrArtifactDelegateResponses =
        Lists.newArrayList(GcrArtifactDelegateResponse.builder()
                               .buildDetails(ArtifactBuildDetailsNG.builder().build())
                               .imagePath("/")
                               .build());
    GcrResponseDTO gcrResponseDTO = GcrResourceMapper.toGcrResponse(gcrArtifactDelegateResponses);
    assertThat(gcrResponseDTO).isNotNull();
    assertThat(gcrResponseDTO.getBuildDetailsList()).isNotEmpty();
    assertThat(gcrResponseDTO.getBuildDetailsList().get(0))
        .isEqualTo(GcrBuildDetailsDTO.builder().imagePath("/").build());
  }
}
