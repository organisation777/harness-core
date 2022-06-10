package io.harness.ng.oauth;

import static java.lang.String.format;

import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.ValueType;

import com.google.inject.Inject;
import java.util.Date;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class OauthSecretService {
  @Inject GithubOauthClient githubOauthClient;
  @Inject SecretCrudService ngSecretService;
  @Inject GitlabOauthClient gitlabOauthClient;
  @Inject BitbucketOauthClient bitbucketOauthClient;

  String oauthAccessTokenSecretName = "harnessoauthaccesstoken_%s_%s";
  String oauthRefreshTokenSecretName = "harnessoauthsecrettoken_%s_%s";

  public OauthClient getOauthProvider(String oauthProvider) {
    switch (oauthProvider) {
      case "github":
        return githubOauthClient;
      case "bitbucket":
        return bitbucketOauthClient;
      case "gitlab":
        return gitlabOauthClient;
      default:
        throw new InvalidRequestException(format("Oauth provider %s not supported.", oauthProvider));
    }
  }

  public OauthSecretResponse createSecrets(String provider, String code, String state, String accountIdentifier) {
    OauthClient oauthProvider = getOauthProvider(provider);
    OauthAccessToken oauthAccessToken = oauthProvider.execute(code, state, accountIdentifier);
    SecretTextSpecDTO accessTokenSecretDTO = SecretTextSpecDTO.builder()
                                                 .secretManagerIdentifier("harnessSecretManager")
                                                 .value(oauthAccessToken.getAccessToken())
                                                 .valueType(ValueType.Inline)
                                                 .build();
    SecretTextSpecDTO refreshTokenSecretDTO = SecretTextSpecDTO.builder()
                                                  .secretManagerIdentifier("harnessSecretManager")
                                                  .value(oauthAccessToken.getRefreshToken())
                                                  .valueType(ValueType.Inline)
                                                  .build();
    SecretResponseWrapper accessTokenResponse = ngSecretService.create(accountIdentifier,
        SecretDTOV2.builder()
            .identifier(format(oauthAccessTokenSecretName, provider, (new Date()).getTime()))
            .name("Harness Oauth access token")
            .spec(accessTokenSecretDTO)
            .type(SecretType.SecretText)
            .build());

    // github doesn't provides refresh token
    if (provider.equals("github")) {
      return OauthSecretResponse.builder().accessTokenRef(accessTokenResponse.getSecret().getIdentifier()).build();
    }

    SecretResponseWrapper refreshTokenResponse = ngSecretService.create(accountIdentifier,
        SecretDTOV2.builder()
            .identifier(format(oauthRefreshTokenSecretName, provider, (new Date()).getTime()))
            .name("Harness Oauth refresh token")
            .spec(accessTokenSecretDTO)
            .type(SecretType.SecretText)
            .build());
    return OauthSecretResponse.builder()
        .accessTokenRef(accessTokenResponse.getSecret().getIdentifier())
        .refreshTokenRef(refreshTokenResponse.getSecret().getIdentifier())
        .build();
  }
}
