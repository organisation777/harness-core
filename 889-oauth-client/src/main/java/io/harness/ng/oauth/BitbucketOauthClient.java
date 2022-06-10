package io.harness.ng.oauth;

import static java.lang.String.format;

import io.harness.exception.InvalidArgumentsException;

import software.wings.security.SecretManager;
import software.wings.security.authentication.oauth.BitbucketConfig;
import software.wings.security.authentication.oauth.ProvidersImpl.Bitbucket;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;

@Slf4j
public class BitbucketOauthClient extends BaseOauthClient implements OauthClient {
  OAuth20Service service;

  public BitbucketOauthClient(SecretManager secretManager, BitbucketConfig config) {
    super(secretManager);
    service = new ServiceBuilder(config.getClientId())
                  .apiSecret(config.getClientSecret())
                  .callback(config.getCallbackUrl())
                  .build(Bitbucket.instance());
  }

  @Override
  public String getName() {
    return "bitbucket";
  }

  @Override
  public URI getRedirectUrl() {
    URIBuilder uriBuilder = null;
    try {
      uriBuilder = new URIBuilder(service.getAuthorizationUrl());
      appendStateToURL(uriBuilder);
      return uriBuilder.build();
    } catch (Exception e) {
      log.info("failed to fetch redirection url for github", e);
      throw new InvalidArgumentsException("get redirection url failed");
    }
  }

  @Override
  public OauthAccessToken execute(String code, String state, String accountIdentifier) {
    verifyState(state);
    OAuth2AccessToken accessToken = null;
    try {
      accessToken = service.getAccessToken(code);
    } catch (Exception e) {
      log.info(format("failed to fetch access token for %s", accountIdentifier));
      throw new InvalidArgumentsException("failed to get oauth access token");
    }
    return OauthAccessToken.builder()
        .accessToken(accessToken.getAccessToken())
        .refreshToken(accessToken.getRefreshToken())
        .build();
  }
}
