package io.harness.ng.core.dto;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.IDENTIFIER_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@Schema(
    name = "ResourceScopeAndIdentifier", description = "This is the view for resource scope along with its identifier")
public class ResourceScopeAndIdentifierDTO {
  @NotNull @Schema(description = ACCOUNT_PARAM_MESSAGE) String accountIdentifier;
  @Schema(description = ORG_PARAM_MESSAGE) String orgIdentifier;
  @Schema(description = PROJECT_PARAM_MESSAGE) String projectIdentifier;
  @NotNull @Schema(description = IDENTIFIER_PARAM_MESSAGE) String identifier;
}
