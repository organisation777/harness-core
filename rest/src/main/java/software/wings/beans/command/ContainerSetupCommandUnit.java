package software.wings.beans.command;

import com.google.inject.Inject;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

/**
 * Created by brett on 11/18/17
 */
public abstract class ContainerSetupCommandUnit extends AbstractCommandUnit {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  static final int KEEP_N_REVISIONS = 3;

  @Inject @Transient private transient DelegateLogService logService;

  public ContainerSetupCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
    setArtifactNeeded(true);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    SettingAttribute cloudProviderSetting = context.getCloudProviderSetting();
    List<EncryptedDataDetail> cloudProviderCredentials = context.getCloudProviderCredentials();
    ContainerSetupParams setupParams = context.getContainerSetupParams();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(context, getName());
    executionLogCallback.setLogService(logService);

    try {
      context.setCommandExecutionData(executeInternal(cloudProviderSetting, cloudProviderCredentials, setupParams,
          context.getServiceVariables(), executionLogCallback));
      return CommandExecutionStatus.SUCCESS;
    } catch (Exception ex) {
      executionLogCallback.saveExecutionLog(ex.getMessage(), LogLevel.ERROR);
      logger.error(ex.getMessage(), ex);
      if (ex instanceof WingsException) {
        throw ex;
      }
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, ex.getMessage(), ex);
    }
  }

  protected abstract ContainerSetupCommandUnitExecutionData executeInternal(SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, ContainerSetupParams setupParams,
      Map<String, String> serviceVariables, ExecutionLogCallback executionLogCallback);

  @Data
  @EqualsAndHashCode(callSuper = true)
  public abstract static class Yaml extends AbstractCommandUnit.Yaml {
    public Yaml(String commandUnitType) {
      super(commandUnitType);
    }

    public Yaml(String name, String commandUnitType, String deploymentType) {
      super(name, commandUnitType, deploymentType);
    }
  }
}
