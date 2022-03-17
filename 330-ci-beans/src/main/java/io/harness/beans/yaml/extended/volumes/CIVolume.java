package io.harness.beans.yaml.extended.volumes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.annotation.TypeAlias;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = EmptyDirYaml.class)
@JsonSubTypes({
  @JsonSubTypes.Type(value = EmptyDirYaml.class, name = "EmptyDir")
  , @JsonSubTypes.Type(value = PersistentVolumeClaimYaml.class, name = "PersistentVolumeClaim"),
      @JsonSubTypes.Type(value = HostPathYaml.class, name = "HostPath")
})
public interface CIVolume {
  @TypeAlias("volume_type")
  enum Type {
    @JsonProperty("EmptyDir") EMPTY_DIR("EmptyDir"),
    @JsonProperty("PersistentVolumeClaim") PERSISTENT_VOLUME_CLAIM("PersistentVolumeClaim"),
    @JsonProperty("HostPath") HOST_PATH("HostPath");

    private final String yamlName;

    Type(String yamlName) {
      this.yamlName = yamlName;
    }

    @JsonValue
    public String getYamlName() {
      return yamlName;
    }
  }
  CIVolume.Type getType();
}