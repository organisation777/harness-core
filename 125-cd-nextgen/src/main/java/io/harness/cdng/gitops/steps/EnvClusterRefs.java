package io.harness.cdng.gitops.steps;

import io.harness.annotation.RecasterAlias;

import java.util.Collection;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("envClusterRefs")
@RecasterAlias("io.harness.cdng.gitops.steps.EnvClusterRefs")
public class EnvClusterRefs {
  private String envRef;
  private Collection<String> clusterRefs;
  boolean deployToAll;
}
