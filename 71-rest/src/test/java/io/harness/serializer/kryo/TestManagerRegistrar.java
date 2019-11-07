package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;
import software.wings.common.cache.MongoStoreTest.TestNominalEntity;
import software.wings.common.cache.MongoStoreTest.TestOrdinalEntity;
import software.wings.service.impl.SweepingOutputServiceImplTest;
import software.wings.service.impl.WorkflowExecutionUpdateFake;
import software.wings.sm.StateMachineExecutionCallbackMock;
import software.wings.sm.StateMachineExecutorTest.CustomExecutionEventAdvisor;
import software.wings.sm.StateMachineTest.TestStateExecutionData;

public class TestManagerRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    int index = 71 * 10000;
    kryo.register(StateMachineExecutionCallbackMock.class, index++);
    kryo.register(TestStateExecutionData.class, index++);
    kryo.register(CustomExecutionEventAdvisor.class, index++);
    kryo.register(WorkflowExecutionUpdateFake.class, index++);

    kryo.register(TestNominalEntity.class, index++);
    kryo.register(TestOrdinalEntity.class, index++);

    kryo.register(SweepingOutputServiceImplTest.SweepingOutputValue.class, index++);
  }
}
