package io.harness.ccm.views.graphql;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLCEViewGroupBy {
  private QLCEViewFieldInput entityGroupBy;
  private QLCEViewTimeTruncGroupBy timeTruncGroupBy;
}
