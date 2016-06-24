package software.wings.service;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.Tag.Builder.aTag;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.INFRA_ID;
import static software.wings.utils.WingsTestConstants.TAG_ID;
import static software.wings.utils.WingsTestConstants.TAG_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.Host;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Tag;
import software.wings.beans.Tag.Builder;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.TagService;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 6/24/16.
 */
public class TagServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ServiceInstanceService serviceInstanceService;
  @Mock private HostService hostService;
  @Mock private ServiceTemplateService serviceTemplateService;

  @Inject @InjectMocks private TagService tagService;

  @Mock Query<Tag> query;
  @Mock FieldEnd end;

  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.createQuery(Tag.class)).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
  }

  private Builder getTagBuilder() {
    return aTag().withAppId(APP_ID).withEnvId(ENV_ID).withName(TAG_NAME);
  }

  @Test
  public void shouldListTags() {
    PageResponse<Tag> pageResponse = new PageResponse<>();
    PageRequest<Tag> pageRequest = new PageRequest<>();
    pageResponse.setResponse(asList(getTagBuilder().build()));
    when(wingsPersistence.query(Tag.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<Tag> tags = tagService.list(pageRequest);
    assertThat(tags).containsAll(asList(getTagBuilder().build()));
  }

  @Test
  public void shouldGetTag() {
    tagService.get(APP_ID, ENV_ID, TAG_ID);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
    verify(query).field("_id");
    verify(end).equal(TAG_ID);
    verify(query).get();
  }

  @Test
  public void shouldUpdateTag() {
    Tag tag = getTagBuilder().withUuid(TAG_ID).withDescription("TAG_DESCRIPTION").build();
    tagService.update(tag);
    verify(wingsPersistence).updateFields(Tag.class, TAG_ID, of("name", TAG_NAME, "description", "TAG_DESCRIPTION"));
  }

  @Test
  public void shouldDeleteTag() {
    Tag child = getTagBuilder().withUuid(TAG_ID).withRootTagId("ROOT_TAG_ID").build();
    Tag parent =
        getTagBuilder().withUuid("PARENT_TAG_ID").withRootTagId("ROOT_TAG_ID").withChildren(asList(child)).build();
    when(query.get()).thenReturn(parent).thenReturn(child);

    tagService.delete(parent.getAppId(), parent.getEnvId(), parent.getUuid());
    InOrder inOrder = inOrder(wingsPersistence);
    inOrder.verify(wingsPersistence).delete(parent);
    inOrder.verify(wingsPersistence).delete(child);
  }

  @Test
  public void shouldNotDeleteRootTag() {
    Tag tag =
        getTagBuilder().withUuid("ROOT_TAG_ID").withRootTag(true).withRootTagId(null).withChildren(asList()).build();
    when(query.get()).thenReturn(tag);

    tagService.delete(tag.getAppId(), tag.getEnvId(), tag.getUuid());
    verify(wingsPersistence).createQuery(Tag.class);
    verifyZeroInteractions(wingsPersistence);
  }

  @Test
  public void shouldSaveRootTag() {
    tagService.save(null, getTagBuilder().build());
    verify(wingsPersistence).saveAndGet(Tag.class, getTagBuilder().withRootTag(true).build());
  }

  @Test
  public void shouldSaveNormalTag() {
    Tag savedTag = getTagBuilder().withUuid(TAG_ID).withRootTagId("ROOT_TAG_ID").build();
    Tag parentTag = getTagBuilder()
                        .withUuid("PARENT_TAG_ID")
                        .withRootTagId("ROOT_TAG_ID")
                        .withRootTag(false)
                        .withChildren(asList(savedTag))
                        .build();
    Tag rootTag = getTagBuilder()
                      .withUuid("ROOT_TAG_ID")
                      .withRootTag(true)
                      .withRootTagId(null)
                      .withChildren(asList(parentTag))
                      .build();

    when(wingsPersistence.get(Tag.class, "PARENT_TAG_ID")).thenReturn(parentTag);
    when(wingsPersistence.saveAndGet(Tag.class, getTagBuilder().withRootTagId("ROOT_TAG_ID").build()))
        .thenReturn(savedTag);
    when(query.get()).thenReturn(parentTag).thenReturn(rootTag); // tag->parent->root
    when(serviceTemplateService.getTemplateByMappedTags(asList(parentTag, rootTag)))
        .thenReturn(asList(aServiceTemplate().withUuid(TEMPLATE_ID).build()));

    savedTag = tagService.save("PARENT_TAG_ID", getTagBuilder().build());

    verify(wingsPersistence).saveAndGet(Tag.class, getTagBuilder().withRootTagId("ROOT_TAG_ID").build()); // Tag saved
    verify(wingsPersistence).addToList(Tag.class, "PARENT_TAG_ID", "children", TAG_ID); // parent updated
    verify(serviceTemplateService)
        .addLeafTag(aServiceTemplate().withUuid(TEMPLATE_ID).build(), savedTag); // template updated
  }

  @Test
  public void shouldGetRootConfigTag() {
    tagService.getRootConfigTag(APP_ID, ENV_ID);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
    verify(query).field("rootTag");
    verify(end).equal(true);
    verify(query).get();
  }

  @Test
  public void shouldGetTagsByName() {
    when(query.get())
        .thenReturn(getTagBuilder().withName("TAG_NAME_1").build())
        .thenReturn(getTagBuilder().withName("TAG_NAME_2").build())
        .thenReturn(null);
    List<Tag> tagsByName =
        tagService.getTagsByName(APP_ID, ENV_ID, asList("TAG_NAME_1", "TAG_NAME_2", "INVALID_TAG_NAME"));
    assertThat(tagsByName.size()).isEqualTo(2);
    assertThat(tagsByName.get(0).getName()).isEqualTo("TAG_NAME_1");
    assertThat(tagsByName.get(1).getName()).isEqualTo("TAG_NAME_2");
    verify(query, times(3)).field("appId");
    verify(end, times(3)).equal(APP_ID);
    verify(query, times(3)).field("envId");
    verify(end, times(3)).equal(ENV_ID);
    verify(query, times(3)).field("name");
    InOrder inOrder = inOrder(end);
    inOrder.verify(end).equal("TAG_NAME_1");
    inOrder.verify(end).equal("TAG_NAME_2");
    inOrder.verify(end).equal("INVALID_TAG_NAME");
    verify(query, times(3)).get();
  }

  @Test
  public void shouldTagHosts() {
    Tag tag = getTagBuilder().withUuid(TAG_ID).build();
    ServiceTemplate serviceTemplate =
        aServiceTemplate().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TEMPLATE_ID).build();
    Host host = Host.HostBuilder.aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withUuid(HOST_ID).build();

    when(query.get()).thenReturn(tag);
    when(serviceTemplateService.getTemplatesByLeafTag(tag)).thenReturn(asList(serviceTemplate));
    when(hostService.getInfraId(APP_ID, ENV_ID)).thenReturn(INFRA_ID);
    when(hostService.getHostsByHostIds(APP_ID, INFRA_ID, asList(HOST_ID))).thenReturn(asList(host));

    tagService.tagHosts(APP_ID, ENV_ID, TAG_ID, asList(HOST_ID));

    verify(hostService).setTags(host, asList(tag));
    verify(serviceInstanceService).updateInstanceMappings(serviceTemplate, asList(host), asList());
  }

  @Test
  public void shouldUnTagHosts() {
    Tag tag = getTagBuilder().withUuid(TAG_ID).build();
    ServiceTemplate serviceTemplate =
        aServiceTemplate().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TEMPLATE_ID).build();
    Host host = Host.HostBuilder.aHost()
                    .withAppId(APP_ID)
                    .withInfraId(INFRA_ID)
                    .withUuid(HOST_ID)
                    .withTags(asList(tag))
                    .build();

    when(query.get()).thenReturn(tag);
    when(serviceTemplateService.getTemplatesByLeafTag(tag)).thenReturn(asList(serviceTemplate));
    when(hostService.getInfraId(APP_ID, ENV_ID)).thenReturn(INFRA_ID);
    when(hostService.getHostsByTags(APP_ID, ENV_ID, asList(tag))).thenReturn(asList(host));

    tagService.tagHosts(APP_ID, ENV_ID, TAG_ID, asList());

    verify(hostService).removeTagFromHost(host, tag);
    verify(serviceInstanceService).updateInstanceMappings(serviceTemplate, asList(), asList(host));
  }

  @Test
  public void shouldTagAndUntagHosts() {
    Tag tag = getTagBuilder().withUuid(TAG_ID).build();
    ServiceTemplate serviceTemplate =
        aServiceTemplate().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TEMPLATE_ID).build();
    Host newHost = Host.HostBuilder.aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withUuid("NEW_HOST_ID").build();
    Host existingHost = Host.HostBuilder.aHost()
                            .withAppId(APP_ID)
                            .withInfraId(INFRA_ID)
                            .withUuid("EXISTING_HOST_ID")
                            .withTags(asList(tag))
                            .build();

    when(query.get()).thenReturn(tag);
    when(serviceTemplateService.getTemplatesByLeafTag(tag)).thenReturn(asList(serviceTemplate));
    when(hostService.getInfraId(APP_ID, ENV_ID)).thenReturn(INFRA_ID);
    when(hostService.getHostsByTags(APP_ID, ENV_ID, asList(tag))).thenReturn(asList(existingHost));
    when(hostService.getHostsByHostIds(APP_ID, INFRA_ID, asList("NEW_HOST_ID"))).thenReturn(asList(newHost));

    tagService.tagHosts(APP_ID, ENV_ID, TAG_ID, asList("NEW_HOST_ID"));

    verify(hostService).setTags(newHost, asList(tag));
    verify(hostService).removeTagFromHost(existingHost, tag);
    verify(serviceInstanceService).updateInstanceMappings(serviceTemplate, asList(newHost), asList());
    verify(serviceInstanceService).updateInstanceMappings(serviceTemplate, asList(), asList(existingHost));
  }

  @Test
  @Ignore
  public void shouldGetLeafTags() {}
}
