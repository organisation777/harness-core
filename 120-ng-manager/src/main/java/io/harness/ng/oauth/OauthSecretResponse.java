package io.harness.ng.oauth;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OauthSecretResponse {
    String accessTokenRef;
    String refreshTokenRef;
}
