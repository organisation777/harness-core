package software.wings.service.impl.yaml.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.threading.Morpheus.quietSleep;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.yaml.YamlConstants.ENVIRONMENTS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER_PATH;
import static software.wings.beans.yaml.YamlConstants.VALUES_YAML_KEY;
import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;
import static software.wings.beans.yaml.YamlType.ACCOUNT_DEFAULTS;
import static software.wings.beans.yaml.YamlType.APPLICATION;
import static software.wings.beans.yaml.YamlType.APPLICATION_DEFAULTS;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST_VALUES_ENV_OVERRIDE;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST_VALUES_ENV_SERVICE_OVERRIDE;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST_VALUES_SERVICE_OVERRIDE;
import static software.wings.beans.yaml.YamlType.ARTIFACT_SERVER;
import static software.wings.beans.yaml.YamlType.ARTIFACT_SERVER_ARTIFACT_STREAM_OVERRIDE;
import static software.wings.beans.yaml.YamlType.ARTIFACT_SERVER_OVERRIDE;
import static software.wings.beans.yaml.YamlType.ARTIFACT_STREAM;
import static software.wings.beans.yaml.YamlType.CLOUD_PROVIDER;
import static software.wings.beans.yaml.YamlType.CLOUD_PROVIDER_ARTIFACT_STREAM_OVERRIDE;
import static software.wings.beans.yaml.YamlType.CLOUD_PROVIDER_OVERRIDE;
import static software.wings.beans.yaml.YamlType.COLLABORATION_PROVIDER;
import static software.wings.beans.yaml.YamlType.COMMAND;
import static software.wings.beans.yaml.YamlType.CONFIG_FILE;
import static software.wings.beans.yaml.YamlType.CONFIG_FILE_CONTENT;
import static software.wings.beans.yaml.YamlType.CONFIG_FILE_OVERRIDE;
import static software.wings.beans.yaml.YamlType.CONFIG_FILE_OVERRIDE_CONTENT;
import static software.wings.beans.yaml.YamlType.CV_CONFIGURATION;
import static software.wings.beans.yaml.YamlType.DEPLOYMENT_SPECIFICATION;
import static software.wings.beans.yaml.YamlType.DEPLOYMENT_TRIGGER;
import static software.wings.beans.yaml.YamlType.ENVIRONMENT;
import static software.wings.beans.yaml.YamlType.INFRA_DEFINITION;
import static software.wings.beans.yaml.YamlType.INFRA_MAPPING;
import static software.wings.beans.yaml.YamlType.LOADBALANCER_PROVIDER;
import static software.wings.beans.yaml.YamlType.MANIFEST_FILE;
import static software.wings.beans.yaml.YamlType.MANIFEST_FILE_VALUES_ENV_OVERRIDE;
import static software.wings.beans.yaml.YamlType.MANIFEST_FILE_VALUES_ENV_SERVICE_OVERRIDE;
import static software.wings.beans.yaml.YamlType.MANIFEST_FILE_VALUES_SERVICE_OVERRIDE;
import static software.wings.beans.yaml.YamlType.NOTIFICATION_GROUP;
import static software.wings.beans.yaml.YamlType.PIPELINE;
import static software.wings.beans.yaml.YamlType.PROVISIONER;
import static software.wings.beans.yaml.YamlType.SERVICE;
import static software.wings.beans.yaml.YamlType.TAG;
import static software.wings.beans.yaml.YamlType.TRIGGER;
import static software.wings.beans.yaml.YamlType.VERIFICATION_PROVIDER;
import static software.wings.beans.yaml.YamlType.WORKFLOW;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.Mark;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.scanner.ScannerException;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;
import io.harness.exception.YamlException;
import io.harness.rest.RestResponse;
import io.harness.rest.RestResponse.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import software.wings.beans.Base;
import software.wings.beans.FeatureName;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.YamlProcessingException;
import software.wings.exception.YamlProcessingException.ChangeWithErrorMsg;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.tag.HarnessTagYamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.YamlPayload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author rktummala on 10/16/17
 */
