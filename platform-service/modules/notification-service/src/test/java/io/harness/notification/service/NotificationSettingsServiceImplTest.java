package io.harness.notification.service;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.ADITHYA;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.NotificationRequest;
import io.harness.notification.remote.SmtpConfigClient;
import io.harness.notification.repositories.NotificationSettingRepository;
import io.harness.rule.Owner;
import io.harness.user.remote.UserClient;
import io.harness.usergroups.UserGroupClient;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PL)
public class NotificationSettingsServiceImplTest extends CategoryTest {
  @Mock private UserGroupClient userGroupClient;
  @Mock private UserClient userClient;
  @Mock private NotificationSettingRepository notificationSettingRepository;
  @Mock private SmtpConfigClient smtpConfigClient;
  private NotificationSettingsServiceImpl notificationSettingsService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    notificationSettingsService = new NotificationSettingsServiceImpl(
        userGroupClient, userClient, notificationSettingRepository, smtpConfigClient);
  }

  @Test
  @Owner(developers = ADITHYA)
  @Category(UnitTests.class)
  public void testGetNotificationRequestForUserGroups() {
    //        List<String> getNotificationRequestForUserGroups(List< NotificationRequest.UserGroup>
    //        notificationUserGroups,
    //                NotificationChannelType notificationChannelType, String accountId, long expressionFunctorToken)
    NotificationRequest.UserGroup userGroup1 = NotificationRequest.UserGroup.newBuilder().build();
  }
}
