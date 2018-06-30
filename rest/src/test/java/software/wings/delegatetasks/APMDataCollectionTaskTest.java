package software.wings.delegatetasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.apm.APMDataCollectionInfo;
import software.wings.sm.StateType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class APMDataCollectionTaskTest {
  private static final Logger logger = LoggerFactory.getLogger(APMDataCollectionTaskTest.class);

  APMDataCollectionInfo dataCollectionInfo;
  private APMDataCollectionTask dataCollectionTask;

  private void setup() {
    String delegateId = UUID.randomUUID().toString();
    String appId = UUID.randomUUID().toString();
    String envId = UUID.randomUUID().toString();
    String waitId = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String infrastructureMappingId = UUID.randomUUID().toString();
    String timeDuration = "10";
    dataCollectionInfo = APMDataCollectionInfo.builder()
                             .startTime(12312321123L)
                             .stateType(StateType.APM_VERIFICATION)
                             .dataCollectionFrequency(2)
                             .hosts(new HashSet(Arrays.asList("test.host.node1", "test.host.node2")))
                             .encryptedDataDetails(new ArrayList<EncryptedDataDetail>())
                             .dataCollectionMinute(0)
                             .build();

    DelegateTask task = aDelegateTask()
                            .withTaskType(TaskType.APM_METRIC_DATA_COLLECTION_TASK)
                            .withAccountId(accountId)
                            .withAppId(appId)
                            .withWaitId(waitId)
                            .withParameters(new Object[] {dataCollectionInfo})
                            .withEnvId(envId)
                            .withInfrastructureMappingId(infrastructureMappingId)
                            .withTimeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration) + 120))
                            .build();
    dataCollectionTask = new APMDataCollectionTask(delegateId, task, null, null);
  }
  private Method useReflectionToMakeInnerClassVisible() throws Exception {
    Class[] innerClasses = dataCollectionTask.getClass().getDeclaredClasses();
    logger.info("" + innerClasses);
    Class[] parameterTypes = new Class[1];
    parameterTypes[0] = java.lang.String.class;
    Method m = innerClasses[0].getDeclaredMethod("resolveBatchHosts", parameterTypes);
    m.setAccessible(true);
    return m;
  }
  @Test
  public void testBatchingHosts() throws Exception {
    setup();
    DataCollectionTaskResult tr = dataCollectionTask.initDataCollection(dataCollectionTask.getParameters());
    String batchUrl = "urlData{$harness_batch{pod_name:${host},'|'}}";
    List<String> batchedHosts =
        (List<String>) useReflectionToMakeInnerClassVisible().invoke(dataCollectionTask.getDataCollector(tr), batchUrl);
    assertEquals("batched hosts should have 1 item", 1, batchedHosts.size());
    assertEquals(
        "Batched string should be", "urlData{pod_name:test.host.node1|pod_name:test.host.node2}", batchedHosts.get(0));
  }

  @Test
  public void testMoreThanFiftyHostsInBatch() throws Exception {
    setup();
    List<String> hostList = new ArrayList<>();
    for (int i = 0; i < 52; i++) {
      hostList.add("test.host.node" + i);
    }

    dataCollectionInfo.setHosts(new HashSet<>(hostList));
    DataCollectionTaskResult tr = dataCollectionTask.initDataCollection(dataCollectionTask.getParameters());
    String batchUrl = "urlData{$harness_batch{pod_name:${host},'|'}}";
    List<String> batchedHosts =
        (List<String>) useReflectionToMakeInnerClassVisible().invoke(dataCollectionTask.getDataCollector(tr), batchUrl);
    assertEquals("batched hosts should have 2 items", 2, batchedHosts.size());
    // Since hostList in the CollectionTask class is a set, the order isn't maintained. So wecant compare directly.
    int occuranceCount1 = StringUtils.countMatches(batchedHosts.get(0), "test.host.node");
    int occuranceCount2 = StringUtils.countMatches(batchedHosts.get(1), "test.host.node");
    assertTrue("Firstbatch has 50 hosts", occuranceCount1 == 50);
    assertTrue("Second batch has 2 hosts", occuranceCount2 == 2);
  }
}