@Singleton
@Slf4j
public class YamlServiceImpl<Y extends BaseYaml, B extends Base> implements YamlService<Y, B> {
  /**
   * We need to evict UserPermissionCache and UserRestrictionCache for some yaml operations.
   * All the rbac*YamlTypes define the entity types in which we have to refresh the cache.
   */
  private static final Set<YamlType> rbacCreateYamlTypes = Sets.newHashSet(YamlType.APPLICATION, YamlType.SERVICE,
      YamlType.ENVIRONMENT, YamlType.PROVISIONER, YamlType.WORKFLOW, YamlType.PIPELINE);
  private static final Set<YamlType> rbacUpdateYamlTypes =
      Sets.newHashSet(YamlType.ENVIRONMENT, YamlType.WORKFLOW, YamlType.PIPELINE);
  private static final Set<YamlType> rbacDeleteYamlTypes = Sets.newHashSet(YamlType.APPLICATION, YamlType.ENVIRONMENT);

  private static final int YAML_MAX_PARALLEL_COUNT = 20;

  @Inject private YamlHandlerFactory yamlHandlerFactory;

  @Inject private transient YamlGitService yamlGitService;
  @Inject private AuthService authService;
  @Inject private ExecutorService executorService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private YamlResourceService yamlResourceService;
  @Inject private WorkflowService workflowService;
  @Inject private AppService appService;
  @Inject private FailedCommitStore failedCommitStore;
  @Inject private FeatureFlagService featureFlagService;

  @Inject private HarnessTagYamlHelper harnessTagYamlHelper;

  private final List<YamlType> yamlProcessingOrder = getEntityProcessingOrder();

  private List<YamlType> getEntityProcessingOrder() {
    return Lists.newArrayList(ACCOUNT_DEFAULTS, TAG, CLOUD_PROVIDER, CLOUD_PROVIDER_OVERRIDE, ARTIFACT_SERVER,
        ARTIFACT_SERVER_OVERRIDE, COLLABORATION_PROVIDER, LOADBALANCER_PROVIDER, VERIFICATION_PROVIDER,
        NOTIFICATION_GROUP, APPLICATION, APPLICATION_DEFAULTS, SERVICE, PROVISIONER, CV_CONFIGURATION, ARTIFACT_STREAM,
        ARTIFACT_SERVER_ARTIFACT_STREAM_OVERRIDE, CLOUD_PROVIDER_ARTIFACT_STREAM_OVERRIDE, COMMAND,
        DEPLOYMENT_SPECIFICATION, CONFIG_FILE_CONTENT, CONFIG_FILE, APPLICATION_MANIFEST, MANIFEST_FILE,
        APPLICATION_MANIFEST_VALUES_SERVICE_OVERRIDE, MANIFEST_FILE_VALUES_SERVICE_OVERRIDE, ENVIRONMENT, INFRA_MAPPING,
        INFRA_DEFINITION, CONFIG_FILE_OVERRIDE_CONTENT, CONFIG_FILE_OVERRIDE, APPLICATION_MANIFEST_VALUES_ENV_OVERRIDE,
        APPLICATION_MANIFEST_VALUES_ENV_SERVICE_OVERRIDE, MANIFEST_FILE_VALUES_ENV_OVERRIDE,
        MANIFEST_FILE_VALUES_ENV_SERVICE_OVERRIDE, WORKFLOW, PIPELINE, TRIGGER, DEPLOYMENT_TRIGGER);
  }

  @Override
  public List<ChangeContext> processChangeSet(List<Change> changeList) throws YamlProcessingException {
    return processChangeSet(changeList, true);
  }

  public List<ChangeContext> processChangeSet(List<Change> changeList, boolean isGitSyncPath)
      throws YamlProcessingException {
    // e.g. remove files outside of setup folder. (checking filePath)
    changeList = filterInvalidFilePaths(changeList);

    // compute the order of processing
    computeProcessingOrder(changeList);
    // validate
    List<ChangeContext> changeContextList = validate(changeList);
    // process in the given order
    process(changeContextList, isGitSyncPath);

    return changeContextList;
  }

  @VisibleForTesting
  List<Change> filterInvalidFilePaths(List<Change> changeList) {
    return changeList.stream().filter(change -> change.getFilePath().startsWith(SETUP_FOLDER_PATH)).collect(toList());
  }

