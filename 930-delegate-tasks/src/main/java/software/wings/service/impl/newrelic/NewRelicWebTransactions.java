/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Created by rsingh on 9/5/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicWebTransactions {
  private double average_call_time;
  private double average_response_time;
  private long requests_per_minute;
  private long call_count;
  private double min_call_time;
  private double max_call_time;
  private long total_call_time;
  private double throughput;
  private double standard_deviation;
}