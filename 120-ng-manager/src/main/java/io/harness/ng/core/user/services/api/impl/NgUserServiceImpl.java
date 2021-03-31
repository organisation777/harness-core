package io.harness.ng.core.user.services.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.remote.client.NGRestUtils.getResponse;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.ng.core.invites.entities.UserMembership;
import io.harness.ng.core.invites.entities.UserMembership.Scope;
import io.harness.ng.core.invites.entities.UserMembership.Scope.ScopeKeys;
import io.harness.ng.core.invites.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.ng.core.user.remote.UserSearchFilter;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.repositories.invites.spring.UserMembershipRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@Slf4j
@OwnedBy(PL)
public class NgUserServiceImpl implements NgUserService {
  private static final String ORG_VIEWER_ROLE_IDENTIFIER = "_organization_viewer";
  private static final String ACCOUNT_VIEWER_ROLE_IDENTIFIER = "_account_viewer";
  private final UserClient userClient;
  private final UserMembershipRepository userMembershipRepository;
  private final AccessControlAdminClient accessControlAdminClient;

  @Inject
  public NgUserServiceImpl(UserClient userClient, UserMembershipRepository userMembershipRepository,
      AccessControlAdminClient accessControlAdminClient) {
    this.userClient = userClient;
    this.userMembershipRepository = userMembershipRepository;
    this.accessControlAdminClient = accessControlAdminClient;
  }

  @Override
  public Page<UserInfo> list(String accountIdentifier, String searchString, Pageable pageable) {
    PageResponse<UserInfo> userPageResponse = RestClientUtils.getResponse(userClient.list(
        accountIdentifier, String.valueOf(pageable.getOffset()), String.valueOf(pageable.getPageSize()), searchString));
    List<UserInfo> users = userPageResponse.getResponse();
    return new PageImpl<>(users, pageable, users.size());
  }

