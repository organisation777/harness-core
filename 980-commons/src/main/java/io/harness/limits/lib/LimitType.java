package io.harness.limits.lib;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public enum LimitType {
  // specifies a static limit. Example: "100 applications allowed per account"
  // A limit of 0 would mean that a particular feature if forbidden, for cases like: "Free customer can not create a
  // pipeline"
  STATIC,

  // rate limits like "10 deployments per minute allowed"
  RATE_LIMIT,
}
