package io.harness.accesscontrol.principals.usergroups;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.KARAN;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.accesscontrol.AccessControlTestBase;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.principals.PrincipalValidator;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class UserGroupValidatorTest extends AccessControlTestBase {
  private UserGroupService userGroupService;
  private PrincipalValidator principalValidator;

  @Before
  public void setup() {
    userGroupService = mock(UserGroupService.class);
    principalValidator = spy(new UserGroupValidator(userGroupService));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGet() {
    assertEquals(PrincipalType.USER_GROUP, principalValidator.getPrincipalType());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testValidatePrincipalValid() {
    String scopeIdentifier = randomAlphabetic(10);
    String principalIdentifier = randomAlphabetic(11);
    Principal principal =
        Principal.builder().principalType(PrincipalType.USER_GROUP).principalIdentifier(principalIdentifier).build();
    when(userGroupService.get(principalIdentifier, scopeIdentifier))
        .thenReturn(Optional.of(UserGroup.builder().build()));
    assertTrue(principalValidator.validatePrincipal(principal, scopeIdentifier).isValid());
    verify(userGroupService, times(1)).get(principalIdentifier, scopeIdentifier);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testValidatePrincipalInValid() {
    String scopeIdentifier = randomAlphabetic(10);
    String principalIdentifier = randomAlphabetic(11);
    Principal principal =
        Principal.builder().principalType(PrincipalType.USER_GROUP).principalIdentifier(principalIdentifier).build();
    when(userGroupService.get(principalIdentifier, scopeIdentifier)).thenReturn(Optional.empty());
    assertFalse(principalValidator.validatePrincipal(principal, scopeIdentifier).isValid());
    verify(userGroupService, times(1)).get(principalIdentifier, scopeIdentifier);
  }
}