  @Override
  public List<String> listUsersAtScope(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Criteria criteria = Criteria.where(UserMembershipKeys.scopes + "." + ScopeKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(UserMembershipKeys.scopes + "." + ScopeKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(UserMembershipKeys.scopes + "." + ScopeKeys.projectIdentifier)
                            .is(projectIdentifier);
    Page<UserMembership> userMembershipPage = userMembershipRepository.findAll(criteria, Pageable.unpaged());
    return userMembershipPage.getContent().stream().map(UserMembership::getUserId).collect(toList());
  }

  @Override
  public List<UserMembership> listUserMemberships(Criteria criteria) {
    return userMembershipRepository.findAll(criteria);
  }

  public Optional<UserInfo> getUserFromEmail(String email, String accountIdentifier) {
    List<UserInfo> users = getUsersFromEmail(Collections.singletonList(email), accountIdentifier);
    if (isEmpty(users)) {
      return Optional.empty();
    }
    return Optional.of(users.get(0));
  }

  @Override
  public List<UserInfo> getUsersFromEmail(List<String> emailIds, String accountId) {
    return RestClientUtils.getResponse(
        userClient.listUsers(UserSearchFilter.builder().emailIds(emailIds).build(), accountId));
  }

  @Override
  public List<String> getUsersHavingRole(Scope scope, String roleIdentifier) {
    List<RoleAssignmentResponseDTO> roleAssignmentResponses =
        getResponse(accessControlAdminClient.getFilteredRoleAssignments(scope.getAccountIdentifier(),
                        scope.getOrgIdentifier(), scope.getProjectIdentifier(), 0, 1000,
                        RoleAssignmentFilterDTO.builder().roleFilter(Collections.singleton(roleIdentifier)).build()))
            .getContent();
    return roleAssignmentResponses.stream()
        .filter(roleAssignmentResponse
            -> roleAssignmentResponse.getRoleAssignment().getPrincipal().getType().equals(PrincipalType.USER))
        .map(roleAssignmentResponse -> roleAssignmentResponse.getRoleAssignment().getPrincipal().getIdentifier())
        .collect(toList());
  }

  @Override
  public Optional<UserMembership> getUserMembership(String userId) {
    return userMembershipRepository.findDistinctByUserId(userId);
  }

  @Override
  public void addUserToScope(UserInfo user, Scope scope) {
    addUserToScope(user.getUuid(), user.getEmail(), scope, true);
  }

  @Override
  public void addUserToScope(UserInfo user, Scope scope, boolean postCreation) {
    addUserToScope(user.getUuid(), user.getEmail(), scope, postCreation);
  }

  @Override
  public void addUserToScope(String userId, String emailId, Scope scope, boolean postCreation) {
    Optional<UserMembership> userMembershipOptional = userMembershipRepository.findDistinctByUserId(userId);

    UserMembership userMembership = userMembershipOptional.orElseGet(
        () -> UserMembership.builder().userId(userId).emailId(emailId).scopes(new ArrayList<>()).build());
    if (!userMembership.getScopes().contains(scope)) {
      userMembership.getScopes().add(scope);
    }
    userMembershipRepository.save(userMembership);
    if (postCreation) {
      postCreation(userId, scope);
    }
  }

  private void postCreation(String userId, Scope scope) {
    //    Adding user to the account for signin flow to work
    try {
      RestClientUtils.getResponse(userClient.addUserToAccount(userId, scope.getAccountIdentifier()));
    } catch (Exception e) {
      log.error("Couldn't add user to the account", e);
    }

    //  Adding user to the parent scopes as well
    if (!isBlank(scope.getProjectIdentifier())) {
      String orgDefaultResourceGroup = String.format("_%s", scope.getOrgIdentifier());
      RoleAssignmentDTO roleAssignmentDTO =
          RoleAssignmentDTO.builder()
              .principal(PrincipalDTO.builder().type(PrincipalType.USER).identifier(userId).build())
              .resourceGroupIdentifier(orgDefaultResourceGroup)
              .disabled(false)
              .roleIdentifier(ORG_VIEWER_ROLE_IDENTIFIER)
              .build();

      try {
        NGRestUtils.getResponse(accessControlAdminClient.createRoleAssignment(
            scope.getAccountIdentifier(), scope.getOrgIdentifier(), null, roleAssignmentDTO));
      } catch (Exception e) {
        log.error("Could add user to org scope as org viewer", e);
      }
    }

    if (!isBlank(scope.getOrgIdentifier())) {
      String accountDefaultResourceGroup = String.format("_%s", scope.getAccountIdentifier());
      RoleAssignmentDTO roleAssignmentDTO =
          RoleAssignmentDTO.builder()
              .principal(PrincipalDTO.builder().type(PrincipalType.USER).identifier(userId).build())
              .resourceGroupIdentifier(accountDefaultResourceGroup)
              .disabled(false)
              .roleIdentifier(ACCOUNT_VIEWER_ROLE_IDENTIFIER)
              .build();

      try {
        NGRestUtils.getResponse(
            accessControlAdminClient.createRoleAssignment(scope.getAccountIdentifier(), null, null, roleAssignmentDTO));
      } catch (Exception e) {
        log.error("Couldn't add user to the account scope", e);
      }
    }
  }

  @Override
  public void addUserToScope(String userId, Scope scope) {
    Optional<UserInfo> userOptional = getUserById(userId);
    if (!userOptional.isPresent()) {
      return;
    }
    UserInfo user = userOptional.get();
    addUserToScope(user.getUuid(), user.getEmail(), scope, true);
  }

  @Override
  public List<UserInfo> getUsersByIds(List<String> userIds, String accountId) {
    return RestClientUtils.getResponse(
        userClient.listUsers(UserSearchFilter.builder().userIds(userIds).build(), accountId));
  }

  @Override
  public Optional<UserInfo> getUserById(String userId) {
    return RestClientUtils.getResponse(userClient.getUserById(userId));
  }

  @Override
  public boolean isUserInAccount(String accountId, String userId) {
    return Boolean.TRUE.equals(RestClientUtils.getResponse(userClient.isUserInAccount(accountId, userId)));
  }

  @Override
  public void removeUserFromScope(
      String userId, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<UserMembership> userMembershipOptional = getUserMembership(userId);
    if (!userMembershipOptional.isPresent()) {
      return;
    }
    UserMembership userMembership = userMembershipOptional.get();
    Scope scope = Scope.builder()
                      .accountIdentifier(accountIdentifier)
                      .orgIdentifier(orgIdentifier)
                      .projectIdentifier(projectIdentifier)
                      .build();
    List<Scope> scopes = userMembership.getScopes();
    if (!scopes.contains(scope)) {
      return;
    }
    scopes.remove(scope);
    boolean isUserRemovedFromAccount =
        scopes.stream().noneMatch(scope1 -> scope1.getAccountIdentifier().equals(accountIdentifier));
    if (isUserRemovedFromAccount) {
      RestClientUtils.getResponse(userClient.safeDeleteUser(userId, accountIdentifier));
    }
  }

  @Override
  public boolean removeUserMembership(String userId) {
    return userMembershipRepository.deleteUserMembershipByUserId(userId) > 0;
  }
}
