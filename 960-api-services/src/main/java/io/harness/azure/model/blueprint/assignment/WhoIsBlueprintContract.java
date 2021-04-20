package io.harness.azure.model.blueprint.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhoIsBlueprintContract {
  private String objectId;
}
