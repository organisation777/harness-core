/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.notification.service;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.expression.EngineExpressionEvaluator.EXPR_END;
import static io.harness.expression.EngineExpressionEvaluator.EXPR_START;
import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.ng.core.dto.UserGroupFilterDTO;
import io.harness.ng.core.notification.NotificationSettingConfigDTO;
import io.harness.ng.core.user.UserInfo;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.NotificationRequest;
import io.harness.notification.SmtpConfig;
import io.harness.notification.entities.NotificationSetting;
import io.harness.notification.evaluator.SecretExpressionEvaluator;
import io.harness.notification.remote.SmtpConfigClient;
import io.harness.notification.remote.SmtpConfigResponse;
import io.harness.notification.repositories.NotificationSettingRepository;
import io.harness.notification.service.api.NotificationSettingsService;
import io.harness.remote.client.RestClientUtils;
import io.harness.user.remote.UserClient;
import io.harness.user.remote.UserFilterNG;
import io.harness.usergroups.UserGroupClient;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class NotificationSettingsServiceImpl implements NotificationSettingsService {
  private final UserGroupClient userGroupClient;
  private final UserClient userClient;
  private final NotificationSettingRepository notificationSettingRepository;
  private final SmtpConfigClient smtpConfigClient;
  private static final Pattern VALID_EXPRESSION_PATTERN = Pattern.compile("\\<\\+secrets.getValue\\(\\'\\w*\\'\\)>");

  private List<UserGroupDTO> getUserGroups(List<String> userGroupIds) {
    if (isEmpty(userGroupIds)) {
      return new ArrayList<>();
    }
    List<UserGroupDTO> userGroups = new ArrayList<>();
    try {
      UserGroupFilterDTO userGroupFilterDTO =
          UserGroupFilterDTO.builder().databaseIdFilter(new HashSet<>(userGroupIds)).build();
      userGroups = getResponse(userGroupClient.getFilteredUserGroups(userGroupFilterDTO));
    } catch (Exception ex) {
      log.error("Error while fetching user groups.", ex);
    }
    return userGroups;
  }

  private List<UserGroupDTO> getUserGroups(List<NotificationRequest.UserGroup> userGroups, String accountIdentifier) {
    if (isEmpty(userGroups)) {
      return new ArrayList<>();
    }
    List<UserGroupDTO> userGroupDTOS = new ArrayList<>();
    try {
      List<UserGroupFilterDTO> userGroupFilterDTO =
          userGroups.stream()
              .map(userGroup -> {
                String orgIdentifier = isEmpty(userGroup.getOrgIdentifier()) ? null : userGroup.getOrgIdentifier();
                String projectIdentifier =
                    isEmpty(userGroup.getProjectIdentifier()) ? null : userGroup.getProjectIdentifier();
                return UserGroupFilterDTO.builder()
                    .accountIdentifier(accountIdentifier)
                    .identifierFilter(Sets.newHashSet(ImmutableList.of(userGroup.getIdentifier())))
                    .orgIdentifier(orgIdentifier)
                    .projectIdentifier(projectIdentifier)
                    .build();
              })
              .collect(Collectors.toList());

      for (UserGroupFilterDTO filterDTO : userGroupFilterDTO) {
        userGroupDTOS.addAll(getResponse(userGroupClient.getFilteredUserGroups(filterDTO)));
      }

    } catch (Exception ex) {
      log.error("Error while fetching user groups.", ex);
    }
    return userGroupDTOS;
  }

  private List<String> getEmailsForUserIds(List<String> userIds, String accountId) {
    if (isEmpty(userIds)) {
      return new ArrayList<>();
    }
    List<UserInfo> users = new ArrayList<>();
    try {
      users =
          RestClientUtils.getResponse(userClient.listUsers(accountId, UserFilterNG.builder().userIds(userIds).build()));
    } catch (Exception exception) {
      log.error("Failure while fetching emails of users from userIds", exception);
    }
    return users.stream().map(UserInfo::getEmail).collect(Collectors.toList());
  }

  @Override
  public List<String> getNotificationRequestForUserGroups(List<NotificationRequest.UserGroup> notificationUserGroups,
      NotificationChannelType notificationChannelType, String accountId, long expressionFunctorToken) {
    List<UserGroupDTO> userGroups = getUserGroups(notificationUserGroups, accountId);
    List<String> notificationSetting = getNotificationSettings(notificationChannelType, userGroups, accountId);
    return resolveUserGroups(notificationChannelType, notificationSetting, expressionFunctorToken);
  }

  @Override
  public List<String> getNotificationSettingsForGroups(
      List<String> userGroupIds, NotificationChannelType notificationChannelType, String accountId) {
    // get user groups by ids
    List<UserGroupDTO> userGroups = getUserGroups(userGroupIds);
    return getNotificationSettings(notificationChannelType, userGroups, accountId);
  }

  private List<String> getNotificationSettings(
      NotificationChannelType notificationChannelType, List<UserGroupDTO> userGroups, String accountId) {
    Set<String> notificationSettings = new HashSet<>();
    for (UserGroupDTO userGroupDTO : userGroups) {
      for (NotificationSettingConfigDTO notificationSettingConfigDTO : userGroupDTO.getNotificationConfigs()) {
        if (notificationSettingConfigDTO.getType() == notificationChannelType
            && notificationSettingConfigDTO.getSetting().isPresent()) {
          notificationSettings.add(notificationSettingConfigDTO.getSetting().get());
        }
      }
    }
    return Lists.newArrayList(notificationSettings);
  }

  @VisibleForTesting
  List<String> resolveUserGroups(
      NotificationChannelType notificationChannelType, List<String> notificationSetting, long expressionFunctorToken) {
    if (notificationChannelType.equals(NotificationChannelType.EMAIL)) {
      return notificationSetting;
    } else {
      if (!notificationSetting.isEmpty()) {
        if (notificationSetting.get(0).startsWith(EXPR_START) && notificationSetting.get(0).endsWith(EXPR_END)) {
          log.info("Resolving UserGroup expression: {}" , notificationSetting.get(0));
          if (!VALID_EXPRESSION_PATTERN.matcher(notificationSetting.get(0)).matches()) {
            throw new InvalidRequestException("Expression provided is not valid");
          }
          log.info("Resolving UserGroup expression");
          SecretExpressionEvaluator evaluator = new SecretExpressionEvaluator(expressionFunctorToken);
          Object resolvedExpressions = evaluator.resolve(notificationSetting, true);
          if (resolvedExpressions == null) {
            throw new InvalidRequestException("Expression provided is not valid");
          }
          log.info("Parsed Expression {}", resolvedExpressions);
          return (List<String>) resolvedExpressions;
        } else {
          return notificationSetting;
        }
      } else {
        return notificationSetting;
      }
    }
  }

  @Override
  public Optional<NotificationSetting> getNotificationSetting(String accountId) {
    return notificationSettingRepository.findByAccountId(accountId);
  }

  @Override
  public boolean getSendNotificationViaDelegate(String accountId) {
    Optional<NotificationSetting> notificationSettingOptional =
        notificationSettingRepository.findByAccountId(accountId);
    return notificationSettingOptional.map(NotificationSetting::isSendNotificationViaDelegate).orElse(false);
  }

  @Override
  public Optional<SmtpConfig> getSmtpConfig(String accountId) {
    Optional<NotificationSetting> notificationSettingOptional =
        notificationSettingRepository.findByAccountId(accountId);
    return Optional.ofNullable(notificationSettingOptional.map(NotificationSetting::getSmtpConfig).orElse(null));
  }

  @Override
  public SmtpConfigResponse getSmtpConfigResponse(String accountId) {
    SmtpConfigResponse smtpConfigResponse = null;
    try {
      smtpConfigResponse = RestClientUtils.getResponse(smtpConfigClient.getSmtpConfig(accountId));
    } catch (Exception ex) {
      log.error("Rest call for getting smtp config failed: ", ex);
    }
    return smtpConfigResponse;
  }

  @Override
  public NotificationSetting setSendNotificationViaDelegate(String accountId, boolean sendNotificationViaDelegate) {
    Optional<NotificationSetting> notificationSettingOptional =
        notificationSettingRepository.findByAccountId(accountId);
    NotificationSetting notificationSetting =
        notificationSettingOptional.orElse(NotificationSetting.builder().accountId(accountId).build());
    notificationSetting.setSendNotificationViaDelegate(sendNotificationViaDelegate);
    notificationSettingRepository.save(notificationSetting);
    return notificationSetting;
  }

  @Override
  public NotificationSetting setSmtpConfig(String accountId, SmtpConfig smtpConfig) {
    Optional<NotificationSetting> notificationSettingOptional =
        notificationSettingRepository.findByAccountId(accountId);
    NotificationSetting notificationSetting =
        notificationSettingOptional.orElse(NotificationSetting.builder().accountId(accountId).build());
    notificationSetting.setSmtpConfig(smtpConfig);
    notificationSettingRepository.save(notificationSetting);
    return notificationSetting;
  }
}