  @Override
  public RestResponse<B> update(YamlPayload yamlPayload, String accountId) {
    GitFileChange change = GitFileChange.Builder.aGitFileChange()
                               .withChangeType(ChangeType.MODIFY)
                               .withFileContent(yamlPayload.getYaml())
                               .withFilePath(yamlPayload.getPath())
                               .withAccountId(accountId)
                               .build();
    RestResponse rr = new RestResponse<>();
    List<GitFileChange> gitFileChangeList = asList(change);

    try {
      List<ChangeContext> changeContextList = processChangeSet(asList(change));
      notNullCheck("Change Context List is null", changeContextList);
      boolean empty = isEmpty(changeContextList);
      if (!empty) {
        // We only sent one

        ChangeContext changeContext = changeContextList.get(0);
        Object base = changeContext.getYamlSyncHandler().get(
            changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
        rr.setResource(base);
        yamlGitService.removeGitSyncErrors(accountId, gitFileChangeList, false);
      } else {
        throw new WingsException(ErrorCode.GENERAL_YAML_ERROR, USER)
            .addParam("message", "Update failed. Reason: " + yamlPayload.getName());
      }

      return rr;
    } catch (YamlProcessingException ex) {
      Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap = ex.getFailedYamlFileChangeMap();
      String errorMsg;
      if (isNotEmpty(failedYamlFileChangeMap)) {
        ChangeWithErrorMsg changeWithErrorMsg = failedYamlFileChangeMap.get(change.getFilePath());
        if (changeWithErrorMsg != null) {
          errorMsg = changeWithErrorMsg.getErrorMsg();
        } else {
          errorMsg = "Internal error";
        }
      } else {
        errorMsg = "Internal error";
      }

      throw new WingsException(ErrorCode.GENERAL_YAML_ERROR, "Update failed. Reason: " + errorMsg, USER)
          .addParam("message", "Update failed. Reason: " + errorMsg);
    }
  }

  @Override
  public RestResponse processYamlFilesAsZip(String accountId, InputStream fileInputStream, String yamlPath) {
    try {
      Future<RestResponse> future = Executors.newSingleThreadExecutor().submit(() -> {
        try {
          List changeList = getChangesForZipFile(accountId, fileInputStream, yamlPath);

          List<ChangeContext> changeSets = processChangeSet(changeList);
          Map<String, Object> metaDataMap = Maps.newHashMap();
          metaDataMap.put("yamlFilesProcessed", changeSets.size());
          return Builder.aRestResponse().withMetaData(metaDataMap).build();
        } catch (YamlProcessingException ex) {
          logger.warn(format("Unable to process zip upload for account %s. ", accountId), ex);
          // gitToHarness is false, as this is not initiated from git
          yamlGitService.processFailedChanges(accountId, ex.getFailedYamlFileChangeMap(), false);
        }
        return Builder.aRestResponse()
            .withResponseMessages(
                asList(new ResponseMessage[] {ResponseMessage.builder().code(ErrorCode.DEFAULT_ERROR_CODE).build()}))
            .build();
      });
      return future.get(30, TimeUnit.SECONDS);
    } catch (Exception e) {
      return Builder.aRestResponse()
          .withResponseMessages(
              asList(new ResponseMessage[] {ResponseMessage.builder().code(ErrorCode.DEFAULT_ERROR_CODE).build()}))
          .build();
    }
  }

  protected List<GitFileChange> getChangesForZipFile(String accountId, InputStream fileInputStream, String yamlPath)
      throws IOException {
    List<GitFileChange> changeList = Lists.newArrayList();
    File tempFile = File.createTempFile(accountId + "_" + System.currentTimeMillis() + "_yaml", ".tmp");
    ZipFile zipFile = null;
    try {
      OutputStream outputStream = new FileOutputStream(tempFile);
      IOUtils.copy(fileInputStream, outputStream);
      outputStream.close();

      zipFile = new ZipFile(tempFile.getAbsoluteFile());

      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        File currFile = new File(entry.getName());
        try {
          if (!currFile.isHidden() && !entry.isDirectory()
              && (entry.getName().endsWith(YAML_EXTENSION)
                     || entry.getName().contains(YamlConstants.CONFIG_FILES_FOLDER))) {
            InputStream stream = zipFile.getInputStream(entry);
            StringWriter writer = new StringWriter();
            IOUtils.copy(stream, writer, "UTF-8");
            GitFileChange change =
                GitFileChange.Builder.aGitFileChange()
                    .withAccountId(accountId)
                    .withChangeType(ChangeType.ADD)
                    .withFileContent(writer.toString())
                    .withFilePath((yamlPath != null ? yamlPath + File.separatorChar : "") + entry.getName())
                    .build();
            changeList.add(change);
          }
        } finally {
          FileUtils.deleteQuietly(currFile);
        }
      }
    } finally {
      if (zipFile != null) {
        zipFile.close();
      }
      FileUtils.deleteQuietly(tempFile);
    }
    return changeList;
  }

