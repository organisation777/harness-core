package software.wings.service.impl.aws.model;

public interface AwsConstants {
  int DEFAULT_AMI_ASG_MAX_INSTANCES = 10;
  int DEFAULT_AMI_ASG_TIMEOUT_MIN = 10;
  String DEFAULT_AMI_ASG_NAME = "DEFAULT_ASG";
  String AMI_SETUP_COMMAND_NAME = "AMI Service Setup";
  String AWS_AMI_ALL_PHASE_ROLLBACK_NAME = "Aws Ami All Phase Rollback";
  String PHASE_PARAM = "PHASE_PARAM";
}