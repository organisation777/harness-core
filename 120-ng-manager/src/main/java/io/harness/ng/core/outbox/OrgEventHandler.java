package io.harness.ng.core.outbox;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY;

import io.harness.ModuleType;
import io.harness.audit.ActionConstants;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.entity_crud.organization.OrganizationEntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.auditevent.OrgCreateEvent;
import io.harness.ng.core.auditevent.OrgDeleteEvent;
import io.harness.ng.core.auditevent.OrgRestoreEvent;
import io.harness.ng.core.auditevent.OrgUpdateEvent;
import io.harness.ng.core.dto.OrganizationRequest;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.scope.ResourceScope;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrgEventHandler implements OutboxEventHandler {
  private final ObjectMapper objectMapper;
  private final Producer eventProducer;
  private final AuditClientService auditClientService;
  private final ObjectMapper yamlObjectMapper;

  @Inject
  public OrgEventHandler(ObjectMapper objectMapper, @Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer,
      AuditClientService auditClientService) {
    this.objectMapper = objectMapper;
    this.eventProducer = eventProducer;
    this.auditClientService = auditClientService;
    this.yamlObjectMapper = new ObjectMapper(new YAMLFactory());
  }

  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case "OrgCreated":
          return handleOrgCreateEvent(outboxEvent);
        case "OrgUpdated":
          return handleOrgUpdateEvent(outboxEvent);
        case "OrgDeleted":
          return handleOrgDeleteEvent(outboxEvent);
        case "OrgRestored":
          return handleOrgRestoreEvent(outboxEvent);
        default:
          throw new IllegalArgumentException("Not supported event type {}".format(outboxEvent.getEventType()));
      }
    } catch (IOException ioe) {
      return false;
    }
  }

  private boolean handleOrgCreateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();

    boolean publishedToRedis =
        publishOrganizationChangeEventToRedis(((AccountScope) outboxEvent.getResourceScope()).getAccountIdentifier(),
            outboxEvent.getResource().getIdentifier(), EventsFrameworkMetadataConstants.CREATE_ACTION);
    OrgCreateEvent orgCreateEvent = objectMapper.readValue(outboxEvent.getEventData(), OrgCreateEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(ActionConstants.CREATE_ACTION)
                                .module(ModuleType.CORE)
                                .newYaml(yamlObjectMapper.writeValueAsString(
                                    OrganizationRequest.builder().organization(orgCreateEvent.getOrg()).build()))
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(outboxEvent.getResource())
                                .resourceScope(ResourceScope.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleOrgUpdateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();

    boolean publishedToRedis =
        publishOrganizationChangeEventToRedis(((AccountScope) outboxEvent.getResourceScope()).getAccountIdentifier(),
            outboxEvent.getResource().getIdentifier(), EventsFrameworkMetadataConstants.UPDATE_ACTION);
    OrgUpdateEvent orgUpdateEvent = objectMapper.readValue(outboxEvent.getEventData(), OrgUpdateEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(ActionConstants.UPDATE_ACTION)
                                .module(ModuleType.CORE)
                                .newYaml(orgUpdateEvent.getNewOrg().toString())
                                .oldYaml(orgUpdateEvent.getOldOrg().toString())
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(outboxEvent.getResource())
                                .resourceScope(ResourceScope.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();

    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleOrgDeleteEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();

    boolean publishedToRedis =
        publishOrganizationChangeEventToRedis(((AccountScope) outboxEvent.getResourceScope()).getAccountIdentifier(),
            outboxEvent.getResource().getIdentifier(), EventsFrameworkMetadataConstants.DELETE_ACTION);
    OrgDeleteEvent orgDeleteEvent = objectMapper.readValue(outboxEvent.getEventData(), OrgDeleteEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(ActionConstants.DELETE_ACTION)
                                .module(ModuleType.CORE)
                                .newYaml(yamlObjectMapper.writeValueAsString(
                                    OrganizationRequest.builder().organization(orgDeleteEvent.getOrg()).build()))
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(outboxEvent.getResource())
                                .resourceScope(ResourceScope.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();

    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleOrgRestoreEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();

    boolean publishedToRedis =
        publishOrganizationChangeEventToRedis(((AccountScope) outboxEvent.getResourceScope()).getAccountIdentifier(),
            outboxEvent.getResource().getIdentifier(), EventsFrameworkMetadataConstants.RESTORE_ACTION);
    OrgRestoreEvent orgRestoreEvent = objectMapper.readValue(outboxEvent.getEventData(), OrgRestoreEvent.class);
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(ActionConstants.RESTORE_ACTION)
                                .module(ModuleType.CORE)
                                .newYaml(yamlObjectMapper.writeValueAsString(
                                    OrganizationRequest.builder().organization(orgRestoreEvent.getOrg()).build()))
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(outboxEvent.getResource())
                                .resourceScope(ResourceScope.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();

    return publishedToRedis && auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean publishOrganizationChangeEventToRedis(String accountIdentifier, String identifier, String action) {
    try {
      eventProducer.send(Message.newBuilder()
                             .putAllMetadata(ImmutableMap.of("accountId", accountIdentifier,
                                 EventsFrameworkMetadataConstants.ENTITY_TYPE, ORGANIZATION_ENTITY,
                                 EventsFrameworkMetadataConstants.ACTION, action))
                             .setData(getOrganizationPayload(accountIdentifier, identifier))
                             .build());
    } catch (ProducerShutdownException e) {
      log.error("Failed to send event to events framework orgIdentifier: " + identifier, e);
      return false;
    }
    return true;
  }

  private ByteString getOrganizationPayload(String accountIdentifier, String identifier) {
    return OrganizationEntityChangeDTO.newBuilder()
        .setIdentifier(identifier)
        .setAccountIdentifier(accountIdentifier)
        .build()
        .toByteString();
  }
}
