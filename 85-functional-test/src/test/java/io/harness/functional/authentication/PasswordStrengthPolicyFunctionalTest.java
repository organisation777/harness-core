package io.harness.functional.authentication;

import static io.harness.rule.OwnerRule.UTKARSH;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.exception.WingsException;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.restutils.LoginSettingsUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.loginSettings.LoginSettingsService;
import software.wings.beans.loginSettings.PasswordStrengthPolicy;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PasswordStrengthPolicyFunctionalTest extends AbstractFunctionalTest {
  static final boolean PASSWORD_STRENGTH_POLICY_ENABLED = true;
  static final boolean PASSWORD_STRENGTH_POLICY_DISABLED = false;
  static final int MINIMUM_NUMBER_OF_CHARACTERS = 15;
  static final int MINIMUM_NUMBER_OF_UPPERCASE_CHARACTERS = 1;
  static final int MINIMUM_NUMBER_OF_LOWERCASE_CHARACTERS = 1;
  static final int MINIMUM_NUMBER_OF_SPECIAL_CHARACTERS = 1;
  static final int MINIMUM_NUMBER_OF_DIGITS = 1;
  static final int UPDATED_NUMBER_OF_CHARACTERS = 12;
  PasswordStrengthPolicy passwordStrengthPolicy;
  static String LoginSettingsId;

  @Inject private LoginSettingsService loginSettingsService;

  @Test
  @Owner(emails = UTKARSH, resent = false)
  @Category(FunctionalTests.class)
  public void TC0_setPasswordPolicy() {
    passwordStrengthPolicy = PasswordStrengthPolicy.builder()
                                 .enabled(PASSWORD_STRENGTH_POLICY_ENABLED)
                                 .minNumberOfCharacters(MINIMUM_NUMBER_OF_CHARACTERS)
                                 .minNumberOfUppercaseCharacters(MINIMUM_NUMBER_OF_UPPERCASE_CHARACTERS)
                                 .minNumberOfLowercaseCharacters(MINIMUM_NUMBER_OF_LOWERCASE_CHARACTERS)
                                 .minNumberOfSpecialCharacters(MINIMUM_NUMBER_OF_SPECIAL_CHARACTERS)
                                 .minNumberOfDigits(MINIMUM_NUMBER_OF_DIGITS)
                                 .build();

    LoginSettings loginSettings =
        LoginSettingsUtils.passwordStrengthPolicyUpdate(bearerToken, getAccount().getUuid(), passwordStrengthPolicy);
    assertThat(loginSettings.getUuid()).isNotNull();
    LoginSettingsId = loginSettings.getUuid();

    PasswordStrengthPolicy passwordStrengthPolicyResponse = loginSettings.getPasswordStrengthPolicy();
    assertEquals(passwordStrengthPolicyResponse.isEnabled(), PASSWORD_STRENGTH_POLICY_ENABLED);
    assertEquals(passwordStrengthPolicyResponse.getMinNumberOfCharacters(), MINIMUM_NUMBER_OF_CHARACTERS);
    assertEquals(
        passwordStrengthPolicyResponse.getMinNumberOfUppercaseCharacters(), MINIMUM_NUMBER_OF_UPPERCASE_CHARACTERS);
    assertEquals(
        passwordStrengthPolicyResponse.getMinNumberOfLowercaseCharacters(), MINIMUM_NUMBER_OF_LOWERCASE_CHARACTERS);
    assertEquals(passwordStrengthPolicyResponse.getMinNumberOfDigits(), MINIMUM_NUMBER_OF_DIGITS);
    assertEquals(
        passwordStrengthPolicyResponse.getMinNumberOfSpecialCharacters(), MINIMUM_NUMBER_OF_SPECIAL_CHARACTERS);
  }

  @Test
  @Owner(emails = UTKARSH, resent = false)
  @Category(FunctionalTests.class)
  public void TC1_updatePasswordPolicy() {
    passwordStrengthPolicy = PasswordStrengthPolicy.builder()
                                 .enabled(PASSWORD_STRENGTH_POLICY_ENABLED)
                                 .minNumberOfCharacters(UPDATED_NUMBER_OF_CHARACTERS)
                                 .minNumberOfUppercaseCharacters(MINIMUM_NUMBER_OF_UPPERCASE_CHARACTERS)
                                 .minNumberOfLowercaseCharacters(MINIMUM_NUMBER_OF_LOWERCASE_CHARACTERS)
                                 .minNumberOfSpecialCharacters(MINIMUM_NUMBER_OF_SPECIAL_CHARACTERS)
                                 .minNumberOfDigits(MINIMUM_NUMBER_OF_DIGITS)
                                 .build();

    LoginSettings loginSettings =
        LoginSettingsUtils.passwordStrengthPolicyUpdate(bearerToken, getAccount().getUuid(), passwordStrengthPolicy);
    assertEquals(loginSettings.getUuid(), LoginSettingsId);

    PasswordStrengthPolicy passwordStrengthPolicyResponse = loginSettings.getPasswordStrengthPolicy();
    assertEquals(passwordStrengthPolicyResponse.isEnabled(), PASSWORD_STRENGTH_POLICY_ENABLED);
    assertEquals(passwordStrengthPolicyResponse.getMinNumberOfCharacters(), UPDATED_NUMBER_OF_CHARACTERS);
  }

  @Test
  @Owner(emails = UTKARSH, resent = false)
  @Category(FunctionalTests.class)
  public void TC2_changePasswordSuccess() {
    final String TEST_PASSWORD = "Helloafsddsfasdsas1@";
    assertEquals(loginSettingsService.verifyPasswordStrength(getAccount(), TEST_PASSWORD.toCharArray()), true);
  }

  @Test
  @Owner(emails = UTKARSH, resent = false)
  @Category(FunctionalTests.class)
  public void TC3_changePasswordFailure() {
    final String TEST_PASSWORD = "Helloafsddsfasdsas1";
    try {
      loginSettingsService.verifyPasswordStrength(getAccount(), TEST_PASSWORD.toCharArray());
      fail("Password should not have been accepted");
    } catch (WingsException e) {
      assertEquals(String.valueOf(e),
          String.format("io.harness.exception.WingsException: Password validation checks failed for account :[%s].",
              getAccount().getUuid()));
    }
  }

  @Test
  @Owner(emails = UTKARSH, resent = false)
  @Category(FunctionalTests.class)
  public void TC4_disablePasswordPolicy() {
    passwordStrengthPolicy = PasswordStrengthPolicy.builder().enabled(PASSWORD_STRENGTH_POLICY_DISABLED).build();

    LoginSettings loginSettings =
        LoginSettingsUtils.passwordStrengthPolicyUpdate(bearerToken, getAccount().getUuid(), passwordStrengthPolicy);
    assertEquals(loginSettings.getUuid(), LoginSettingsId);

    PasswordStrengthPolicy passwordStrengthPolicyResponse = loginSettings.getPasswordStrengthPolicy();
    assertEquals(passwordStrengthPolicyResponse.isEnabled(), PASSWORD_STRENGTH_POLICY_DISABLED);
  }
}
