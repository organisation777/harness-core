package io.harness.ng.oauth;

import com.google.inject.Inject;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.ValueType;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class OauthSecretService {
    @Inject GithubOauthClient githubOauthClient;
    @Inject SecretCrudService ngSecretService;

    public GithubOauthClient getOauthProvider(String oauthProvider) {
        switch (oauthProvider) {
            case "github":
                return githubOauthClient;
            case "bitbucket":
                return null;
            case "gitlab":
                return null;
            default:
                throw new InvalidRequestException(String.format("Oauth provider %s not supported.", oauthProvider));
        }
    }

    public OauthSecretResponse createSecrets(String provider, String code, String state, String accountIdentifier) {
        GithubOauthClient oauthProvider = getOauthProvider(provider);
        OauthAccessToken oauthAccessToken = oauthProvider.execute(code, state, accountIdentifier);
        SecretTextSpecDTO accessTokenSecretDTO = SecretTextSpecDTO.builder().secretManagerIdentifier("harnessSecretManager").value(oauthAccessToken.getAccessToken()).valueType(ValueType.Inline).build();
        SecretTextSpecDTO refreshTokenSecretDTO = SecretTextSpecDTO.builder().secretManagerIdentifier("harnessSecretManager").value(oauthAccessToken.getRefreshToken()).valueType(ValueType.Inline).build();
        SecretResponseWrapper accessTokenResponse = ngSecretService.create(accountIdentifier,
                SecretDTOV2.builder().identifier("harnessoauthaccesstoken").name("Harness Oauth access token").spec(accessTokenSecretDTO).type(SecretType.SecretText).build());
        SecretResponseWrapper refreshTokenResponse = ngSecretService.create(accountIdentifier,
                SecretDTOV2.builder().identifier("harnessoauthrefreshtoken").name("Harness Oauth refresh token").spec(accessTokenSecretDTO).type(SecretType.SecretText).build());
        return OauthSecretResponse.builder().accessTokenRef(accessTokenResponse.getSecret().getIdentifier())
                .refreshTokenRef(accessTokenResponse.getSecret().getIdentifier())
                .build();
    }

}
