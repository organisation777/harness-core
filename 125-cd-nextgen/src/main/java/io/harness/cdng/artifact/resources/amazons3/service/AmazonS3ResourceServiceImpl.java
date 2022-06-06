/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.resources.amazons3.service;

import static java.util.stream.Collectors.toList;

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
  public Map<String, String> getBuckets(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    List<String> bucketNames = awsS3HelperServiceDelegate.listBucketNames(awsConfig, encryptionDetails);
    return bucketNames.stream().collect(Collectors.toMap(s -> s, s -> s));
  }

  @Override
  public List<String> getArtifactPaths(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName) {
    ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();
    listObjectsV2Request.withBucketName(bucketName).withMaxKeys(FETCH_FILE_COUNT_IN_BUCKET);
    ListObjectsV2Result result;

    List<S3ObjectSummary> objectSummaryListFinal = new ArrayList<>();
    do {
      result = awsS3HelperServiceDelegate.listObjectsInS3(awsConfig, encryptionDetails, listObjectsV2Request);
      List<S3ObjectSummary> objectSummaryList = result.getObjectSummaries();
      if (EmptyPredicate.isNotEmpty(objectSummaryList)) {
        objectSummaryListFinal.addAll(objectSummaryList.stream()
                                          .filter(objectSummary -> !objectSummary.getKey().endsWith("/"))
                                          .collect(Collectors.toList()));
      }

      listObjectsV2Request.setContinuationToken(result.getNextContinuationToken());
    } while (result.isTruncated() && objectSummaryListFinal.size() < MAX_FILES_TO_SHOW_IN_UI);

    sortDescending(objectSummaryListFinal);
    return objectSummaryListFinal.stream().map(S3ObjectSummary::getKey).collect(toList());
  }

  private void sortDescending(List<S3ObjectSummary> objectSummaryList) {
    if (EmptyPredicate.isEmpty(objectSummaryList)) {
      return;
    }

    objectSummaryList.sort((o1, o2) -> o2.getLastModified().compareTo(o1.getLastModified()));
  }
}
