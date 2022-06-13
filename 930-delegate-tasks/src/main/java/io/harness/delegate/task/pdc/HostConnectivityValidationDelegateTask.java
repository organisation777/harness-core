/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.pdc;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.network.SafeHttpCall.execute;
import static software.wings.delegatetasks.cv.CVConstants.MAX_RETRIES;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import io.harness.annotations.dev.OwnedBy;
import io.harness.concurrent.HTimeLimiter;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.pdcconnector.HostConnectivityTaskParams;
import io.harness.delegate.beans.connector.pdcconnector.HostConnectivityTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.cf.retry.RetryPolicy;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import io.harness.filestoreclient.remote.FileStoreClient;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import org.jose4j.lang.JoseException;

import javax.ws.rs.core.Response;

@OwnedBy(CDP)
@Slf4j
public class HostConnectivityValidationDelegateTask extends AbstractDelegateRunnableTask {

  @Inject
  private FileStoreClient filesStoreClient;
  @Inject private TimeLimiter timeLimiter;

  public HostConnectivityValidationDelegateTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) throws IOException, JoseException {
    HostConnectivityTaskParams hostConnectivityTaskParams = (HostConnectivityTaskParams) parameters;
    String hostName = hostConnectivityTaskParams.getHostName();
    int port = hostConnectivityTaskParams.getPort();

    try {
      //fetchFile();
      boolean connectableHost = connectableHost(hostName, port, hostConnectivityTaskParams.getSocketTimeout());
      return HostConnectivityTaskResponse.builder().connectionSuccessful(connectableHost).build();
    } catch (Exception ex) {
      log.error("Socket Connection failed for hostName: {}, post: {} ", hostName, port, ex);
      return HostConnectivityTaskResponse.builder()
          .connectionSuccessful(false)
          .errorCode(ErrorCode.SOCKET_CONNECTION_ERROR)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }
  }

  private void fetchFile() throws IOException{
    log.info("Fetch file");
    retrofit2.Response<Response> response = filesStoreClient.downloadFile("kmpySmUISimoRrJL6NL73w", "default", "testProject", "file2").execute();
    log.info("Response: {}", response.body());

    net.jodah.failsafe.RetryPolicy<Object> retryPolicy = new net.jodah.failsafe.RetryPolicy<>()
            .handle(Exception.class)
            .withDelay(Duration.ofSeconds(5L))
            .withMaxRetries(MAX_RETRIES)
            .onFailedAttempt(event
                    -> log.info("[Retrying]: Failed updating task status attempt: {}",
                    event.getAttemptCount(), event.getLastFailure()))
            .onFailure(event
                    -> log.error("[Failed]: Failed updating task status attempt: {}",
                    event.getAttemptCount(), event.getFailure()));

    Failsafe.with(retryPolicy)
            .run(()
                    -> HTimeLimiter.callInterruptible21(timeLimiter, Duration.ofSeconds(10L),
                    ()
                            -> execute(
                            filesStoreClient.downloadFile("kmpySmUISimoRrJL6NL73w", "default", "testProject", "file2"))));
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    return null;
  }

  @Override
  public boolean isSupportingErrorFramework() {
    return true;
  }

  private boolean connectableHost(final String hostName, int port, int socketTimeout) throws Exception {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(hostName, port), socketTimeout);
      log.info(
          "Socket Connection succeeded for hostName {} on port {}, socketTimeout: {}", hostName, port, socketTimeout);
      return true;
    }
  }
}
