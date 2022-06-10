package io.harness.ng.oauth;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

public interface OauthClient {
  String getName();

  URI getRedirectUrl();

  OauthAccessToken execute(String code, String state, String accountIdentifier);
}
