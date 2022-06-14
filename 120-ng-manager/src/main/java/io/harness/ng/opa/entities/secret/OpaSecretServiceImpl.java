package io.harness.ng.opa.entities.secret;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.harness.account.AccountClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.connector.ConnectorDTO;
import io.harness.ng.opa.OpaEvaluationContext;
import io.harness.ng.opa.OpaService;
import io.harness.opaclient.model.OpaConstants;
import io.harness.pms.contracts.governance.GovernanceMetadata;
import io.harness.remote.client.RestClientUtils;
import io.harness.secretmanagerclient.dto.SecretDTO;

import java.io.IOException;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class OpaSecretServiceImpl implements OpaSecretService{
    private OpaService opaService;
    private AccountClient accountClient;

    public  GovernanceMetadata evaluatePoliciesWithEntity(String accountId, SecretDTO secretDTO, String orgIdentifier,
                                                          String projectIdentifier, String action, String identifier) {
        if (!RestClientUtils.getResponse(
                accountClient.isFeatureFlagEnabled(FeatureName.OPA_SECRET_GOVERNANCE.name(), accountId))) {
            return GovernanceMetadata.newBuilder()
                    .setDeny(false)
                    .setMessage(
                            String.format("FF: [%s] is disabled for account: [%s]", FeatureName.OPA_SECRET_GOVERNANCE, accountId))
                    .build();
        }

        OpaEvaluationContext context;

        try {
            String expandedYaml = getConnectorYaml(secretDTO);
            context = opaService.createEvaluationContext(expandedYaml, OpaConstants.OPA_EVALUATION_TYPE_SECRET);
            return opaService.evaluate(context, accountId, orgIdentifier, projectIdentifier, identifier, action,
                    OpaConstants.OPA_EVALUATION_TYPE_SECRET);
        } catch (IOException ex) {
            return GovernanceMetadata.newBuilder()
                    .setDeny(true)
                    .setMessage(String.format("Could not create OPA context: [%s]", ex.getMessage()))
                    .build();
        }
    }

    private String getConnectorYaml(SecretDTO secretDTO) {
        String connectorYaml = null;
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID));
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            connectorYaml = objectMapper.writeValueAsString(secretDTO);
        } catch (Exception ex) {
            log.error("Failed while converting to connector yaml format", ex);
        }
        return connectorYaml;
    }
}
