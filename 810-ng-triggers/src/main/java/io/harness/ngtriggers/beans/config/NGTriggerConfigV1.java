package io.harness.ngtriggers.beans.config;

import io.harness.ngtriggers.beans.source.NGTriggerSourceV1;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NGTriggerConfigV1 implements NGTriggerInterface {
  String name;
  @NotNull String identifier;
  String description;
  String orgIdentifier;
  String projectIdentifier;
  String pipelineIdentifier;
  Map<String, String> tags;
  String inputYaml;
  NGTriggerSourceV1 source;
  @Builder.Default Boolean enabled = Boolean.TRUE;
}
