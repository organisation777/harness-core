package software.wings.service.impl.instance;

import static io.harness.rule.OwnerRule.HITESH;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerDeploymentInfoWithNames;
import software.wings.api.DeploymentSummary;
import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.key.deployment.ContainerDeploymentKey;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.settings.SettingValue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class CloudToHarnessMappingServiceImplTest extends WingsBaseTest {
  @InjectMocks @Inject private CloudToHarnessMappingServiceImpl cloudToHarnessMappingService;
  @Mock private DeploymentServiceImpl deploymentService;

  private final String ACCOUNT_ID = "account_id";
  private final String ECS_SERVICE_NAME = "ecs_service_name";
  private final String ECS_CLUSTER_NAME = "ecs_cluster_name";
  private final String INFRA_MAPPING_ID = "infra_mapping_id";
  private final Instant instant = Instant.now();

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetNullHarnessServiceInfo() {
    DeploymentSummary deploymentSummary = ecsDeploymentSummary();
    when(deploymentService.getWithAccountId(deploymentSummary)).thenReturn(Optional.empty());
    Optional<HarnessServiceInfo> harnessServiceInfo =
        cloudToHarnessMappingService.getHarnessServiceInfo(deploymentSummary);
    assertThat(harnessServiceInfo).isNotPresent();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetAccountNameFromId() {
    Account accountInfoFromId = cloudToHarnessMappingService.getAccountInfoFromId(ACCOUNT_ID);
    assertThat(accountInfoFromId.getAccountName()).isEqualTo(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetSettingAttributes() {
    List<SettingAttribute> settingAttributes = cloudToHarnessMappingService.getSettingAttributes(ACCOUNT_ID,
        SettingAttribute.SettingCategory.CE_CONNECTOR.toString(), SettingValue.SettingVariableTypes.CE_AWS.toString(),
        instant.toEpochMilli(), instant.plus(1, ChronoUnit.DAYS).toEpochMilli());
    assertThat(settingAttributes.size()).isEqualTo(0);
  }

  public DeploymentSummary ecsDeploymentSummary() {
    ContainerDeploymentKey containerDeploymentKey =
        ContainerDeploymentKey.builder().containerServiceName(ECS_SERVICE_NAME).build();
    ContainerDeploymentInfoWithNames containerDeploymentInfoWithNames = ContainerDeploymentInfoWithNames.builder()
                                                                            .clusterName(ECS_CLUSTER_NAME)
                                                                            .containerSvcName(ECS_SERVICE_NAME)
                                                                            .build();
    return DeploymentSummary.builder()
        .accountId(ACCOUNT_ID)
        .containerDeploymentKey(containerDeploymentKey)
        .deploymentInfo(containerDeploymentInfoWithNames)
        .infraMappingId(INFRA_MAPPING_ID)
        .build();
  }
}
