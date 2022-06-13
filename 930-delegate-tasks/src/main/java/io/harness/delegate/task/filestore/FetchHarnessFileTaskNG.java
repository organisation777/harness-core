package io.harness.delegate.task.filestore;

import com.google.inject.Inject;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.filestoreclient.remote.FileStoreClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.jose4j.lang.JoseException;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class FetchHarnessFileTaskNG extends AbstractDelegateRunnableTask {

    @Inject
    public FetchHarnessFileTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute, FileStoreClient filesStoreClient) {
        super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
        this.filesStoreClient = filesStoreClient;
    }

    private final FileStoreClient filesStoreClient;

    @Override
    public DelegateResponseData run(Object[] parameters) {
        throw new NotImplementedException("Not implemented");
    }

        @Override
    public DelegateResponseData run(TaskParameters parameters) throws IOException, JoseException {
        try {
            FetchFileTaskNgParams fileStoreParams = (FetchFileTaskNgParams) parameters;
            log.info("Retrieving file {}", fileStoreParams.getIdentifier());
            retrofit2.Response<Response> response = filesStoreClient.downloadFile(fileStoreParams.getAccountIdentifier(), fileStoreParams.getOrgIdentifier(), fileStoreParams.getProjectIdentifier(), fileStoreParams.getIdentifier()).execute();
            log.info("Retrieving file content for file: {}", fileStoreParams.getIdentifier());
            if (response != null && response.body() != null) {
                log.info("Fetching file content for file {}", fileStoreParams.getIdentifier());
                return FetchHarnessFileTaskNGResponse.builder().file(response.body()).build();
            }
        } catch (IOException ex) {
            log.error("Failed to execute task.", ex);
            throw(ex);
        }
        return FetchHarnessFileTaskNGResponse.builder().build();
    }
}
