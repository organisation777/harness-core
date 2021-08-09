package software.wings.beans;

import software.wings.service.intfc.WorkflowService;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.states.EnvResumeState.EnvResumeStateKeys;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoopEnvResumeStateParams implements LoopParams {
  String prevStateExecutionId;
  String prevPipelineExecutionId;
  List<String> prevWorkflowExecutionIds;
  String stageName;

  String stepName;

  public State getEnvStateInstanceFromParams(WorkflowService workflowService, String appId) {
    Map<String, StateTypeDescriptor> stencilMap = workflowService.stencilMap(appId);
    StateTypeDescriptor stateTypeDesc = stencilMap.get(StateType.ENV_RESUME_STATE.getType());

    State state = stateTypeDesc.newInstance(stepName);
    Map<String, Object> properties = new HashMap<>();

    properties.put(EnvResumeStateKeys.prevStateExecutionId, prevStateExecutionId);
    properties.put(EnvResumeStateKeys.prevPipelineExecutionId, prevPipelineExecutionId);
    properties.put(EnvResumeStateKeys.prevWorkflowExecutionIds, prevWorkflowExecutionIds);
    state.parseProperties(properties);
    state.resolveProperties();
    return state;
  }
}
