package software.wings.api.pcf;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.SweepingOutput;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.ResizeStrategy;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.delegate.task.pcf.PcfManifestsPackage;

import software.wings.api.pcf.InfoVariables.InfoVariablesBuilder;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("setupSweepingOutputPcf")
public class SetupSweepingOutputPcf implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "setupSweepingOutputPcf";

  private String uuid;
  private String serviceId;
  private String infraMappingId;
  private String name;
  private String commandName;
  private Integer maxInstanceCount;
  private boolean useCurrentRunningInstanceCount;
  private Integer currentRunningInstanceCount;
  private ResizeStrategy resizeStrategy;
  private CfCommandRequest pcfCommandRequest;
  private String ManifestYaml;
  private CfAppSetupTimeDetails newPcfApplicationDetails;
  private Integer totalPreviousInstanceCount;
  private List<String> tempRouteMap;
  private List<String> routeMaps;
  private Integer timeoutIntervalInMinutes;
  private List<CfAppSetupTimeDetails> appDetailsToBeDownsized;
  private CfAppSetupTimeDetails mostRecentInactiveAppVersionDetails;
  private boolean isStandardBlueGreenWorkflow;
  private boolean isDownsizeOldApps;
  private boolean isUseCfCli;
  private boolean enforceSslValidation;
  private boolean useAppAutoscalar;
  private Integer desiredActualFinalCount;
  private PcfManifestsPackage pcfManifestsPackage;
  private boolean isSuccess;
  private List<String> tags;
  private String cfAppNamePrefix;
  private boolean versioningChanged;
  private boolean nonVersioning;
  private Integer activeAppRevision;

  public InfoVariables fetchPcfVariableInfo() {
    InfoVariablesBuilder infoVariablesBuilder = InfoVariables.builder();
    if (newPcfApplicationDetails != null) {
      infoVariablesBuilder.newAppName(newPcfApplicationDetails.getApplicationName());
      infoVariablesBuilder.newAppGuid(newPcfApplicationDetails.getApplicationGuid());
      infoVariablesBuilder.newAppRoutes(newPcfApplicationDetails.getUrls());
    }

    CfAppSetupTimeDetails oldAppDetails = getOldAppDetail(appDetailsToBeDownsized);
    if (oldAppDetails != null) {
      infoVariablesBuilder.oldAppName(oldAppDetails.getApplicationName());
      infoVariablesBuilder.oldAppGuid(oldAppDetails.getApplicationGuid());
      infoVariablesBuilder.oldAppRoutes(oldAppDetails.getUrls());
    }

    if (mostRecentInactiveAppVersionDetails != null) {
      infoVariablesBuilder.mostRecentInactiveAppVersionGuid(mostRecentInactiveAppVersionDetails.getApplicationGuid());
      infoVariablesBuilder.mostRecentInactiveAppVersionName(mostRecentInactiveAppVersionDetails.getApplicationName());
      infoVariablesBuilder.mostRecentInactiveAppVersionRunningInstances(
          mostRecentInactiveAppVersionDetails.getInitialInstanceCount());
    }

    infoVariablesBuilder.finalRoutes(routeMaps);
    infoVariablesBuilder.tempRoutes(tempRouteMap);
    return infoVariablesBuilder.build();
  }

  private CfAppSetupTimeDetails getOldAppDetail(List<CfAppSetupTimeDetails> appDetailsToBeDownsized) {
    if (isNotEmpty(appDetailsToBeDownsized)) {
      return appDetailsToBeDownsized.get(0);
    }

    return null;
  }

  @Override
  public String getType() {
    return "setupSweepingOutputPcf";
  }
}
