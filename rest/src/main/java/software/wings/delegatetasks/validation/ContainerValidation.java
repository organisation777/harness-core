package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static software.wings.utils.HttpUtil.connectableHttpUrl;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultBuilder;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/29/17
 */
public class ContainerValidation extends AbstractDelegateValidateTask {
  @Inject @Transient private transient GkeClusterService gkeClusterService;
  @Inject @Transient private transient AzureHelperService azureHelperService;

  public ContainerValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    ContainerServiceParams containerServiceParams = (ContainerServiceParams) getParameters()[2];
    String criteria = getCriteria(containerServiceParams);

    DelegateConnectionResultBuilder result = DelegateConnectionResult.builder().criteria(criteria);
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();

    if (value instanceof AwsConfig) {
      result.validated(AwsHelperService.isInAwsRegion(substringAfter(criteria, ":")));
    } else {
      result.validated(connectableHttpUrl(getKubernetesMasterUrl(containerServiceParams)));
    }

    return singletonList(result.build());
  }

  private String getKubernetesMasterUrl(ContainerServiceParams containerServiceParams) {
    SettingAttribute settingAttribute = containerServiceParams.getSettingAttribute();
    SettingValue value = settingAttribute.getValue();
    KubernetesConfig kubernetesConfig;
    if (value instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) value;
    } else {
      String clusterName = containerServiceParams.getClusterName();
      String namespace = containerServiceParams.getNamespace();
      String subscriptionId = containerServiceParams.getSubscriptionId();
      String resourceGroup = containerServiceParams.getResourceGroup();
      List<EncryptedDataDetail> edd = containerServiceParams.getEncryptionDetails();
      if (value instanceof GcpConfig) {
        kubernetesConfig = gkeClusterService.getCluster(settingAttribute, edd, clusterName, namespace);
      } else if (value instanceof AzureConfig) {
        AzureConfig azureConfig = (AzureConfig) value;
        kubernetesConfig = azureHelperService.getKubernetesClusterConfig(
            azureConfig, edd, subscriptionId, resourceGroup, clusterName, namespace);
      } else {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "Unknown kubernetes cloud provider setting value: " + value.getType());
      }
    }
    return kubernetesConfig.getMasterUrl();
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(getCriteria((ContainerServiceParams) getParameters()[2]));
  }

  private String getCriteria(ContainerServiceParams containerServiceParams) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (value instanceof AwsConfig) {
      return "AWS:" + containerServiceParams.getRegion();
    } else if (value instanceof KubernetesConfig) {
      return ((KubernetesConfig) value).getMasterUrl();
    } else {
      return containerServiceParams.getSettingAttribute().getName() + "|" + containerServiceParams.getClusterName();
    }
  }
}
