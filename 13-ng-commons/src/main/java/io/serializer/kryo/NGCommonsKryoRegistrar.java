package io.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.ng.core.BaseNGAccess;
import io.harness.serializer.KryoRegistrar;

public class NGCommonsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(BaseNGAccess.class, 54324);
  }
}
