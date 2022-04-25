/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.pcf;

import io.harness.pcf.model.ManifestType;

import software.wings.helpers.ext.k8s.request.K8sValuesLocation;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CFManifestDataInfo {
  private Map<K8sValuesLocation, Map<ManifestType, List<String>>> manifestMap;
  private String applicationManifestFilePath;
  private String autoscalarManifestFilePath;
}