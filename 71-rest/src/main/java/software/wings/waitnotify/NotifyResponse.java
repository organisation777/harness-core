package software.wings.waitnotify;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import io.harness.waiter.WaitQueue;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;

/**
 * Represents response generated by a correlationId.
 */
@Entity(value = "notifyResponses", noClassnameStored = true)
@Value
@Builder
public class NotifyResponse<T extends ResponseData> extends PersistentEntity implements UuidAccess, CreatedAtAccess {
  public static final String STATUS_KEY = "status";

  public static final Duration TTL = WaitQueue.TTL.plusDays(7);

  @Id private String uuid;
  @Indexed private long createdAt;

  private T response;

  private boolean error;

  @Indexed private Date expiryTs;

  @Indexed private ExecutionStatus status = ExecutionStatus.NEW;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());
}
