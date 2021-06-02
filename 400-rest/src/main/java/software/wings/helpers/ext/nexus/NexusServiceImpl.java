package software.wings.helpers.ext.nexus;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.ListNotifyResponseData;
import io.harness.exception.ArtifactServerException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.nexus.NexusRequest;

import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.utils.ArtifactType;
import software.wings.utils.RepositoryFormat;
import software.wings.utils.RepositoryType;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLHandshakeException;
import javax.xml.stream.XMLStreamException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by srinivas on 3/28/17.
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._960_API_SERVICES)
@Singleton
@Slf4j
public class NexusServiceImpl implements NexusService {
  @Inject private NexusThreeServiceImpl nexusThreeService;
  @Inject private NexusTwoServiceImpl nexusTwoService;
  @Inject private TimeLimiter timeLimiter;

  public static void handleException(IOException e) {
    throw new InvalidArtifactServerException(ExceptionUtils.getMessage(e), USER);
  }

  public static boolean isSuccessful(Response<?> response) {
    if (response == null) {
      return false;
    }
    if (!response.isSuccessful()) {
      log.error("Request not successful. Reason: {}", response);
      int code = response.code();
      switch (code) {
        case 404:
          return false;
        case 401:
          throw new InvalidArtifactServerException("Invalid Nexus credentials", USER);
        case 405:
          throw new InvalidArtifactServerException("Method not allowed " + response.message(), USER);
        default:
          throw new InvalidArtifactServerException(response.message(), USER);
      }
    }
    return true;
  }

  public static String getBaseUrl(NexusRequest nexusConfig) {
    return nexusConfig.getNexusUrl().endsWith("/") ? nexusConfig.getNexusUrl() : nexusConfig.getNexusUrl() + "/";
  }

  public static Retrofit getRetrofit(NexusRequest nexusConfig, Converter.Factory converterFactory) {
    String baseUrl = getBaseUrl(nexusConfig);
    return new Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(converterFactory)
        .client(Http.getOkHttpClient(baseUrl, nexusConfig.isCertValidationRequired()))
        .build();
  }

  @Override
  public Map<String, String> getRepositories(NexusRequest nexusConfig) {
    return getRepositories(nexusConfig, null);
  }

