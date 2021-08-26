package io.harness.cvng.beans;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CV)
public enum DataCollectionRequestType {
  SPLUNK_SAVED_SEARCHES,
  SPLUNK_SAMPLE_DATA,
  SPLUNK_LATEST_HISTOGRAM,
  STACKDRIVER_DASHBOARD_LIST,
  STACKDRIVER_DASHBOARD_GET,
  STACKDRIVER_SAMPLE_DATA,
  STACKDRIVER_LOG_SAMPLE_DATA,
  APPDYNAMICS_FETCH_APPS,
  APPDYNAMICS_FETCH_TIERS,
  APPDYNAMICS_GET_METRIC_DATA,
  NEWRELIC_APPS_REQUEST,
  NEWRELIC_VALIDATION_REQUEST,
  PROMETHEUS_METRIC_LIST_GET,
  PROMETHEUS_LABEL_NAMES_GET,
  PROMETHEUS_LABEL_VALUES_GET,
  PROMETHEUS_SAMPLE_DATA,
  PAGERDUTY_SERVICES
}
