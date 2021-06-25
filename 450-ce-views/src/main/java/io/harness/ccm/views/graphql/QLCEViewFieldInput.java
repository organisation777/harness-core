package io.harness.ccm.views.graphql;

import io.harness.ccm.views.entities.ViewFieldIdentifier;

import io.leangen.graphql.annotations.GraphQLNonNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLCEViewFieldInput {
  @GraphQLNonNull String fieldId;
  @GraphQLNonNull String fieldName;
  @GraphQLNonNull ViewFieldIdentifier identifier;
  String identifierName;
}
