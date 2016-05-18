package software.wings.service.intfc;

import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * HttpAuditService.
 *
 * @author Rishi
 */
public interface AuditService {
  public AuditHeader create(AuditHeader header);

  public String create(AuditHeader header, RequestType requestType, byte[] httpBody);

  public void finalize(AuditHeader header, byte[] payload);

  public PageResponse<AuditHeader> list(PageRequest<AuditHeader> req);

  public void updateUser(AuditHeader header, User user);
}
