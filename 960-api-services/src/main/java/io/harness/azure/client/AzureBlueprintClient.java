package io.harness.azure.client;

import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.blueprint.Blueprint;
import io.harness.azure.model.blueprint.PublishedBlueprint;
import io.harness.azure.model.blueprint.artifact.Artifact;
import io.harness.azure.model.blueprint.assignment.Assignment;
import io.harness.azure.model.blueprint.assignment.WhoIsBlueprintContract;
import io.harness.azure.model.blueprint.assignment.operation.AssignmentOperation;

import com.microsoft.azure.PagedList;

public interface AzureBlueprintClient {
  /**
   * Create or update a blueprint definition.
   *
   * @param azureConfig
   * @param resourceScope
   * @param blueprintName
   * @param blueprintJSON
   * @return
   */
  Blueprint createOrUpdateBlueprint(
      AzureConfig azureConfig, String resourceScope, String blueprintName, String blueprintJSON);

  /**
   * Get a blueprint definition.
   *
   * @param azureConfig
   * @param resourceScope
   * @param blueprintName
   * @return
   */
  Blueprint getBlueprint(AzureConfig azureConfig, String resourceScope, String blueprintName);

  /**
   * Create or update blueprint artifact.
   *
   * @param azureConfig
   * @param resourceScope
   * @param blueprintName
   * @param artifactName
   * @param artifactJSON
   * @return
   */
  Artifact createOrUpdateArtifact(
      AzureConfig azureConfig, String resourceScope, String blueprintName, String artifactName, String artifactJSON);

  /**
   * Publish a new version of the blueprint definition with the latest artifacts. Published blueprint definitions are
   * immutable.
   *
   * @param azureConfig
   * @param resourceScope
   * @param blueprintName
   * @param versionId
   * @return
   */
  PublishedBlueprint publishBlueprintDefinition(
      AzureConfig azureConfig, String resourceScope, String blueprintName, String versionId);

  /**
   * Get a published version of a blueprint definition.
   *
   * @param azureConfig
   * @param resourceScope
   * @param blueprintName
   * @param versionId
   * @return
   */
  PublishedBlueprint getPublishedBlueprintVersion(
      AzureConfig azureConfig, String resourceScope, String blueprintName, String versionId);

  /**
   * List published versions of given blueprint definition.
   *
   * @param azureConfig
   * @param resourceScope
   * @param blueprintName
   * @return
   */
  PagedList<PublishedBlueprint> listPublishedBlueprintVersions(
      AzureConfig azureConfig, String resourceScope, String blueprintName);

  /**
   * Create or update a blueprint assignment.
   *
   * @param azureConfig
   * @param resourceScope
   * @param assignmentName
   * @param assignmentJSON
   * @return
   */
  Assignment beginCreateOrUpdateAssignment(
      AzureConfig azureConfig, String resourceScope, String assignmentName, String assignmentJSON);

  /**
   * Get a blueprint assignment.
   *
   * @param azureConfig
   * @param resourceScope
   * @param assignmentName
   * @return
   */
  Assignment getAssignment(AzureConfig azureConfig, String resourceScope, String assignmentName);

  /**
   * List blueprint assignments within a subscription or a management group.
   *
   * @param azureConfig
   * @param resourceScope
   * @return
   */
  PagedList<Assignment> listAssignments(AzureConfig azureConfig, String resourceScope);

  /**
   * List operations for given blueprint assignment within a subscription or a management group.
   *
   * @param azureConfig
   * @param resourceScope
   * @param assignmentName
   * @return
   */
  PagedList<AssignmentOperation> listAssignmentOperations(
      AzureConfig azureConfig, String resourceScope, String assignmentName);

  /**
   * Get Blueprints service SPN objectId.
   *
   * @param azureConfig
   * @param resourceScope
   * @param assignmentName
   * @return
   */
  WhoIsBlueprintContract whoIsBlueprint(AzureConfig azureConfig, String resourceScope, String assignmentName);
}
