package io.harness.pms.event.pollingevent;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.event.MessageListener;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.response.TriggerEventResponse;
import io.harness.ngtriggers.helpers.TriggerEventResponseHelper;
import io.harness.pms.triggers.build.eventmapper.BuildTriggerEventMapper;
import io.harness.pms.triggers.webhook.helpers.TriggerEventExecutionHelper;
import io.harness.polling.contracts.PollingResponse;
import io.harness.repositories.spring.TriggerEventHistoryRepository;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class PollingEventStreamListener implements MessageListener {
  private BuildTriggerEventMapper mapper;
  private TriggerEventExecutionHelper triggerEventExecutionHelper;
  private TriggerEventHistoryRepository triggerEventHistoryRepository;

  @Inject
  public PollingEventStreamListener(BuildTriggerEventMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      try {
        PollingResponse response = PollingResponse.parseFrom(message.getMessage().getData());
        WebhookEventMappingResponse webhookEventMappingResponse = mapper.consumeBuildTriggerEvent(response);
        if (!webhookEventMappingResponse.isFailedToFindTrigger()) {
          List<TriggerEventResponse> responses = triggerEventExecutionHelper.processTriggersForActivation(
              webhookEventMappingResponse.getTriggers(), response);
          if (isNotEmpty(responses)) {
            responses.forEach(resp -> triggerEventHistoryRepository.save(TriggerEventResponseHelper.toEntity(resp)));
          }
        }
      } catch (InvalidProtocolBufferException e) {
        throw new InvalidRequestException("Exception in unpacking/processing of WebhookDTO event", e);
      }
    }
    return true;
  }
}
