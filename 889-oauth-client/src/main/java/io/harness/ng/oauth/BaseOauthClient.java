package io.harness.ng.oauth;

import com.google.inject.Inject;
import io.harness.exception.InvalidArgumentsException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import software.wings.security.JWT_CATEGORY;
import software.wings.security.SecretManager;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseOauthClient {
    static final String STATE_KEY = "state";
    SecretManager secretManager;

    public BaseOauthClient(SecretManager secretManager) {
        this.secretManager = secretManager;
    }

    public URI appendStateToURL(URIBuilder uriBuilder) throws URISyntaxException {
        String jwtSecret = secretManager.generateJWTToken(null, JWT_CATEGORY.OAUTH_REDIRECT);
        log.info("Status appending to oauth url is [{}]", jwtSecret);
        uriBuilder.addParameter(STATE_KEY, jwtSecret);
        return uriBuilder.build();
    }

    public void verifyState(String state) {
        try {
            log.info("The status received is: [{}]", state);
            secretManager.verifyJWTToken(state, JWT_CATEGORY.OAUTH_REDIRECT);
        } catch (Exception ex) {
            log.warn("State verification failed in oauth.", ex);
            throw new InvalidArgumentsException("Oauth failed because of state mismatch");
        }
    }
}
