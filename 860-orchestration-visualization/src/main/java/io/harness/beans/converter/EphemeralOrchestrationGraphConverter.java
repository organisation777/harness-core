package io.harness.beans.converter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EphemeralOrchestrationGraph;
import io.harness.beans.OrchestrationGraph;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class EphemeralOrchestrationGraphConverter {
  public EphemeralOrchestrationGraph convertFrom(OrchestrationGraph orchestrationGraph) {
    return EphemeralOrchestrationGraph.builder()
        .startTs(orchestrationGraph.getStartTs())
        .endTs(orchestrationGraph.getEndTs())
        .status(orchestrationGraph.getStatus())
        .rootNodeIds(orchestrationGraph.getRootNodeIds())
        .planExecutionId(orchestrationGraph.getPlanExecutionId())
        .adjacencyList(orchestrationGraph.getAdjacencyList())
        .build();
  }
}
