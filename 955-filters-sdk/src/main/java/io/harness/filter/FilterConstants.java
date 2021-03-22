package io.harness.filter;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DX)
public class FilterConstants {
  public static final String CONNECTOR_FILTER = "Connector";
  public static final String PIPELINE_SETUP_FILTER = "PipelineSetup";
  public static final String PIPELINE_EXECUTION_FILTER = "PipelineExecution";
  public static final String DEPLOYMENT_FILTER = "Deployment";
  public static final String AUDIT_FILTER = "Audit";
}
