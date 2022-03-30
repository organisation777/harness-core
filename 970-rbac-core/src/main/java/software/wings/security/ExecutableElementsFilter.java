package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.security.ExecutableElementsFilter.FilterType.PIPELINE;
import static software.wings.security.ExecutableElementsFilter.FilterType.WORKFLOW;

import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExecutableElementsFilter extends EnvFilter {
  public interface FilterType {
    String WORKFLOW = "WORKFLOW";
    String PIPELINE = "PIPELINE";
  }

  static boolean isValidFilterType(String filterType) {
    switch (filterType) {
      case WORKFLOW:
      case PIPELINE:
        return true;
      default:
        return false;
    }
  }

  private String executableElementFilterType;
  private GenericEntityFilter filter;

  @Builder(builderMethodName = "executableBuilder")
  public ExecutableElementsFilter(
      Set<String> envIds, Set<String> envTypes, String executableElementType, GenericEntityFilter filter) {
    super(envIds, envTypes);
    this.executableElementFilterType = executableElementType;
    this.filter = filter;
  }
}