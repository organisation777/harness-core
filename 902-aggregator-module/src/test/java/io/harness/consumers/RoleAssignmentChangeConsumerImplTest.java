package io.harness.consumers;

import static io.harness.rule.OwnerRule.PHOENIKX;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.models.SourceMetadata;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.aggregator.consumers.RoleAssignmentChangeConsumerImpl;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RoleAssignmentChangeConsumerImplTest extends CategoryTest {
  public static final String SOME_RANDOM_ID = "some_random_id";
  private RoleAssignmentChangeConsumerImpl roleAssignmentChangeConsumer;
  private ACLService aclService;
  private ResourceGroupService resourceGroupService;
  private RoleService roleService;

  @Before
  public void setup() {
    aclService = mock(ACLService.class);
    resourceGroupService = mock(ResourceGroupService.class);
    roleService = mock(RoleService.class);
    roleAssignmentChangeConsumer = new RoleAssignmentChangeConsumerImpl(aclService, roleService, resourceGroupService);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleAssignmentCreation() {
    RoleAssignmentDBO roleAssignmentDBO = RoleAssignmentDBO.builder().build();
    when(roleService.get(any(), any(), any()))
        .thenReturn(
            Optional.of(Role.builder()
                            .permissions(Sets.newHashSet(ImmutableList.of("core.secret.create", "core.secret.edit")))
                            .build()));
    when(resourceGroupService.get(any(), any()))
        .thenReturn(Optional.of(ResourceGroup.builder()
                                    .resourceSelectors(Sets.newHashSet(ImmutableList.of("/SECRET/abc", "/SECRET/xyz")))
                                    .build()));
    when(aclService.insertAllIgnoringDuplicates(any())).thenReturn(10L);
    long count = roleAssignmentChangeConsumer.consumeCreateEvent(SOME_RANDOM_ID, roleAssignmentDBO);
    assertThat(count).isEqualTo(10L);
    verify(roleService).get(any(), any(), any());
    verify(resourceGroupService).get(any(), any());
    verify(aclService).insertAllIgnoringDuplicates(any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleAssignmentCreationWhenRoleNotFound() {
    RoleAssignmentDBO roleAssignmentDBO = RoleAssignmentDBO.builder().build();
    when(roleService.get(any(), any(), any())).thenReturn(Optional.empty());
    long count = roleAssignmentChangeConsumer.consumeCreateEvent(SOME_RANDOM_ID, roleAssignmentDBO);
    assertThat(count).isEqualTo(0L);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleAssignmentCreationWhenResourceGroupNotFound() {
    RoleAssignmentDBO roleAssignmentDBO = RoleAssignmentDBO.builder().build();
    when(roleService.get(any(), any(), any()))
        .thenReturn(
            Optional.of(Role.builder()
                            .permissions(Sets.newHashSet(ImmutableList.of("core.secret.create", "core.secret.edit")))
                            .build()));
    when(resourceGroupService.get(any(), any())).thenReturn(Optional.empty());
    long count = roleAssignmentChangeConsumer.consumeCreateEvent(SOME_RANDOM_ID, roleAssignmentDBO);
    assertThat(count).isEqualTo(0L);
  }

  private List<ACL> getAlreadyExistingACLS() {
    return Lists.newArrayList(
        ImmutableList.of(ACL.builder()
                             .roleAssignmentId("roleAssignmentId1")
                             .principalType("USER")
                             .sourceMetadata(SourceMetadata.builder()
                                                 .roleAssignmentIdentifier("roleAssignmentIdentifier")
                                                 .resourceGroupIdentifier("resourceGroupIdentifier")
                                                 .build())
                             .scopeIdentifier("/ACCOUNT/account/ORG/org")
                             .principalIdentifier("qwerty")
                             .permissionIdentifier("core.secret.edit")
                             .resourceSelector("/SECRETS/abcde")
                             .build()));
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleAssignmentUpdationWithRelevantFieldsChanged() {
    RoleAssignmentDBO roleAssignmentDBO = RoleAssignmentDBO.builder().id("xyz").disabled(true).build();
    when(aclService.getByRoleAssignmentId("xyz")).thenReturn(getAlreadyExistingACLS());
    when(aclService.saveAll(anyList())).thenReturn(1L);
    long count = roleAssignmentChangeConsumer.consumeUpdateEvent("xyz", roleAssignmentDBO);
    assertThat(count).isEqualTo(1L);
    verify(aclService).getByRoleAssignmentId(any());
    verify(aclService).saveAll(any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleAssignmentUpdationWithRelevantFieldsNotChanged() {
    long count = roleAssignmentChangeConsumer.consumeUpdateEvent(
        SOME_RANDOM_ID, RoleAssignmentDBO.builder().version(1L).build());
    assertThat(count).isEqualTo(0L);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleAssignmentDeletion() {
    when(aclService.deleteByRoleAssignmentId(any())).thenReturn(100L);
    long count = roleAssignmentChangeConsumer.consumeDeleteEvent(SOME_RANDOM_ID);
    assertThat(count).isEqualTo(100L);
    verify(aclService).deleteByRoleAssignmentId(any());
  }
}
