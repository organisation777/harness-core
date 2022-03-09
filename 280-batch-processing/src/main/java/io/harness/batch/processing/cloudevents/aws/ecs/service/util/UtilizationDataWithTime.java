/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.cloudevents.aws.ecs.service.util;

import io.harness.batch.processing.billing.service.UtilizationData;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class UtilizationDataWithTime {
  private UtilizationData utilizationData;
  private String serviceId;
  private Instant startTime;
  private Instant endTime;
}