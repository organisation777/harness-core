/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cvng.beans.CustomHealthDataCollectionInfo;
import io.harness.cvng.beans.MetricResponseMappingDTO;
import io.harness.cvng.core.beans.CustomHealthRequestDefinition;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.MetricResponseMapping;
import io.harness.cvng.core.entities.CustomHealthMetricCVConfig;
import io.harness.cvng.core.entities.VerificationTask.TaskType;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionSLIInfoMapper;
import io.harness.cvng.core.utils.dataCollection.MetricDataCollectionUtils;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CustomHealthMetricDataCollectionInfoMapper
    implements DataCollectionInfoMapper<CustomHealthDataCollectionInfo, CustomHealthMetricCVConfig>,
               DataCollectionSLIInfoMapper<CustomHealthDataCollectionInfo, CustomHealthMetricCVConfig> {
  @Override
  public CustomHealthDataCollectionInfo toDataCollectionInfo(CustomHealthMetricCVConfig cvConfig, TaskType taskType) {
    CustomHealthDataCollectionInfo customHealthDataCollectionInfo =
        CustomHealthDataCollectionInfo.builder()
            .groupName(cvConfig.getGroupName())
            .metricInfoList(
                cvConfig.getMetricDefinitions()
                    .stream()
                    .filter(metricInfo
                        -> MetricDataCollectionUtils.isMetricApplicableForDataCollection(metricInfo, taskType))
                    .map(metricDefinition -> mapMetricDefinitionToMetricInfo(metricDefinition))
                    .collect(Collectors.toList()))
            .build();
    customHealthDataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return customHealthDataCollectionInfo;
  }

  @Override
  public CustomHealthDataCollectionInfo toDataCollectionInfo(
      List<CustomHealthMetricCVConfig> cvConfigs, ServiceLevelIndicator serviceLevelIndicator) {
    if (isEmpty(cvConfigs) || serviceLevelIndicator == null) {
      return null;
    }

    List<String> sliMetricNames = serviceLevelIndicator.getMetricNames();
    List<CustomHealthDataCollectionInfo.CustomHealthMetricInfo> metricInfoList = new ArrayList<>();
    cvConfigs.forEach(cvConfig -> cvConfig.getMetricDefinitions().forEach(metricInfo -> {
      if (sliMetricNames.contains(metricInfo.getMetricName())) {
        metricInfoList.add(mapMetricDefinitionToMetricInfo(metricInfo));
      }
    }));

    CustomHealthDataCollectionInfo customHealthDataCollectionInfo = CustomHealthDataCollectionInfo.builder()
                                                                        .groupName(cvConfigs.get(0).getGroupName())
                                                                        .metricInfoList(metricInfoList)
                                                                        .build();
    customHealthDataCollectionInfo.setDataCollectionDsl(cvConfigs.get(0).getDataCollectionDsl());
    return customHealthDataCollectionInfo;
  }

  private CustomHealthDataCollectionInfo.CustomHealthMetricInfo mapMetricDefinitionToMetricInfo(
      CustomHealthMetricCVConfig.CustomHealthCVConfigMetricDefinition metricDefinition) {
    MetricResponseMapping metricResponseMapping = metricDefinition.getMetricResponseMapping();
    CustomHealthRequestDefinition healthDefinition = metricDefinition.getRequestDefinition();
    return CustomHealthDataCollectionInfo.CustomHealthMetricInfo.builder()
        .metricName(metricDefinition.getMetricName())
        .metricIdentifier(metricDefinition.getIdentifier())
        .endTime(healthDefinition.getEndTimeInfo())
        .responseMapping(MetricResponseMappingDTO.builder()
                             .metricValueJsonPath(metricResponseMapping.getMetricValueJsonPath())
                             .serviceInstanceJsonPath(metricResponseMapping.getServiceInstanceJsonPath())
                             .timestampFormat(metricResponseMapping.getTimestampFormat())
                             .timestampJsonPath(metricResponseMapping.getTimestampJsonPath())
                             .build())
        .body(healthDefinition.getRequestBody())
        .method(healthDefinition.getMethod())
        .startTime(healthDefinition.getStartTimeInfo())
        .urlPath(healthDefinition.getUrlPath())
        .build();
  }
}