  /**
   *
   * @param changeList
   * @throws WingsException
   */
  private void computeProcessingOrder(List<Change> changeList) {
    changeList.sort(new FilePathComparator());
  }

  private ChangeContext validateManifestFile(String yamlFilePath, Change change) {
    ChangeContext.Builder changeContextBuilder = ChangeContext.Builder.aChangeContext().withChange(change);

    if (yamlFilePath.contains(
            YamlConstants.MANIFEST_FOLDER + YamlConstants.PATH_DELIMITER + YamlConstants.MANIFEST_FILE_FOLDER)) {
      changeContextBuilder.withYamlType(YamlType.MANIFEST_FILE)
          .withYamlSyncHandler(yamlHandlerFactory.getYamlHandler(YamlType.MANIFEST_FILE));

      return changeContextBuilder.build();
    } else if (yamlFilePath.contains(YamlConstants.VALUES_FOLDER + YamlConstants.PATH_DELIMITER + VALUES_YAML_KEY)
        && !yamlFilePath.contains(ENVIRONMENTS_FOLDER)) {
      changeContextBuilder.withYamlType(YamlType.MANIFEST_FILE_VALUES_SERVICE_OVERRIDE)
          .withYamlSyncHandler(yamlHandlerFactory.getYamlHandler(YamlType.MANIFEST_FILE_VALUES_SERVICE_OVERRIDE));
      return changeContextBuilder.build();
    } else if (yamlFilePath.contains(YamlConstants.VALUES_FOLDER + YamlConstants.PATH_DELIMITER + VALUES_YAML_KEY)) {
      changeContextBuilder.withYamlType(YamlType.MANIFEST_FILE_VALUES_ENV_OVERRIDE)
          .withYamlSyncHandler(yamlHandlerFactory.getYamlHandler(YamlType.MANIFEST_FILE_VALUES_ENV_OVERRIDE));

      return changeContextBuilder.build();
    } else if (yamlFilePath.contains(
                   YamlConstants.VALUES_FOLDER + YamlConstants.PATH_DELIMITER + YamlConstants.SERVICES_FOLDER)
        && yamlFilePath.contains(VALUES_YAML_KEY)) {
      changeContextBuilder.withYamlType(YamlType.MANIFEST_FILE_VALUES_ENV_SERVICE_OVERRIDE)
          .withYamlSyncHandler(yamlHandlerFactory.getYamlHandler(YamlType.MANIFEST_FILE_VALUES_ENV_SERVICE_OVERRIDE));

      return changeContextBuilder.build();
    }

    return null;
  }

  private boolean ignoreIfFeatureFlagEnabled(
      String accountId, FeatureName featureName, YamlType yamlTypeToIgnore, YamlType givenYamlType) {
    return yamlTypeToIgnore == givenYamlType && featureFlagService.isEnabled(featureName, accountId);
  }

