package software.wings.service.intfc.aws.manager;

import io.harness.delegate.task.protocol.AwsElbListener;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

public interface AwsElbHelperServiceManager {
  List<String> listClassicLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId);
  List<String> listApplicationLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId);
  Map<String, String> listTargetGroupsForAlb(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String loadBalancerName, String appId);
  List<String> listElasticLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId);
  List<String> listNetworkLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId);
  List<AwsElbListener> listListenersForElb(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String loadBalancerName, String appId);
}