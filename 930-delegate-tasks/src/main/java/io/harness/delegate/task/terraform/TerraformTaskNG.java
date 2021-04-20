package io.harness.delegate.task.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.terraform.handlers.TerraformAbstractTaskHandler;
import io.harness.exception.UnexpectedTypeException;
import io.harness.logging.LogCallback;

import com.google.inject.Inject;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@OwnedBy(CDP)
public class TerraformTaskNG extends AbstractDelegateRunnableTask {
  @Inject private Map<TFTaskType, TerraformAbstractTaskHandler> tfTaskTypeToHandlerMap;

  public TerraformTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    return null;
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    TerraformTaskNGParameters taskParameters = (TerraformTaskNGParameters) parameters;
    CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

    LogCallback logCallback = getLogCallback(
        getLogStreamingTaskClient(), taskParameters.getTaskType().getDisplayName(), true, commandUnitsProgress);

    if (!tfTaskTypeToHandlerMap.containsKey(taskParameters.getTaskType())) {
      throw new UnexpectedTypeException(
          String.format("Unexpected Terraform Task Type: [%s]", taskParameters.getTaskType()));
    }

    TerraformAbstractTaskHandler taskHandler = tfTaskTypeToHandlerMap.get(taskParameters.getTaskType());
    return taskHandler.executeTask(taskParameters, getDelegateId(), getTaskId(), logCallback, commandUnitsProgress);
  }

  public LogCallback getLogCallback(ILogStreamingTaskClient logStreamingTaskClient, String commandUnitName,
      boolean shouldOpenStream, CommandUnitsProgress commandUnitsProgress) {
    return new NGDelegateLogCallback(logStreamingTaskClient, commandUnitName, shouldOpenStream, commandUnitsProgress);
  }
}