  private <T extends BaseYamlHandler> List<ChangeContext> validate(List<Change> changeList)
      throws YamlProcessingException {
    logger.info(GIT_YAML_LOG_PREFIX + "Validating changeset");
    List<ChangeContext> changeContextList = Lists.newArrayList();
    Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap = Maps.newConcurrentMap();

    for (Change change : changeList) {
      String yamlFilePath = change.getFilePath();

      try {
        ChangeContext manifestFileChangeContext = validateManifestFile(yamlFilePath, change);

        if (manifestFileChangeContext != null) {
          changeContextList.add(manifestFileChangeContext);
        } else if (yamlFilePath.endsWith(YAML_EXTENSION)) {
          validateYaml(change.getFileContent());
          YamlType yamlType = findYamlType(yamlFilePath, change.getAccountId());
          String yamlSubType = getYamlSubType(change.getFileContent());

          if (ignoreIfFeatureFlagEnabled(
                  change.getAccountId(), FeatureName.INFRA_MAPPING_REFACTOR, INFRA_MAPPING, yamlType)) {
            logger.warn(format("Skipping %s because feature flag %s is enabled", change.getFilePath(),
                FeatureName.INFRA_MAPPING_REFACTOR));
            continue;
          }

          T yamlSyncHandler = yamlHandlerFactory.getYamlHandler(yamlType, yamlSubType);
          Class yamlClass = yamlSyncHandler.getYamlClass();
          BaseYaml yaml = getYaml(change.getFileContent(), yamlClass);
          notNullCheck("Could not get yaml object for :" + yamlFilePath, yaml);

          ChangeContext.Builder changeContextBuilder = ChangeContext.Builder.aChangeContext()
                                                           .withChange(change)
                                                           .withYaml(yaml)
                                                           .withYamlType(yamlType)
                                                           .withYamlSyncHandler(yamlSyncHandler);
          ChangeContext changeContext = changeContextBuilder.build();
          changeContextList.add(changeContext);
        } else if (yamlFilePath.contains(YamlConstants.CONFIG_FILES_FOLDER)) {
          // Special handling for config files
          YamlType yamlType = findYamlType(yamlFilePath, change.getAccountId());
          if (YamlType.CONFIG_FILE_CONTENT == yamlType || YamlType.CONFIG_FILE_OVERRIDE_CONTENT == yamlType) {
            ChangeContext.Builder changeContextBuilder =
                ChangeContext.Builder.aChangeContext().withChange(change).withYamlType(yamlType);
            changeContextList.add(changeContextBuilder.build());
          } else {
            addToFailedYamlMap(failedYamlFileChangeMap, change, "Unsupported type: " + yamlType);
          }
        }
      } catch (ScannerException ex) {
        String message;
        Mark contextMark = ex.getContextMark();
        if (contextMark != null) {
          String snippet = contextMark.get_snippet();
          if (snippet != null) {
            message = "Not a well-formed yaml. The field " + snippet + " in line " + contextMark.getLine()
                + " doesn't end with :";
          } else {
            message = ExceptionUtils.getMessage(ex);
          }
        } else {
          message = ExceptionUtils.getMessage(ex);
        }
        logger.warn(message, ex);
        addToFailedYamlMap(failedYamlFileChangeMap, change, message);
      } catch (UnrecognizedPropertyException ex) {
        String propertyName = ex.getPropertyName();
        if (propertyName != null) {
          String error = "Unrecognized field: " + propertyName;
          logger.warn(error, ex);
          addToFailedYamlMap(failedYamlFileChangeMap, change, error);
        } else {
          logger.warn("Unable to load yaml from string for file: " + yamlFilePath, ex);
          addToFailedYamlMap(failedYamlFileChangeMap, change, ExceptionUtils.getMessage(ex));
        }
      } catch (Exception ex) {
        logger.warn("Unable to load yaml from string for file: " + yamlFilePath, ex);
        addToFailedYamlMap(failedYamlFileChangeMap, change, ExceptionUtils.getMessage(ex));
      }
    }

    if (failedYamlFileChangeMap.size() > 0) {
      throw new YamlProcessingException(
          "Error while processing some yaml files in the changeset", failedYamlFileChangeMap);
    }

    logger.info(GIT_YAML_LOG_PREFIX + "Validated changeset");
    return changeContextList;
  }

  private void addToFailedYamlMap(
      Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap, Change change, String errorMsg) {
    ChangeWithErrorMsg changeWithErrorMsg = ChangeWithErrorMsg.builder().change(change).errorMsg(errorMsg).build();
    failedYamlFileChangeMap.put(change.getFilePath(), changeWithErrorMsg);
  }

  /**
   * To find the yaml sub type, we need to look at the type field in the yaml payload
   * @param fileContent
   * @return
   */
  private String getYamlSubType(String fileContent) throws IOException {
    YamlReader reader = new YamlReader(fileContent);
    Object object = reader.read();
    Map map = (Map) object;
    return (String) map.get("type");
  }

