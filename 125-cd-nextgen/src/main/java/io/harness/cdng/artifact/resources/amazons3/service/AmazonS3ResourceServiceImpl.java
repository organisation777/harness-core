/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.resources.amazons3.service;

import static java.util.stream.Collectors.toList;

import io.harness.beans.IdentifierRef;
import io.harness.data.structure.EmptyPredicate;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.service.intfc.aws.delegate.AwsS3HelperServiceDelegate;

import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AmazonS3ResourceServiceImpl implements AmazonS3ResourceService {
  @Inject AwsS3HelperServiceDelegate awsS3HelperServiceDelegate;
  private static final int FETCH_FILE_COUNT_IN_BUCKET = 500;
  private static final int MAX_FILES_TO_SHOW_IN_UI = 1000;

  @Override
  public Map<String, String> getBuckets(
      IdentifierRef connectorIdentifier, String accountId, String orgId, String projId) {
    return null;
  }

  @Override
  public List<String> getArtifactPaths(
      IdentifierRef connectorIdentifier, String accountId, String orgId, String projId) {
    return null;
  }
}
