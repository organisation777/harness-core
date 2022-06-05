/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.jenkins;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import static java.util.stream.Collectors.toList;

import io.harness.artifacts.jenkins.beans.JenkinsInternalConfig;
import io.harness.artifacts.jenkins.client.JenkinsClient;
import io.harness.artifacts.jenkins.client.JenkinsCustomServer;
import io.harness.artifacts.jenkins.service.JenkinsRegistryService;
import io.harness.artifacts.jenkins.service.JenkinsRegistryUtils;
import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.artifacts.DelegateArtifactTaskHandler;
import io.harness.delegate.task.artifacts.mappers.JenkinsRequestResponseMapper;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.jenkins.JenkinsBuildTaskNGResponse;
import io.harness.exception.ArtifactServerException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.SecretDecryptionService;

import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.QueueReference;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class JenkinsArtifactTaskHandler extends DelegateArtifactTaskHandler<JenkinsArtifactDelegateRequest> {
  private static final int ARTIFACT_RETENTION_SIZE = 25;
  private final SecretDecryptionService secretDecryptionService;
  private final JenkinsRegistryService jenkinsRegistryService;
  @Inject private JenkinsRegistryUtils jenkinsRegistryUtils;

  @Override
  public ArtifactTaskExecutionResponse validateArtifactServer(JenkinsArtifactDelegateRequest attributesRequest) {
    boolean isServerValidated = jenkinsRegistryService.validateCredentials(
        JenkinsRequestResponseMapper.toJenkinsInternalConfig(attributesRequest));
    return ArtifactTaskExecutionResponse.builder().isArtifactServerValid(isServerValidated).build();
  }

  @Override
  public ArtifactTaskExecutionResponse getJob(JenkinsArtifactDelegateRequest artifactDelegateRequest) {
    List<JobDetails> jobDetails =
        jenkinsRegistryService.getJobs(JenkinsRequestResponseMapper.toJenkinsInternalConfig(artifactDelegateRequest),
            artifactDelegateRequest.getParentJobName());
    return ArtifactTaskExecutionResponse.builder().jobDetails(jobDetails).build();
  }

  @Override
  public ArtifactTaskExecutionResponse getJobWithParamters(JenkinsArtifactDelegateRequest artifactDelegateRequest) {
    JobDetails jobDetails = jenkinsRegistryService.getJobWithParamters(
        JenkinsRequestResponseMapper.toJenkinsInternalConfig(artifactDelegateRequest),
        artifactDelegateRequest.getJobName());
    List<JobDetails> details = new ArrayList<>();
    details.add(jobDetails);
    return ArtifactTaskExecutionResponse.builder().jobDetails(details).build();
  }

  @Override
  public ArtifactTaskExecutionResponse getArtifactPaths(JenkinsArtifactDelegateRequest artifactDelegateRequest) {
    try {
      JobWithDetails jobDetails = jenkinsRegistryService.getJobWithDetails(
          JenkinsRequestResponseMapper.toJenkinsInternalConfig(artifactDelegateRequest),
          artifactDelegateRequest.getJobName());
      List<String> artifactPath = Lists.newArrayList(jobDetails.getLastSuccessfulBuild()
                                                         .details()
                                                         .getArtifacts()
                                                         .stream()
                                                         .map(Artifact::getRelativePath)
                                                         .distinct()
                                                         .collect(toList()));
      return ArtifactTaskExecutionResponse.builder().artifactPath(artifactPath).build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception ex) {
      throw new ArtifactServerException(
          "Error in artifact paths from jenkins server. Reason:" + ExceptionUtils.getMessage(ex), ex, USER);
    }
  }

  @Override
  public ArtifactTaskExecutionResponse getBuilds(JenkinsArtifactDelegateRequest attributesRequest) {
    List<BuildDetails> buildDetails =
        jenkinsRegistryService.getBuildsForJob(JenkinsRequestResponseMapper.toJenkinsInternalConfig(attributesRequest),
            attributesRequest.getJobName(), attributesRequest.getArtifactPaths(), ARTIFACT_RETENTION_SIZE);
    return ArtifactTaskExecutionResponse.builder().buildDetails(buildDetails).build();
  }

  public ArtifactTaskExecutionResponse triggerBuild(JenkinsArtifactDelegateRequest attributesRequest) {
    JenkinsBuildTaskNGResponse jenkinsBuildTaskNGResponse = new JenkinsBuildTaskNGResponse();
    try {
      ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
      String msg = "Error occurred while starting Jenkins task\n";
      JenkinsInternalConfig jenkinsInternalConfig =
          JenkinsRequestResponseMapper.toJenkinsInternalConfig(attributesRequest);
      QueueReference queueReference = jenkinsRegistryUtils.trigger(
          attributesRequest.getJobName(), jenkinsInternalConfig, attributesRequest.getJobParameter());
      String queueItemUrl = queueReference != null ? queueReference.getQueueItemUrlPart() : null;

      // Check if jenkins job start is successful
      if (queueReference != null && isNotEmpty(queueItemUrl)) {
        if (jenkinsInternalConfig.isUseConnectorUrlForJobExecution()) {
          queueItemUrl = updateQueueItemUrl(queueItemUrl, jenkinsInternalConfig.getJenkinsUrl());
          queueReference = createQueueReference(queueItemUrl);
        }
        log.info("Triggered Job successfully with queued Build URL {} ", queueItemUrl);
        jenkinsBuildTaskNGResponse.setQueuedBuildUrl(queueItemUrl);
        saveLogs(null,
            "Triggered Job successfully with queued Build URL : " + queueItemUrl + " and remaining Time (sec): "
                + (attributesRequest.getTimeout() - (System.currentTimeMillis() - attributesRequest.getStartTs()))
                    / 1000);
      } else {
        log.error("The Job was not triggered successfully with queued Build URL {} ", queueItemUrl);
        executionStatus = ExecutionStatus.FAILED;
        jenkinsBuildTaskNGResponse.setErrorMessage(msg);
      }
      JenkinsCustomServer jenkinsServer = JenkinsClient.getJenkinsServer(jenkinsInternalConfig);
      Build jenkinsBuild = jenkinsRegistryUtils.waitForJobToStartExecution(queueReference, jenkinsInternalConfig);
      jenkinsBuildTaskNGResponse.setBuildNumber(String.valueOf(jenkinsBuild.getNumber()));
      jenkinsBuildTaskNGResponse.setJobUrl(jenkinsBuild.getUrl());
    } catch (WingsException e) {
      throw e;
    } catch (IOException ex) {
      throw new InvalidRequestException("Failed to trigger the Jenkins Job" + ExceptionUtils.getMessage(ex), USER);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return ArtifactTaskExecutionResponse.builder().jenkinsBuildTaskNGResponse(jenkinsBuildTaskNGResponse).build();
  }

  private ArtifactTaskExecutionResponse getSuccessTaskExecutionResponse(
      List<JenkinsArtifactDelegateResponse> responseList) {
    return ArtifactTaskExecutionResponse.builder()
        .artifactDelegateResponses(responseList)
        .isArtifactSourceValid(true)
        .isArtifactServerValid(true)
        .build();
  }

  boolean isRegex(JenkinsArtifactDelegateRequest artifactDelegateRequest) {
    return EmptyPredicate.isNotEmpty(artifactDelegateRequest.getBuildRegex());
  }

  @Override
  public void decryptRequestDTOs(JenkinsArtifactDelegateRequest jenkinsArtifactDelegateRequest) {
    if (jenkinsArtifactDelegateRequest.getJenkinsConnectorDTO().getAuth() != null) {
      secretDecryptionService.decrypt(
          jenkinsArtifactDelegateRequest.getJenkinsConnectorDTO().getAuth().getCredentials(),
          jenkinsArtifactDelegateRequest.getEncryptedDataDetails());
    }
  }

  private QueueReference createQueueReference(String location) {
    return new QueueReference(location);
  }

  private String updateQueueItemUrl(String queueItemUrl, String jenkinsUrl) {
    if (jenkinsUrl.endsWith("/")) {
      jenkinsUrl = jenkinsUrl.substring(0, jenkinsUrl.length() - 1);
    }
    String[] queueItemUrlParts = queueItemUrl.split("/queue/");
    return jenkinsUrl.concat("/queue/").concat(queueItemUrlParts[1]);
  }

  private void saveLogs(LogCallback executionLogCallback, String message) {
    if (executionLogCallback != null) {
      executionLogCallback.saveExecutionLog(message);
    }
  }
}
