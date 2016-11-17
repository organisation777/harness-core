package software.wings.service.impl;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCodes.COMMAND_DOES_NOT_EXIST;
import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.command.CommandUnitType.COMMAND;

import com.google.inject.Inject;

import software.wings.beans.Activity;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Event.Type;
import software.wings.beans.command.AbstractCommandUnit.ExecutionResult;
import software.wings.beans.command.CleanupCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.InitCommandUnit;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Singleton
@ValidateOnExecution
public class ActivityServiceImpl implements ActivityService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private LogService logService;
  @Inject private ArtifactService artifactService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private EventEmitter eventEmitter;

  @Override
  public PageResponse<Activity> list(PageRequest<Activity> pageRequest) {
    return wingsPersistence.query(Activity.class, pageRequest);
  }

  @Override
  public Activity get(String id, String appId) {
    Activity activity = wingsPersistence.get(Activity.class, appId, id);
    if (activity == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Activity doesn't exist");
    }
    return activity;
  }

  @Override
  public Activity save(Activity activity) {
    wingsPersistence.save(activity);
    if (isNotBlank(activity.getServiceInstanceId())) {
      serviceInstanceService.updateActivity(activity);
    }
    eventEmitter.send(Channel.ACTIVITIES,
        anEvent()
            .withType(Type.CREATE)
            .withUuid(activity.getUuid())
            .withAppId(activity.getAppId())
            .withEnvId(activity.getEnvironmentId())
            .build());
    return activity;
  }

  @Override
  public void updateStatus(String activityId, String appId, ExecutionStatus status) {
    wingsPersistence.update(
        wingsPersistence.createQuery(Activity.class).field(ID_KEY).equal(activityId).field("appId").equal(appId),
        wingsPersistence.createUpdateOperations(Activity.class).set("status", status));
    Activity activity = get(activityId, appId);
    if (isNotBlank(activity.getServiceInstanceId())) {
      serviceInstanceService.updateActivity(activity);
    }
    eventEmitter.send(Channel.ACTIVITIES,
        anEvent()
            .withType(Type.UPDATE)
            .withUuid(activity.getUuid())
            .withAppId(activity.getAppId())
            .withEnvId(activity.getEnvironmentId())
            .build());
  }

  @Override
  public List<CommandUnit> getCommandUnits(String appId, String activityId) {
    Activity activity = get(activityId, appId);
    Command command =
        serviceResourceService
            .getCommandByName(appId, activity.getServiceId(), activity.getEnvironmentId(), activity.getCommandName())
            .getCommand();
    List<CommandUnit> commandUnits =
        getFlattenCommandUnitList(appId, activity.getServiceId(), activity.getEnvironmentId(), command);
    if (commandUnits != null && commandUnits.size() > 0) {
      commandUnits.add(0, new InitCommandUnit());
      commandUnits.add(new CleanupCommandUnit());
      boolean markNextQueued = false;
      for (CommandUnit commandUnit : commandUnits) {
        ExecutionResult executionResult = ExecutionResult.QUEUED;
        if (!markNextQueued) {
          executionResult = logService.getUnitExecutionResult(appId, activityId, commandUnit.getName());
          if (executionResult == ExecutionResult.FAILURE || executionResult == ExecutionResult.RUNNING) {
            markNextQueued = true;
          }
        }
        commandUnit.setExecutionResult(executionResult);
      }
    }
    return commandUnits;
  }

  /**
   * Gets flatten command unit list.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param command   the command
   * @return the flatten command unit list
   */
  private List<CommandUnit> getFlattenCommandUnitList(String appId, String serviceId, String envId, Command command) {
    Command executableCommand = command;
    if (executableCommand == null) {
      return new ArrayList<>();
    }

    if (isNotBlank(command.getReferenceId())) {
      executableCommand =
          serviceResourceService.getCommandByName(appId, serviceId, envId, command.getReferenceId()).getCommand();
      if (executableCommand == null) {
        throw new WingsException(COMMAND_DOES_NOT_EXIST);
      }
    }

    return executableCommand.getCommandUnits()
        .stream()
        .flatMap(commandUnit -> {
          if (COMMAND.equals(commandUnit.getCommandUnitType())) {
            return getFlattenCommandUnitList(appId, serviceId, envId, (Command) commandUnit).stream();
          } else {
            return Stream.of(commandUnit);
          }
        })
        .collect(toList());
  }

  @Override
  public Activity getLastActivityForService(String appId, String serviceId) {
    return wingsPersistence.createQuery(Activity.class)
        .field("appId")
        .equal(appId)
        .field("serviceId")
        .equal(serviceId)
        .get();
  }

  @Override
  public Activity getLastProductionActivityForService(String appId, String serviceId) {
    return wingsPersistence.createQuery(Activity.class)
        .field("appId")
        .equal(appId)
        .field("serviceId")
        .equal(serviceId)
        .field("environmentType")
        .equal(EnvironmentType.PROD)
        .get();
  }

  @Override
  public boolean delete(String appId, String activityId) {
    boolean deleted = wingsPersistence.delete(
        wingsPersistence.createQuery(Activity.class).field("appId").equal(appId).field(ID_KEY).equal(activityId));
    if (deleted) {
      logService.deleteActivityLogs(appId, activityId);
    }
    return deleted;
  }

  @Override
  public void deleteByEnvironment(String appId, String envId) {
    wingsPersistence.createQuery(Activity.class)
        .field("appId")
        .equal(appId)
        .field("environmentId")
        .equal(envId)
        .asKeyList()
        .forEach(activityKey -> delete(appId, (String) activityKey.getId()));
  }
}