  private void process(List<ChangeContext> changeContextList, boolean gitSyncPath) throws YamlProcessingException {
    if (isEmpty(changeContextList)) {
      logger.info("No changes to process in the change set");
      return;
    }

    String accountId = changeContextList.get(0).getChange().getAccountId();
    Queue<Future> futures = new ConcurrentLinkedQueue<>();

    Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap = Maps.newConcurrentMap();
    YamlType previousYamlType = null;
    ChangeType previousChangeType = null;
    int numOfParallelChanges = 0;

    for (ChangeContext changeContext : changeContextList) {
      String yamlFilePath = changeContext.getChange().getFilePath();
      YamlType yamlType = changeContext.getYamlType();
      ChangeType changeType = changeContext.getChange().getChangeType();
      if (previousYamlType == null) {
        previousYamlType = yamlType;
      }

      if (previousChangeType == null) {
        previousChangeType = changeContext.getChange().getChangeType();
      }

      if (previousYamlType != yamlType || (++numOfParallelChanges == YAML_MAX_PARALLEL_COUNT)) {
        checkFuturesAndEvictCache(futures, previousYamlType, accountId, previousChangeType);
        previousYamlType = yamlType;
        previousChangeType = changeType;
        numOfParallelChanges = 0;
      }

      futures.add(executorService.submit(() -> {
        try {
          logger.info("Processing file: [{}]", changeContext.getChange().getFilePath());
          processYamlChange(changeContext, changeContextList);
          if (gitSyncPath) {
            yamlGitService.discardGitSyncError(changeContext.getChange().getAccountId(), yamlFilePath);
          }

          logger.info("Processing done for file [{}]", changeContext.getChange().getFilePath());
        } catch (Exception ex) {
          logger.warn(format("Exception while processing yaml file %s", yamlFilePath), ex);
          ChangeWithErrorMsg changeWithErrorMsg = ChangeWithErrorMsg.builder()
                                                      .change(changeContext.getChange())
                                                      .errorMsg(ExceptionUtils.getMessage(ex))
                                                      .build();
          // We continue processing the yaml files we understand, the failures are reported at the end
          failedYamlFileChangeMap.put(changeContext.getChange().getFilePath(), changeWithErrorMsg);
        }
      }));
    }

    checkFuturesAndEvictCache(futures, previousYamlType, accountId, previousChangeType);

    if (failedYamlFileChangeMap.size() > 0) {
      logAllErrorsWhileYamlInjestion(failedYamlFileChangeMap);
      throw new YamlProcessingException(
          "Error while processing some yaml files in the changeset", failedYamlFileChangeMap);
    }
  }

  private void logAllErrorsWhileYamlInjestion(Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap) {
    StringBuilder builder =
        new StringBuilder(128).append(GIT_YAML_LOG_PREFIX).append("Found Following Errors in Yaml Injestion");
    if (EmptyPredicate.isNotEmpty(failedYamlFileChangeMap)) {
      for (Map.Entry<String, ChangeWithErrorMsg> entry : failedYamlFileChangeMap.entrySet()) {
        builder.append("\nFileName: ")
            .append(entry.getKey())
            .append(", ErrorMsg: ")
            .append(entry.getValue().getErrorMsg());
      }
    }

    logger.warn(builder.toString());
  }

  private void checkFuturesAndEvictCache(
      Queue<Future> futures, YamlType yamlType, String accountId, ChangeType changeType) {
    while (!futures.isEmpty()) {
      try {
        if (futures.peek().isDone()) {
          futures.poll().get();
        }
      } catch (InterruptedException | ExecutionException e) {
        logger.error("Error while waiting for processing of entities of type {} for account {} ",
            yamlType != null ? yamlType.name() : "", accountId);
      }
      quietSleep(ofMillis(10));
    }
    invalidateUserRelatedCacheIfNeeded(accountId, yamlType, changeType);
  }

  private void invalidateUserRelatedCacheIfNeeded(String accountId, YamlType yamlType, ChangeType changeType) {
    if (changeType.equals(ChangeType.ADD) && rbacCreateYamlTypes.contains(yamlType)) {
      if (yamlType.equals(YamlType.APPLICATION) || yamlType.equals(YamlType.ENVIRONMENT)) {
        authService.evictUserPermissionAndRestrictionCacheForAccount(accountId, true, true);
      } else {
        authService.evictUserPermissionCacheForAccount(accountId, true);
      }
    } else if (changeType.equals(ChangeType.MODIFY) && rbacUpdateYamlTypes.contains(yamlType)) {
      if (yamlType.equals(YamlType.ENVIRONMENT)) {
        authService.evictUserPermissionAndRestrictionCacheForAccount(accountId, true, true);
      } else {
        authService.evictUserPermissionCacheForAccount(accountId, true);
      }
    } else if (changeType.equals(ChangeType.DELETE) && rbacDeleteYamlTypes.contains(yamlType)) {
      authService.evictUserPermissionAndRestrictionCacheForAccount(accountId, true, true);
    }
  }

