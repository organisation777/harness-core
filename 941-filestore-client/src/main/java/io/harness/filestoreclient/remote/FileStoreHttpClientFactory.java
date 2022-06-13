package io.harness.filestoreclient.remote;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class FileStoreHttpClientFactory extends AbstractHttpClientFactory implements Provider<FileStoreClient> {
    public FileStoreHttpClientFactory(ServiceHttpClientConfig fileStoreClientConfig, String serviceSecret,
                                      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId,
                                      ClientMode clientMode) {
        super(fileStoreClientConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId, false, clientMode);
    }

    @Override
    public FileStoreClient get() {
        return getRetrofit().create(FileStoreClient.class);
    }
}