  @Override
  public Map<String, String> getRepositories(NexusRequest nexusConfig, String repositoryFormat) {
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      return timeLimiter.callWithTimeout(() -> {
        if (isNexusTwo) {
          if (RepositoryFormat.docker.name().equals(repositoryFormat)) {
            throw new InvalidArtifactServerException("Nexus 2.x does not support Docker artifact type", USER);
          }
          return nexusTwoService.getRepositories(nexusConfig, repositoryFormat);
        } else {
          if (repositoryFormat == null) {
            throw new InvalidRequestException("Not supported for nexus 3.x", USER);
          }
          return nexusThreeService.getRepositories(nexusConfig, repositoryFormat);
        }
      }, 20L, TimeUnit.SECONDS, true);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error occurred while retrieving Repositories from Nexus server " + nexusConfig.getNexusUrl(), e);
      if (e.getCause() != null && e.getCause() instanceof XMLStreamException) {
        throw new InvalidArtifactServerException("Nexus may not be running", USER);
      }
      checkSSLHandshakeException(e);
      return emptyMap();
    }
  }

  @Override
  public List<String> getGroupIdPaths(NexusRequest nexusConfig, String repoId, String repositoryFormat) {
    List<String> groupIds = new ArrayList<>();
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      if (isNexusTwo) {
        timeLimiter.callWithTimeout(
            ()
                -> nexusTwoService.collectGroupIds(nexusConfig, repoId, groupIds, repositoryFormat),
            20L, TimeUnit.SECONDS, true);
      } else {
        if (repositoryFormat != null) {
          switch (repositoryFormat) {
            case "nuget":
            case "npm":
              return timeLimiter.callWithTimeout(
                  ()
                      -> nexusThreeService.getPackageNames(nexusConfig, repoId, repositoryFormat, groupIds),
                  20L, TimeUnit.SECONDS, true);
            case "maven":
              return timeLimiter.callWithTimeout(
                  ()
                      -> nexusThreeService.getGroupIds(nexusConfig, repoId, repositoryFormat, groupIds),
                  20L, TimeUnit.SECONDS, true);
            case "docker":
              return timeLimiter.callWithTimeout(
                  () -> nexusThreeService.getDockerImages(nexusConfig, repoId, groupIds), 20L, TimeUnit.SECONDS, true);
            default:
              throw new InvalidArtifactServerException("Unsupported repositoryFormat for Nexus 3.x");
          }
        } else {
          // for backward compatibility  with old UI when repositoryFormat is null
          return timeLimiter.callWithTimeout(
              () -> nexusThreeService.getDockerImages(nexusConfig, repoId, groupIds), 20L, TimeUnit.SECONDS, true);
        }
      }
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to fetch images/groups from Nexus server " + nexusConfig.getNexusUrl() + " under repo " + repoId, e);
      if (e.getCause() != null && e.getCause() instanceof XMLStreamException) {
        throw new InvalidArtifactServerException("Nexus may not be running", e);
      } else if ((e.getCause() != null && e.getCause() instanceof SocketTimeoutException)
          || (e instanceof SocketTimeoutException)) {
        throw new ArtifactServerException(
            "Timed out while connecting to the nexus server " + nexusConfig.getNexusUrl() + " under repo " + repoId,
            e.getCause(), USER);
      } else if ((e.getCause() != null && e.getCause() instanceof TimeoutException)
          || (e instanceof TimeoutException)) {
        throw new ArtifactServerException("Timed out while fetching images/groups from Nexus server "
                + nexusConfig.getNexusUrl() + " under repo " + repoId,
            e.getCause(), USER);
      } else {
        throw new ArtifactServerException(
            "Failed to fetch images/groups from Nexus server: " + e.getMessage(), e.getCause(), USER);
      }
    }
    return groupIds;
  }

  @Override
  public List<String> getArtifactPaths(NexusRequest nexusConfig, String repoId) {
    try {
      return nexusTwoService.getArtifactPaths(nexusConfig, repoId);
    } catch (final IOException e) {
      log.error("Error occurred while retrieving repository contents from Nexus Server " + nexusConfig.getNexusUrl()
              + " for repository " + repoId,
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public List<String> getArtifactPaths(NexusRequest nexusConfig, String repoId, String name) {
    try {
      return nexusTwoService.getArtifactPaths(nexusConfig, repoId, name);
    } catch (final IOException e) {
      log.error("Error occurred while retrieving Artifact paths from Nexus server " + nexusConfig.getNexusUrl()
              + " for Repository " + repoId,
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public Pair<String, InputStream> downloadArtifacts(NexusRequest nexusConfig,
      ArtifactStreamAttributes artifactStreamAttributes, Map<String, String> artifactMetadata, String delegateId,
      String taskId, String accountId, ListNotifyResponseData notifyResponseData) {
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      if (isNexusTwo) {
        return nexusTwoService.downloadArtifact(
            nexusConfig, artifactStreamAttributes, artifactMetadata, delegateId, taskId, accountId, notifyResponseData);
      } else {
        return nexusThreeService.downloadArtifact(
            nexusConfig, artifactStreamAttributes, artifactMetadata, delegateId, taskId, accountId, notifyResponseData);
      }
    } catch (IOException e) {
      log.error("Error occurred while downloading the artifact", e);
      throw new ArtifactServerException(ExceptionUtils.getMessage(e), USER);
    }
  }

  @Override
  public List<String> getArtifactNames(NexusRequest nexusConfig, String repoId, String path) {
    try {
      return nexusTwoService.getArtifactNames(nexusConfig, repoId, path);
    } catch (final IOException e) {
      log.error(
          format("Error occurred while retrieving artifact names from Nexus server %s for Repository %s under path %s",
              nexusConfig.getNexusUrl(), repoId, path),
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public List<String> getArtifactNames(NexusRequest nexusConfig, String repoId, String path, String repositoryFormat) {
    try {
      if (repositoryFormat.equals(RepositoryFormat.maven.name())) {
        return nexusThreeService.getArtifactNames(nexusConfig, repoId, path);
      }
    } catch (final IOException e) {
      log.error(
          format("Error occurred while retrieving artifact names from Nexus server %s for Repository %s under path %s",
              nexusConfig.getNexusUrl(), repoId, path),
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public List<BuildDetails> getVersions(NexusRequest nexusConfig, String repoId, String groupId, String artifactName,
      String extension, String classifier) {
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      if (isNexusTwo) {
        return nexusTwoService.getVersions(nexusConfig, repoId, groupId, artifactName, extension, classifier);
      } else {
        return nexusThreeService.getVersions(nexusConfig, repoId, groupId, artifactName, extension, classifier);
      }
    } catch (final IOException e) {
      log.error(
          format(
              "Error occurred while retrieving versions from Nexus server %s for Repository %s under group id %s and artifact name %s",
              nexusConfig.getNexusUrl(), repoId, groupId, artifactName),
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public boolean existsVersion(NexusRequest nexusConfig, String repoId, String groupId, String artifactName,
      String extension, String classifier) {
    if (isEmpty(extension) && isEmpty(classifier)) {
      return true;
    }
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      if (isNexusTwo) {
        return nexusTwoService.existsVersion(nexusConfig, repoId, groupId, artifactName, extension, classifier);
      } else {
        return nexusThreeService.existsVersion(nexusConfig, repoId, groupId, artifactName, extension, classifier);
      }
    } catch (final IOException e) {
      log.error(
          format(
              "Error occurred while retrieving versions from Nexus server %s for Repository %s under group id %s and artifact name %s",
              nexusConfig.getNexusUrl(), repoId, groupId, artifactName),
          e);
      handleException(e);
    }
    return true;
  }

  @Override
  public List<BuildDetails> getVersions(
      String repositoryFormat, NexusRequest nexusConfig, String repoId, String packageName) {
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      if (isNexusTwo) {
        return nexusTwoService.getVersions(repositoryFormat, nexusConfig, repoId, packageName);
      } else {
        return nexusThreeService.getPackageVersions(nexusConfig, repoId, packageName);
      }
    } catch (final IOException e) {
      log.error(
          format("Error occurred while retrieving versions from Nexus server %s for Repository %s under package %s",
              nexusConfig.getNexusUrl(), repoId, packageName),
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  @SuppressWarnings("squid:S00107")
  public List<BuildDetails> getVersion(NexusRequest nexusConfig, String repoId, String groupId, String artifactName,
      String extension, String classifier, String buildNo) {
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      if (isNexusTwo) {
        return nexusTwoService.getVersion(nexusConfig, repoId, groupId, artifactName, extension, classifier, buildNo);
      } else {
        throw new InvalidArtifactServerException(
            "Nexus 3.x does not support getVersion for parameterized artifact stream");
      }
    } catch (final IOException e) {
      log.error(
          format(
              "Error occurred while retrieving versions from Nexus server %s for Repository %s under group id %s and artifact name %s",
              nexusConfig.getNexusUrl(), repoId, groupId, artifactName),
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public List<BuildDetails> getVersion(
      String repositoryFormat, NexusRequest nexusConfig, String repoId, String packageName, String buildNo) {
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      if (isNexusTwo) {
        return Collections.singletonList(
            nexusTwoService.getVersion(repositoryFormat, nexusConfig, repoId, packageName, buildNo));
      } else {
        throw new InvalidArtifactServerException(
            "Nexus 3.x does not support getVersion for parameterized artifact stream");
      }
    } catch (final IOException e) {
      log.error(
          format("Error occurred while retrieving version %s from Nexus server %s for Repository %s under package %s",
              buildNo, nexusConfig.getNexusUrl(), repoId, packageName),
          e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public BuildDetails getLatestVersion(NexusRequest nexusConfig, String repoId, String groupId, String artifactName) {
    return nexusTwoService.getLatestVersion(nexusConfig, repoId, groupId, artifactName);
  }

  @Override
  public List<BuildDetails> getBuilds(
      NexusRequest nexusConfig, ArtifactStreamAttributes artifactStreamAttributes, int maxNumberOfBuilds) {
    try {
      if ((artifactStreamAttributes.getArtifactType() != null
              && artifactStreamAttributes.getArtifactType() == ArtifactType.DOCKER)
          || (artifactStreamAttributes.getRepositoryType() != null
              && artifactStreamAttributes.getRepositoryType().equals(RepositoryType.docker.name()))) {
        return nexusThreeService.getDockerTags(nexusConfig, artifactStreamAttributes);
      }
    } catch (IOException e) {
      log.error("Error occurred while retrieving tags from Nexus server {} for repository {} under image {}",
          nexusConfig.getNexusUrl(), artifactStreamAttributes.getJobName(), artifactStreamAttributes.getImageName(), e);
      handleException(e);
    }
    return new ArrayList<>();
  }

  @Override
  public boolean isRunning(NexusRequest nexusConfig) {
    if (nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x")) {
      return getRepositories(nexusConfig, null) != null;
    } else {
      try {
        return nexusThreeService.isServerValid(nexusConfig);
      } catch (InvalidArtifactServerException e) {
        throw e;
      } catch (WingsException e) {
        if (ExceptionUtils.getMessage(e).contains("Invalid Nexus credentials")) {
          throw e;
        }
        return true;
      } catch (Exception e) {
        log.warn("Failed to retrieve repositories. Ignoring validation for Nexus 3 for now. User can give custom path");
        checkSSLHandshakeException(e);
        return true;
      }
    }
  }

  @Override
  public Pair<String, InputStream> downloadArtifactByUrl(
      NexusRequest nexusConfig, String artifactName, String artifactUrl) {
    boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
    if (isNexusTwo) {
      return nexusTwoService.downloadArtifactByUrl(nexusConfig, artifactName, artifactUrl);
    } else {
      return nexusThreeService.downloadArtifactByUrl(nexusConfig, artifactName, artifactUrl);
    }
  }

  @Override
  public long getFileSize(NexusRequest nexusConfig, String artifactName, String artifactUrl) {
    boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
    if (isNexusTwo) {
      return nexusTwoService.getFileSize(nexusConfig, artifactName, artifactUrl);
    } else {
      return nexusThreeService.getFileSize(nexusConfig, artifactName, artifactUrl);
    }
  }

  private void checkSSLHandshakeException(Exception e) {
    if (e.getCause() instanceof SSLHandshakeException
        || ExceptionUtils.getMessage(e).contains("unable to find valid certification path")) {
      throw new ArtifactServerException("Certificate validation failed:" + getRootCauseMessage(e), e);
    }
  }
}
