package io.harness.logstreaming;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.network.SafeHttpCall;

import java.util.Collections;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
@Slf4j
public class LogStreamingStepClientImpl implements ILogStreamingStepClient {
  public static final String COMMAND_UNIT_PLACEHOLDER = "-commandUnit:%s";

  private final LogStreamingClient logStreamingClient;
  private final LogStreamingSanitizer logStreamingSanitizer;
  private final String token;
  private final String accountId;
  private final String baseLogKey;

  @Override
  public void openStream(String logKeySuffix) {
    try {
      String logKey = generateLogKey(baseLogKey, logKeySuffix);
      SafeHttpCall.executeWithExceptions(logStreamingClient.openLogStream(token, accountId, logKey));
    } catch (Exception ex) {
      throw new InvalidRequestException(ex.getMessage() + "\nPlease ensure log service is running.", ex);
    }
  }

  @Override
  public void closeStream(String logKeySuffix) {
    try {
      String logKey = generateLogKey(baseLogKey, logKeySuffix);
      SafeHttpCall.executeWithExceptions(logStreamingClient.closeLogStream(token, accountId, logKey, true));
    } catch (Exception ex) {
      throw new InvalidRequestException(ex.getMessage() + "\nPlease ensure log service is running.", ex);
    }
  }

  @Override
  public void writeLogLine(LogLine logLine, String logKeySuffix) {
    try {
      String logKey = generateLogKey(baseLogKey, logKeySuffix);
      logStreamingSanitizer.sanitizeLogMessage(logLine);
      LogStreamingHelper.colorLog(logLine);

      SafeHttpCall.executeWithExceptions(
          logStreamingClient.pushMessage(token, accountId, logKey, Collections.singletonList(logLine)));
    } catch (Exception ex) {
      throw new InvalidRequestException(ex.getMessage() + "\nPlease ensure log service is running.", ex);
    }
  }

  private String generateLogKey(String baseLogKey, String logKeySuffix) {
    return logKeySuffix == null ? baseLogKey
                                : LogStreamingHelper.generateLogKeyGivenCommandUnit(baseLogKey, logKeySuffix);
  }
}
