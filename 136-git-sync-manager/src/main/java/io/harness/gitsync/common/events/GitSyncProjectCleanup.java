package io.harness.gitsync.common.events;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.PROJECT_ENTITY;

import io.harness.NgAutoLogContext;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.eventhandlers.GitSyncProjectCleanupHandler;
import io.harness.logging.AutoLogContext;
import io.harness.ng.core.event.MessageListener;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class GitSyncProjectCleanup implements MessageListener {
  private final GitSyncProjectCleanupHandler gitSyncProjectCleanupHandler;

  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap != null && metadataMap.get(ENTITY_TYPE) != null) {
        String entityType = metadataMap.get(ENTITY_TYPE);
        if (PROJECT_ENTITY.equals(entityType)) {
          return processProjectChangeEvent(message);
        }
      }
    }
    return true;
  }

  private boolean processProjectChangeEvent(Message message) {
    ProjectEntityChangeDTO projectEntityChangeDTO;
    try {
      projectEntityChangeDTO = ProjectEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(
          String.format("Exception in unpacking ProjectEntityChangeDTO for key %s", message.getId()), e);
    }
    String action = message.getMessage().getMetadataMap().get(ACTION);
    if (DELETE_ACTION.equals(action)) {
      return processProjectDeleteEvent(projectEntityChangeDTO);
    }
    return true;
  }

  private boolean processProjectDeleteEvent(ProjectEntityChangeDTO projectEntityChangeDTO) {
    try (AutoLogContext ignore1 =
             new NgAutoLogContext(projectEntityChangeDTO.getIdentifier(), projectEntityChangeDTO.getOrgIdentifier(),
                 projectEntityChangeDTO.getAccountIdentifier(), AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      return gitSyncProjectCleanupHandler.deleteAssociatedGitEntities(projectEntityChangeDTO.getAccountIdentifier(),
          projectEntityChangeDTO.getOrgIdentifier(), projectEntityChangeDTO.getIdentifier());
    } catch (Exception ex) {
      log.error("Failed to delete all the git entities ", ex);
      return false;
    }
  }
}