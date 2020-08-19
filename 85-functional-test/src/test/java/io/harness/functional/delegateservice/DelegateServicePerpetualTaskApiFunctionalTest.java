package io.harness.functional.delegateservice;

import static io.harness.delegate.DelegateServiceGrpc.DelegateServiceBlockingStub;
import static io.harness.rule.OwnerRule.MARKO;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.util.Durations;

import io.harness.category.element.FunctionalTests;
import io.harness.delegate.AccountId;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.perpetualtask.PerpetualTaskClientContextDetails;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.TaskClientParams;
import io.harness.perpetualtask.example.SamplePerpetualTaskParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateAsyncService;
import io.harness.threading.Poller;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.dl.WingsPersistence;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DelegateServicePerpetualTaskApiFunctionalTest extends AbstractFunctionalTest {
  @Inject private DelegateServiceBlockingStub delegateServiceBlockingStub;
  @Inject private DelegateAsyncService delegateAsyncService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private WingsPersistence wingsPersistence;

  @Test
  @Owner(developers = MARKO)
  @Category(FunctionalTests.class)
  public void testPerpetualTaskExecution() throws InterruptedException {
    DelegateServiceGrpcClient delegateServiceGrpcClient =
        new DelegateServiceGrpcClient(delegateServiceBlockingStub, delegateAsyncService, kryoSerializer);

    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put("countryName", "testCountry");

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromSeconds(30))
                                         .setTimeout(Durations.fromSeconds(30))
                                         .build();

    PerpetualTaskId perpetualTaskId = delegateServiceGrpcClient.createPerpetualTask(
        AccountId.newBuilder().setId(getAccount().getUuid()).build(), PerpetualTaskType.SAMPLE, schedule,
        PerpetualTaskClientContextDetails.newBuilder()
            .setTaskClientParams(TaskClientParams.newBuilder().putAllParams(clientParamMap).build())
            .build(),
        false, "");

    assertThat(perpetualTaskId).isNotNull();

    PerpetualTaskRecord perpetualTaskRecord = wingsPersistence.get(PerpetualTaskRecord.class, perpetualTaskId.getId());
    assertThat(perpetualTaskRecord).isNotNull();
    assertThat(perpetualTaskRecord.getState()).isEqualTo(PerpetualTaskState.TASK_UNASSIGNED);

    Poller.pollFor(Duration.ofMinutes(3), Duration.ofSeconds(5), () -> {
      PerpetualTaskRecord pTaskRecord = wingsPersistence.get(PerpetualTaskRecord.class, perpetualTaskId.getId());
      return PerpetualTaskState.TASK_RUN_SUCCEEDED == pTaskRecord.getState();
    });

    perpetualTaskRecord = wingsPersistence.get(PerpetualTaskRecord.class, perpetualTaskId.getId());
    assertThat(perpetualTaskRecord.getState()).isEqualTo(PerpetualTaskState.TASK_RUN_SUCCEEDED);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(FunctionalTests.class)
  public void testPerpetualTaskExecutionWithCachedParams() throws InterruptedException {
    String countryName = "testCountry2";

    DelegateServiceGrpcClient delegateServiceGrpcClient =
        new DelegateServiceGrpcClient(delegateServiceBlockingStub, delegateAsyncService, kryoSerializer);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromSeconds(30))
                                         .setTimeout(Durations.fromSeconds(30))
                                         .build();

    PerpetualTaskId perpetualTaskId = delegateServiceGrpcClient.createPerpetualTask(
        AccountId.newBuilder().setId(getAccount().getUuid()).build(), PerpetualTaskType.SAMPLE, schedule,
        PerpetualTaskClientContextDetails.newBuilder()
            .setExecutionBundle(
                PerpetualTaskExecutionBundle.newBuilder()
                    .setTaskParams(Any.pack(
                        SamplePerpetualTaskParams.newBuilder().setCountry(countryName).setPopulation(159).build()))
                    .build())
            .build(),
        false, "");

    assertThat(perpetualTaskId).isNotNull();

    PerpetualTaskRecord perpetualTaskRecord = wingsPersistence.get(PerpetualTaskRecord.class, perpetualTaskId.getId());
    assertThat(perpetualTaskRecord).isNotNull();
    assertThat(perpetualTaskRecord.getState()).isEqualTo(PerpetualTaskState.TASK_UNASSIGNED);

    Poller.pollFor(Duration.ofMinutes(3), Duration.ofSeconds(5), () -> {
      PerpetualTaskRecord pTaskRecord = wingsPersistence.get(PerpetualTaskRecord.class, perpetualTaskId.getId());
      return PerpetualTaskState.TASK_RUN_SUCCEEDED == pTaskRecord.getState();
    });

    perpetualTaskRecord = wingsPersistence.get(PerpetualTaskRecord.class, perpetualTaskId.getId());
    assertThat(perpetualTaskRecord.getState()).isEqualTo(PerpetualTaskState.TASK_RUN_SUCCEEDED);
  }
}