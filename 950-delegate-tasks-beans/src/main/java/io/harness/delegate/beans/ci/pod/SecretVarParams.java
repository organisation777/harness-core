package io.harness.delegate.beans.ci.pod;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SecretVarParams {
  @NotNull private String secretName; // Name of the secret
  @NotNull private String secretKey; // Name of key in the secret
}
