package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessModule._815_CG_TRIGGERS;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.WebHookRequest;

import javax.validation.Valid;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@TargetModule(_815_CG_TRIGGERS)
public interface WebHookService {
  Response execute(@NotEmpty String token, @Valid WebHookRequest webHookRequest, HttpHeaders httpHeaders);
  Response executeByEvent(@NotEmpty(message = "Token can not be empty") String token,
      @NotEmpty(message = "Payload can not be empty") String webhookEventPayload, HttpHeaders httpHeaders);
}
