package io.harness.pms.creator;

import static java.lang.String.format;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.pms.plan.PlanCreationBlobRequest;
import io.harness.pms.plan.PlanCreationBlobResponse;
import io.harness.pms.plan.PlanCreationServiceGrpc.PlanCreationServiceBlockingStub;
import io.harness.pms.plan.YamlFieldBlob;
import io.harness.pms.plan.common.creator.PlanCreationBlobResponseUtils;
import io.harness.pms.plan.common.creator.PlanCreatorUtils;
import io.harness.pms.plan.common.utils.CompletableFutures;
import io.harness.pms.plan.common.yaml.YamlField;
import io.harness.pms.plan.common.yaml.YamlNode;
import io.harness.pms.plan.common.yaml.YamlUtils;
import io.harness.pms.service.PmsSdkInstanceService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class PlanCreatorMergeService {
  private static final int MAX_DEPTH = 10;

  private final Executor executor = Executors.newFixedThreadPool(5);

  private final Map<String, PlanCreationServiceBlockingStub> planCreatorServices;
  private final PmsSdkInstanceService pmsSdkInstanceService;

  @Inject
  public PlanCreatorMergeService(
      Map<String, PlanCreationServiceBlockingStub> planCreatorServices, PmsSdkInstanceService pmsSdkInstanceService) {
    this.planCreatorServices = planCreatorServices;
    this.pmsSdkInstanceService = pmsSdkInstanceService;
  }

  public PlanCreationBlobResponse createPlan(@NotNull String content) throws IOException {
    Map<String, Map<String, Set<String>>> sdkInstances = pmsSdkInstanceService.getSdkInstancesMap();
    Map<String, PlanCreatorServiceInfo> services = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(planCreatorServices) && EmptyPredicate.isNotEmpty(sdkInstances)) {
      sdkInstances.forEach((k, v) -> {
        if (planCreatorServices.containsKey(k)) {
          services.put(k, new PlanCreatorServiceInfo(v, planCreatorServices.get(k)));
        }
      });
    }

    String finalContent = preprocessYaml(content);
    YamlField rootYamlField = YamlUtils.readTree(finalContent);
    YamlField pipelineField = extractPipelineField(rootYamlField);
    Map<String, YamlFieldBlob> dependencies = new HashMap<>();
    dependencies.put(pipelineField.getNode().getUuid(), pipelineField.toFieldBlob());
    PlanCreationBlobResponse finalResponse = createPlanForDependenciesRecursive(services, dependencies);
    validatePlanCreationBlobResponse(finalResponse);
    return finalResponse;
  }

  private PlanCreationBlobResponse createPlanForDependenciesRecursive(
      Map<String, PlanCreatorServiceInfo> services, Map<String, YamlFieldBlob> initialDependencies) {
    PlanCreationBlobResponse.Builder finalResponseBuilder =
        PlanCreationBlobResponse.newBuilder().putAllDependencies(initialDependencies);
    if (EmptyPredicate.isEmpty(services) || EmptyPredicate.isEmpty(initialDependencies)) {
      return finalResponseBuilder.build();
    }

    for (int i = 0; i < MAX_DEPTH && EmptyPredicate.isNotEmpty(finalResponseBuilder.getDependenciesMap()); i++) {
      PlanCreationBlobResponse currIterationResponse =
          createPlanForDependencies(services, finalResponseBuilder.getDependenciesMap());
      PlanCreationBlobResponseUtils.addNodes(finalResponseBuilder, currIterationResponse.getNodesMap());
      PlanCreationBlobResponseUtils.mergeStartingNodeId(
          finalResponseBuilder, currIterationResponse.getStartingNodeId());
      if (EmptyPredicate.isNotEmpty(finalResponseBuilder.getDependenciesMap())) {
        throw new InvalidRequestException("Some YAML nodes could not be parsed");
      }

      PlanCreationBlobResponseUtils.addDependencies(finalResponseBuilder, currIterationResponse.getDependenciesMap());
    }

    return finalResponseBuilder.build();
  }

  private PlanCreationBlobResponse createPlanForDependencies(
      Map<String, PlanCreatorServiceInfo> services, Map<String, YamlFieldBlob> dependencies) {
    PlanCreationBlobResponse.Builder currIterationResponseBuilder = PlanCreationBlobResponse.newBuilder();
    CompletableFutures<PlanCreationBlobResponse> completableFutures = new CompletableFutures<>(executor);
    for (Map.Entry<String, PlanCreatorServiceInfo> serviceEntry : services.entrySet()) {
      Map<String, Set<String>> supportedTypes = serviceEntry.getValue().getSupportedTypes();
      Map<String, YamlFieldBlob> filteredDependencies =
          dependencies.entrySet()
              .stream()
              .filter(entry -> {
                try {
                  YamlField field = YamlField.fromFieldBlob(entry.getValue());
                  return PlanCreatorUtils.supportsField(supportedTypes, field);
                } catch (IOException e) {
                  log.error("Invalid yaml field", e);
                  return false;
                }
              })
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      if (EmptyPredicate.isEmpty(filteredDependencies)) {
        continue;
      }

      completableFutures.supplyAsync(() -> {
        try {
          return serviceEntry.getValue().getPlanCreationClient().createPlan(
              PlanCreationBlobRequest.newBuilder().putAllDependencies(filteredDependencies).build());
        } catch (Exception ex) {
          log.error("Error fetching partial plan from service " + serviceEntry.getKey(), ex);
          return null;
        }
      });
    }

    try {
      List<PlanCreationBlobResponse> planCreationBlobResponses = completableFutures.allOf().get(5, TimeUnit.MINUTES);
      planCreationBlobResponses.forEach(
          resp -> PlanCreationBlobResponseUtils.merge(currIterationResponseBuilder, resp));
    } catch (Exception ex) {
      throw new UnexpectedException("Error fetching plan creation response from service", ex);
    }

    return currIterationResponseBuilder.build();
  }

  private void validatePlanCreationBlobResponse(PlanCreationBlobResponse finalResponse) {
    if (EmptyPredicate.isNotEmpty(finalResponse.getDependenciesMap())) {
      throw new InvalidRequestException(
          format("Unable to interpret nodes: %s", finalResponse.getDependenciesMap().keySet().toString()));
    }
    if (EmptyPredicate.isEmpty(finalResponse.getStartingNodeId())) {
      throw new InvalidRequestException("Unable to find out starting node");
    }
  }

  private String preprocessYaml(@NotNull String content) throws IOException {
    return YamlUtils.injectUuid(content);
  }

  private YamlField extractPipelineField(YamlField rootYamlField) {
    YamlNode rootYamlNode = rootYamlField.getNode();
    return Preconditions.checkNotNull(
        getPipelineField(rootYamlNode), "Invalid pipeline YAML: root of the yaml needs to be an object");
  }

  private YamlField getPipelineField(YamlNode rootYamlNode) {
    return (rootYamlNode == null || !rootYamlNode.isObject()) ? null : rootYamlNode.getField("pipeline");
  }
}
