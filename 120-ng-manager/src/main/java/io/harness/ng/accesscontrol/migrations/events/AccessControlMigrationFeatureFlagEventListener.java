package io.harness.ng.accesscontrol.migrations.events;

import static io.harness.beans.FeatureName.NG_ACCESS_CONTROL_MIGRATION;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentCreateRequestDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.PageResponse;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.featureflag.FeatureFlagChangeDTO;
import io.harness.exception.DuplicateFieldException;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigration;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigration.AccessControlMigrationBuilder;
import io.harness.ng.accesscontrol.migrations.models.RoleAssignmentMetadata;
import io.harness.ng.accesscontrol.migrations.services.AccessControlMigrationService;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.event.MessageListener;
import io.harness.ng.core.invites.entities.UserMembership.Scope;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.utils.CryptoUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class AccessControlMigrationFeatureFlagEventListener implements MessageListener {
  private static final String ACCOUNT_ADMIN_ROLE_IDENTIFIER = "_account_admin";
  private static final String ORG_ADMIN_ROLE_IDENTIFIER = "_organization_admin";
  private static final String PROJECT_ADMIN_ROLE_IDENTIFIER = "_project_admin";
  private static final String ACCOUNT_VIEWER_ROLE_IDENTIFIER = "_account_viewer";
  private static final String ORG_VIEWER_ROLE_IDENTIFIER = "_organization_viewer";
  private static final String PROJECT_VIEWER_ROLE_IDENTIFIER = "_project_viewer";
  private static final String ACCOUNT_LEVEL = "ACCOUNT";
  private static final String ORGANIZATION_LEVEL = "ORGANIZATION";
  private static final String PROJECT_LEVEL = "PROJECT";
  Map<String, List<String>> levelToRolesMapping;
  private final ProjectService projectService;
  private final OrganizationService orgService;
  private final AccessControlMigrationService accessControlMigrationService;
  private final NgUserService ngUserService;
  private final ResourceGroupClient resourceGroupClient;
  private final AccessControlAdminClient accessControlAdminClient;
  private final UserClient userClient;

  @Inject
  public AccessControlMigrationFeatureFlagEventListener(ProjectService projectService,
      OrganizationService organizationService, AccessControlMigrationService accessControlMigrationService,
      NgUserService ngUserService, ResourceGroupClient resourceGroupClient,
      AccessControlAdminClient accessControlAdminClient, UserClient userClient) {
    this.projectService = projectService;
    this.orgService = organizationService;
    this.accessControlMigrationService = accessControlMigrationService;
    this.ngUserService = ngUserService;
    this.resourceGroupClient = resourceGroupClient;
    this.accessControlAdminClient = accessControlAdminClient;
    this.userClient = userClient;
    levelToRolesMapping = new HashMap<>();
    levelToRolesMapping.put(
        ACCOUNT_LEVEL, ImmutableList.of(ACCOUNT_ADMIN_ROLE_IDENTIFIER, ACCOUNT_VIEWER_ROLE_IDENTIFIER));
    levelToRolesMapping.put(
        ORGANIZATION_LEVEL, ImmutableList.of(ORG_ADMIN_ROLE_IDENTIFIER, ORG_VIEWER_ROLE_IDENTIFIER));
    levelToRolesMapping.put(
        PROJECT_LEVEL, ImmutableList.of(PROJECT_ADMIN_ROLE_IDENTIFIER, PROJECT_VIEWER_ROLE_IDENTIFIER));
  }

  private String getRoleIdentifierForUser(
      UserInfo user, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (user.isAdmin()) {
      return levelToRolesMapping
          .get(getLevel(accountIdentifier, orgIdentifier, projectIdentifier).orElse(ACCOUNT_LEVEL))
          .get(0);
    } else {
      return levelToRolesMapping
          .get(getLevel(accountIdentifier, orgIdentifier, projectIdentifier).orElse(ACCOUNT_LEVEL))
          .get(1);
    }
  }

  private Optional<String> getLevel(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (!StringUtils.isEmpty(projectIdentifier)) {
      return Optional.of(PROJECT_LEVEL);
    }
    if (!StringUtils.isEmpty(orgIdentifier)) {
      return Optional.of(ORGANIZATION_LEVEL);
    }
    if (!StringUtils.isEmpty(accountIdentifier)) {
      return Optional.of(ACCOUNT_LEVEL);
    }
    return Optional.empty();
  }

  private RoleAssignmentMetadata createRoleAssignments(
      String account, String org, String project, List<UserInfo> users) {
    List<RoleAssignmentDTO> roleAssignmentsToCreate = new ArrayList<>();
    for (UserInfo user : users) {
      String roleIdentifier = getRoleIdentifierForUser(user, account, org, project);
      roleAssignmentsToCreate.add(getRoleAssignment(user.getUuid(), roleIdentifier, account, org, project));
    }

    List<RoleAssignmentResponseDTO> createdRoleAssignments =
        NGRestUtils.getResponse(accessControlAdminClient.createMultiRoleAssignment(account, org, project,
            RoleAssignmentCreateRequestDTO.builder().roleAssignments(roleAssignmentsToCreate).build()));

    Set<RoleAssignmentDTO> createdRoleAssignmentsSet =
        createdRoleAssignments.stream().map(RoleAssignmentResponseDTO::getRoleAssignment).collect(Collectors.toSet());

    List<RoleAssignmentDTO> failedRoleAssignments = roleAssignmentsToCreate.stream()
                                                        .filter(x -> !createdRoleAssignmentsSet.contains(x))
                                                        .collect(Collectors.toList());

    return RoleAssignmentMetadata.builder()
        .createdRoleAssignments(createdRoleAssignments)
        .failedRoleAssignments(failedRoleAssignments)
        .build();
  }

  private List<UserInfo> getUsers(String accountId) {
    int offset = 0;
    int limit = 500;
    int maxIterations = 50;
    Set<UserInfo> users = new HashSet<>();
    while (maxIterations > 0) {
      PageResponse<UserInfo> usersPage =
          RestClientUtils.getResponse(userClient.list(accountId, String.valueOf(offset), String.valueOf(limit), null));
      if (isEmpty(usersPage.getResponse())) {
        break;
      }
      users.addAll(usersPage.getResponse());
      maxIterations--;
      offset += limit;
    }
    return new ArrayList<>(users);
  }

  private boolean handleAccessControlMigrationEnabledFlag(String accountId) {
    log.info("Running ng access control migration for account: {}", accountId);
    // get users along with user groups for account
    List<UserInfo> users = getUsers(accountId);
    AccessControlMigrationBuilder migrationBuilder =
        AccessControlMigration.builder().accountId(accountId).startedAt(new Date());

    if (isEmpty(users)) {
      accessControlMigrationService.save(migrationBuilder.endedAt(new Date()).build());
      return true;
    }

    upsertResourceGroup(accountId, null, null);

    // adding account level roles to users
    List<RoleAssignmentMetadata> roleAssignmentMetadataList = new ArrayList<>();
    roleAssignmentMetadataList.add(createRoleAssignments(accountId, null, null, users));
    users.forEach(user -> upsertUserMembership(accountId, null, null, user));

    // adding org level roles to users
    List<Organization> organizations =
        orgService.list(accountId, Pageable.unpaged(), OrganizationFilterDTO.builder().build()).getContent();
    for (Organization organization : organizations) {
      upsertResourceGroup(accountId, organization.getIdentifier(), null);
      roleAssignmentMetadataList.add(createRoleAssignments(accountId, organization.getIdentifier(), null, users));
      users.forEach(user -> upsertUserMembership(accountId, organization.getIdentifier(), null, user));

      // adding project level roles to users
      List<Project> projects = projectService
                                   .list(accountId, Pageable.unpaged(),
                                       ProjectFilterDTO.builder().orgIdentifier(organization.getIdentifier()).build())
                                   .getContent();
      for (Project project : projects) {
        upsertResourceGroup(accountId, organization.getIdentifier(), project.getIdentifier());
        roleAssignmentMetadataList.add(
            createRoleAssignments(accountId, organization.getIdentifier(), project.getIdentifier(), users));

        // adding user project map
        users.forEach(
            user -> upsertUserMembership(accountId, organization.getIdentifier(), project.getIdentifier(), user));
      }
    }
    accessControlMigrationService.save(
        migrationBuilder.endedAt(new Date()).metadata(roleAssignmentMetadataList).build());
    return true;
  }

  private void upsertUserMembership(String accountId, String orgIdentifier, String projectIdentifier, UserInfo user) {
    try {
      Scope scope = Scope.builder()
                        .accountIdentifier(accountId)
                        .projectIdentifier(projectIdentifier)
                        .orgIdentifier(orgIdentifier)
                        .build();
      ngUserService.addUserToScope(user, scope, false);
    } catch (DuplicateKeyException | DuplicateFieldException duplicateException) {
      log.info("User project map already exists account: {}, org: {}, project: {}, principal: {}", accountId,
          orgIdentifier, projectIdentifier, user.getUuid());
    }
  }

  @SneakyThrows
  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      FeatureFlagChangeDTO featureFlagChangeDTO;
      try {
        featureFlagChangeDTO = FeatureFlagChangeDTO.parseFrom(message.getMessage().getData());
      } catch (InvalidProtocolBufferException e) {
        log.error("Unable to parse event into feature flag", e);
        return false;
      }
      if (NG_ACCESS_CONTROL_MIGRATION.equals(FeatureName.valueOf(featureFlagChangeDTO.getFeatureName()))
          && featureFlagChangeDTO.getEnable()) {
        try {
          return handleAccessControlMigrationEnabledFlag(featureFlagChangeDTO.getAccountId());
        } catch (Exception ex) {
          log.error(
              "Error while processing {} feature flag for access control migration", NG_ACCESS_CONTROL_MIGRATION, ex);
          return false;
        }
      }
    }
    return true;
  }

  private RoleAssignmentDTO getRoleAssignment(
      String principalIdentifier, String roleIdentifier, String account, String org, String project) {
    return RoleAssignmentDTO.builder()
        .disabled(false)
        .identifier("role_assignment_".concat(CryptoUtils.secureRandAlphaNumString(20)))
        .roleIdentifier(roleIdentifier)
        .resourceGroupIdentifier(getDefaultResourceGroupIdentifier(account, org, project))
        .principal(PrincipalDTO.builder().identifier(principalIdentifier).type(PrincipalType.USER).build())
        .build();
  }

  private String getDefaultResourceGroupIdentifier(String account, String org, String project) {
    String defaultResourceGroupName;
    if (!isBlank(project)) {
      defaultResourceGroupName = project;
    } else if (!isBlank(org)) {
      defaultResourceGroupName = org;
    } else {
      defaultResourceGroupName = account;
    }
    return String.format("_%s", defaultResourceGroupName);
  }

  private void upsertResourceGroup(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    NGRestUtils.getResponse(
        resourceGroupClient.ensureDefaultResourceGroup(accountIdentifier, orgIdentifier, projectIdentifier));
  }
}