  private void processYamlChange(ChangeContext changeContext, List<ChangeContext> changeContextList)
      throws HarnessException {
    notNullCheck("changeContext is null", changeContext, USER);
    Change change = changeContext.getChange();
    notNullCheck("FileChange is null", change, USER);
    notNullCheck("ChangeType is null for change:" + change.getFilePath(), change.getChangeType(), USER);

    if (!(change instanceof GitFileChange)) {
      throw new IllegalArgumentException(
          "Expected change to be instance of GitFileChange. Found: " + change.getClass().getCanonicalName());
    }

    String commitId = ((GitFileChange) changeContext.getChange()).getCommitId();
    String accountId = change.getAccountId();

    // do not process commits which exceed limits
    // if you process them, the processing throws validation errors which populates the Alerts page with GitSyncErrors
    if (failedCommitStore.didExceedLimit(new FailedCommitStore.Commit(commitId, accountId))) {
      return;
    }

    // If its not a yaml file, we don't have a handler for that file
    if (!change.getFilePath().endsWith(YAML_EXTENSION) && !doesEntityUsesActualFile(change.getFilePath())) {
      return;
    }

    BaseYamlHandler yamlSyncHandler = changeContext.getYamlSyncHandler();

    switch (change.getChangeType()) {
      case ADD:
      case MODIFY:
        upsertFromYaml(changeContext, changeContextList);
        break;
      case DELETE:
        yamlSyncHandler.delete(changeContext);
        break;
      case RENAME:
        // TODO
      default:
        // TODO
        break;
    }
  }

  private boolean doesEntityUsesActualFile(String filePath) {
    if (Pattern.compile(YamlType.MANIFEST_FILE.getPathExpression()).matcher(filePath).matches()) {
      return true;
    }

    return false;
  }

  private void upsertFromYaml(ChangeContext changeContext, List<ChangeContext> changeContextList)
      throws HarnessException {
    GitFileChange change = (GitFileChange) changeContext.getChange();
    String commitId = change.getCommitId();
    String accountId = change.getAccountId();
    BaseYamlHandler yamlSyncHandler = changeContext.getYamlSyncHandler();

    try {
      yamlSyncHandler.upsertFromYaml(changeContext, changeContextList);

      // Handling for tags
      harnessTagYamlHelper.upsertTagLinksIfRequired(changeContext);

    } catch (WingsException e) {
      if (e.getCode() == ErrorCode.USAGE_LIMITS_EXCEEDED) {
        logger.info("Usage Limit Exceeded. Account: {}. Message: {}", change.getAccountId(), e.getMessage());
        failedCommitStore.exceededLimit(new FailedCommitStore.Commit(commitId, accountId));
      }
      throw e;
    }
  }

  private BaseYaml getYaml(String yamlString, Class<? extends BaseYaml> yamlClass) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return mapper.readValue(yamlString, yamlClass);
  }

  private final class FilePathComparator implements Comparator<Change> {
    @Override
    public int compare(Change lhs, Change rhs) {
      return findOrdinal(lhs.getFilePath()) - findOrdinal(rhs.getFilePath());
    }
  }

  private int findOrdinal(String yamlFilePath) {
    final AtomicInteger count = new AtomicInteger();
    Optional<YamlType> first = yamlProcessingOrder.stream()
                                   .filter(yamlType -> {
                                     count.incrementAndGet();
                                     return Pattern.matches(yamlType.getPathExpression(), yamlFilePath);
                                   })
                                   .findFirst();

    if (first.isPresent()) {
      return count.get();
    } else {
      return -1;
    }
  }

  private YamlType findYamlType(String yamlFilePath, String accountId) throws YamlException {
    Optional<YamlType> first = yamlProcessingOrder.stream()
                                   .filter(yamlType -> Pattern.matches(yamlType.getPathExpression(), yamlFilePath))
                                   .findFirst();

    if (first.isPresent()) {
      if (first.get().equals(TRIGGER) && isTriggerRefactor(accountId)) {
        return DEPLOYMENT_TRIGGER;
      } else {
        return first.get();
      }
    } else {
      throw new YamlException("Unknown yaml type for path: " + yamlFilePath, null);
    }
  }

  private boolean isTriggerRefactor(String accountId) {
    return featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId);
  }

  /**
   * Check if the yaml is valid
   * @param yamlString
   * @return
   */
  private void validateYaml(String yamlString) throws ScannerException {
    Yaml yamlObj = new Yaml();

    // We just load the yaml to see if its well formed.
    yamlObj.load(yamlString);
  }
}
