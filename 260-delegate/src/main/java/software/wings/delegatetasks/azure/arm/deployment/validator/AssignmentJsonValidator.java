package software.wings.delegatetasks.azure.arm.deployment.validator;

import static io.harness.azure.model.AzureConstants.BLUEPRINT_ID_REGEX;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.model.AzureConstants;
import io.harness.azure.model.blueprint.ResourceScopeType;
import io.harness.azure.model.blueprint.assignment.Assignment;
import io.harness.exception.InvalidArgumentsException;
import io.harness.serializer.JsonUtils;

public class AssignmentJsonValidator implements Validator<String> {
  @Override
  public void validate(String assignJson) {
    Assignment assignment = JsonUtils.asObject(assignJson, Assignment.class);

    if (assignment.getIdentity() == null) {
      throw new InvalidArgumentsException(AzureConstants.ASSIGNMENT_IDENTITY_NULL_VALIDATION_MSG);
    }
    if (isBlank(assignment.getLocation())) {
      throw new InvalidArgumentsException(AzureConstants.ASSIGNMENT_LOCATION_BLANK_VALIDATION_MSG);
    }

    Assignment.Properties properties = assignment.getProperties();
    String blueprintId = properties.getBlueprintId();
    if (isBlank(blueprintId)) {
      throw new InvalidArgumentsException(AzureConstants.PROPERTIES_BLUEPRINT_ID_VALIDATION_MSG);
    }

    if (blueprintId.endsWith("/")) {
      throw new InvalidArgumentsException(format(
          "Not valid value of properties.blueprintId property, ending with '/' character. Required format "
              + "/{resourceScope}/providers/Microsoft.Blueprint/blueprints/{blueprintName}/versions/{versionId}, blueprintId: %s",
          blueprintId));
    }

    if (!BLUEPRINT_ID_REGEX.matcher(blueprintId).matches()) {
      throw new InvalidArgumentsException(format("Not valid value of properties.blueprintId property. Required format "
              + "/{resourceScope}/providers/Microsoft.Blueprint/blueprints/{blueprintName}/versions/{versionId}, blueprintId: %s",
          blueprintId));
    }

    ResourceScopeType resourceScopeType = ResourceScopeType.fromBlueprintId(blueprintId);
    if (resourceScopeType == null) {
      throw new InvalidArgumentsException(
          format("Not found valid resource scope from properties.blueprintId: %s", blueprintId));
    }

    validateAssignmentPropertiesScope(assignment, resourceScopeType);
  }

  private void validateAssignmentPropertiesScope(Assignment assignment, ResourceScopeType resourceScopeType) {
    if (ResourceScopeType.MANAGEMENT_GROUP == resourceScopeType) {
      String scope = assignment.getProperties().getScope();
      if (isBlank(scope)) {
        throw new InvalidArgumentsException(AzureConstants.PROPERTIES_SCOPE_BLANK_VALIDATION_MSG);
      }
      if (!scope.startsWith("/subscriptions/")) {
        throw new InvalidArgumentsException(format(
            "Assignment properties.scope has to being assigned to a subscription for management group resource scope, scope: %s ",
            scope));
      }
    }
  }
}
