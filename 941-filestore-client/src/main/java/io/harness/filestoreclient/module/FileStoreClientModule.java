package io.harness.filestoreclient.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import io.harness.annotations.dev.OwnedBy;
import io.harness.filestoreclient.remote.FileStoreClient;
import io.harness.filestoreclient.remote.FileStoreHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
public class FileStoreClientModule extends AbstractModule {

    private final ServiceHttpClientConfig fileStoreConfig;
    private final String serviceSecret;
    private final String clientId;
    private final ClientMode clientMode;

    public FileStoreClientModule(
            ServiceHttpClientConfig fileStoreConfig, String serviceSecret, String clientId, ClientMode clientMode) {
        this.fileStoreConfig = fileStoreConfig;
        this.serviceSecret = serviceSecret;
        this.clientId = clientId;
        this.clientMode = clientMode;
    }

    @Provides
    private FileStoreHttpClientFactory secretManagerHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
        return new FileStoreHttpClientFactory(
                fileStoreConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId, clientMode);
    }

    @Override
    protected void configure() {
        bind(FileStoreClient.class).toProvider(FileStoreHttpClientFactory.class).in(Scopes.SINGLETON);
    }

}
