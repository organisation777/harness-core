package io.harness.ng.opa.entities.secret;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.secretmanagerclient.dto.SecretDTO;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
public interface OpaSecretService {
    GovernanceMetadata evaluatePoliciesWithEntity(String accountId, SecretDTO secretDTO, String orgIdentifier,
                                                  String projectIdentifier, String action, String identifier);
}
