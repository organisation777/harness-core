package software.wings.graphql.datafetcher;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.app.GraphQLModule;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderOptions;
import org.dataloader.DataLoaderRegistry;
import org.dataloader.MappedBatchLoader;

@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class DataLoaderRegistryHelper {
  private Map<String, MappedBatchLoader> batchedDataLoaders;

  @Inject
  public DataLoaderRegistryHelper() {
    batchedDataLoaders = new HashMap<>();
  }

  @Inject
  public void init(Injector injector) {
    GraphQLModule.getBatchDataLoaderNames().forEach(
        l -> { batchedDataLoaders.put(l, injector.getInstance(Key.get(MappedBatchLoader.class, Names.named(l)))); });
  }

  public DataLoaderRegistry getDataLoaderRegistry() {
    DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();

    batchedDataLoaders.forEach((key, value) -> {
      DataLoader<String, Object> dataLoader =
          DataLoader.newMappedDataLoader(value, DataLoaderOptions.newOptions().setCachingEnabled(false));
      dataLoaderRegistry.register(key, dataLoader);
    });

    return dataLoaderRegistry;
  }
}
