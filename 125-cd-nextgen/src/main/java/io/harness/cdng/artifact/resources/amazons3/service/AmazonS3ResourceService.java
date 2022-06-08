/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.resources.amazons3.service;

import io.harness.beans.IdentifierRef;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import java.util.Map;

public interface AmazonS3ResourceService {
  Map<String, String> getBuckets(IdentifierRef connectorIdentifier, String accountId, String orgId, String projId);

  List<String> getArtifactPaths(IdentifierRef connectorIdentifier, String accountId, String orgId, String projId);
}
