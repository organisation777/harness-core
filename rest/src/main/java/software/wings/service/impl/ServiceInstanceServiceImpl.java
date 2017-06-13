package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;

import com.mongodb.DuplicateKeyException;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceInstanceService;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 5/26/16.
 */
@ValidateOnExecution
@Singleton
public class ServiceInstanceServiceImpl implements ServiceInstanceService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<ServiceInstance> list(PageRequest<ServiceInstance> pageRequest) {
    return wingsPersistence.query(ServiceInstance.class, pageRequest);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceInstanceService#save(software.wings.beans.ServiceInstance)
   */
  @Override
  public ServiceInstance save(ServiceInstance serviceInstance) {
    return wingsPersistence.saveAndGet(ServiceInstance.class, serviceInstance);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceInstanceService#get(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public ServiceInstance get(String appId, String envId, String instanceId) {
    return wingsPersistence.createQuery(ServiceInstance.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .field(ID_KEY)
        .equal(instanceId)
        .get();
  }

  /* (non-Javadoc)
   * @see
   * software.wings.service.intfc.ServiceInstanceService#updateInstanceMappings(software.wings.beans.ServiceTemplate,
   * java.util.List, java.util.List)
   */
  @Override
  public void updateInstanceMappings(ServiceTemplate template, InfrastructureMapping infraMapping,
      List<Host> addedHosts, List<String> deletedPublicDnsNames) {
    Query<ServiceInstance> deleteQuery = wingsPersistence.createQuery(ServiceInstance.class)
                                             .field("appId")
                                             .equal(template.getAppId())
                                             .field("serviceTemplate")
                                             .equal(template.getUuid())
                                             .field("publicDns")
                                             .hasAnyOf(deletedPublicDnsNames);
    wingsPersistence.delete(deleteQuery);

    addedHosts.forEach(host -> {
      ServiceInstance serviceInstance =
          aServiceInstance()
              .withAppId(template.getAppId())
              .withEnvId(template.getEnvId()) // Fixme: do it one by one and ignore unique constraints failure
              .withServiceTemplate(template)
              .withHost(host)
              .withInfraMappingId(infraMapping.getUuid())
              .withInfraMappingType(infraMapping.getComputeProviderType())
              .build();
      try {
        wingsPersistence.save(serviceInstance);
      } catch (DuplicateKeyException ex) {
        logger.warn("Reinserting an existing service instance ignore");
      }
    });
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceInstanceService#delete(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  public void delete(String appId, String envId, String instanceId) {
    wingsPersistence.delete(wingsPersistence.createQuery(ServiceInstance.class)
                                .field("appId")
                                .equal(appId)
                                .field("envId")
                                .equal(envId)
                                .field(ID_KEY)
                                .equal(instanceId));
  }

  @Override
  public void deleteByEnv(String appId, String envId) {
    wingsPersistence.createQuery(ServiceInstance.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .asList()
        .forEach(serviceInstance -> delete(appId, envId, serviceInstance.getUuid()));
  }

  @Override
  public void deleteByServiceTemplate(String appId, String envId, String templateId) {
    wingsPersistence.createQuery(ServiceInstance.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .field("serviceTemplate")
        .equal(templateId)
        .asList()
        .forEach(serviceInstance -> delete(appId, envId, serviceInstance.getUuid()));
  }

  @Override
  public void updateActivity(Activity activity) {
    Query<ServiceInstance> query = wingsPersistence.createQuery(ServiceInstance.class)
                                       .field("appId")
                                       .equal(activity.getAppId())
                                       .field(ID_KEY)
                                       .equal(activity.getServiceInstanceId());
    UpdateOperations<ServiceInstance> operations = wingsPersistence.createUpdateOperations(ServiceInstance.class);

    if (!isNullOrEmpty(activity.getArtifactId())) {
      operations.set("artifactId", activity.getArtifactId())
          .set("artifactName", activity.getArtifactName())
          .set("artifactStreamId", activity.getArtifactStreamId())
          .set("artifactStreamName", activity.getArtifactStreamName())
          .set("artifactDeployedOn", activity.getCreatedAt())
          .set("artifactDeploymentStatus", activity.getStatus())
          .set("artifactDeploymentActivityId", activity.getUuid());
    }
    operations.set("lastActivityId", activity.getUuid())
        .set("lastActivityStatus", activity.getStatus())
        .set("commandName", activity.getCommandName())
        .set("commandType", activity.getCommandType())
        .set("lastActivityCreatedAt", activity.getCreatedAt());

    if (activity.getType() == Type.Command && !isNullOrEmpty(activity.getArtifactId())) {
      operations.set("lastDeployedOn", activity.getLastUpdatedAt());
    }
    wingsPersistence.update(query, operations);
  }

  @Override
  public void deleteByInfraMappingId(String appId, String infraMappingId) {
    wingsPersistence.delete(wingsPersistence.createQuery(ServiceInstance.class)
                                .field("appId")
                                .equal(appId)
                                .field("infraMappingId")
                                .equal(infraMappingId));
  }
}
