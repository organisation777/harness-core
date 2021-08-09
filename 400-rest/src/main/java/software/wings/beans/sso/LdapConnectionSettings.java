package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.helpers.ext.ldap.LdapConnectionConfig;
import software.wings.helpers.ext.ldap.LdapConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

/**
 * Class denotes Connection settings for Ldap.
 */
@OwnedBy(PL)
@JsonIgnoreProperties(value = {"encryptedBindPassword"})
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapConnectionSettings implements LdapConnectionConfig {
  @NotNull String host;
  int port = LdapConstants.DEFAULT_CONNECTION_PORT;
  boolean sslEnabled = LdapConstants.DEFAULT_SSL_STATE;
  boolean referralsEnabled;
  @Min(1) int maxReferralHops = LdapConstants.DEFAULT_MAX_REFERRAL_HOPS;
  String bindDN = "";
  String bindPassword = "";
  String encryptedBindPassword;
  int connectTimeout = LdapConstants.DEFAULT_CONNECT_TIMEOUT;
  int responseTimeout = LdapConstants.DEFAULT_RESPONSE_TIMEOUT;

  @AssertTrue(message = "Bind password can't be empty if Bind DN is provided.")
  private boolean isNonEmptyCredentials() {
    if (StringUtils.isNotBlank(bindDN)) {
      return StringUtils.isNotBlank(bindPassword);
    }
    return StringUtils.isBlank(bindPassword);
  }

  /**
   * API to fetch ldap server url based on given host and port.
   * @return
   */
  @Override
  public String generateUrl() {
    return String.format("%s://%s:%d", sslEnabled ? "ldaps" : "ldap", host, port);
  }
}
