package io.harness.delegate.beans;

import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ResponseData;
import io.harness.tasks.SerializableResponseData;

import software.wings.beans.SerializationFormat;
import software.wings.beans.TaskType;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class SerializedResponseData implements SerializableResponseData, DelegateResponseData {
  @Inject KryoSerializer kryoSerializer;

  byte[] data;
  TaskType taskType;
  SerializationFormat serializationFormat;

  @Override
  public byte[] serialize() {
    return this.data;
  }

  @Override
  public ResponseData deserialize() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    if (this.serializationFormat.equals(SerializationFormat.JSON)) {
      try {
        return objectMapper.readValue(this.data, this.taskType.getResponse());
      } catch (IOException e) {
        log.error("Could not deserialize bytes to object", e);
      }
    }
    return (ResponseData) kryoSerializer.asInflatedObject(this.data);
  }
}
