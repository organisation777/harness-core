package io.harness.plan;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;
import io.harness.pms.contracts.plan.GraphLayoutInfo;
import io.harness.pms.contracts.plan.PlanNodeProto;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * This is the plan we want to execute during the execution
 * It contains a list of ExecutionNode and stating point
 * This is contained as a list sorted by the uuid to quick retrieval.
 *
 * Do not want this to be a map as we will lost the ability to query the database by node properties
 * This will be required by the apps to performs some migrations etc.
 *
 * This was a major pain point for the design of our StateMachine.
 *
 * With this approach we can crate iterators over these and perform the migrations
 */

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "PlanKeys")
@Document("plans")
@TypeAlias("plan")
@Entity(value = "plans")
@StoreIn(DbAliases.PMS)
public class Plan implements PersistentEntity {
  static final long TTL_MONTHS = 6;

  @Default @Wither @Id @org.mongodb.morphia.annotations.Id String uuid = generateUuid();
  @Singular List<PlanNodeProto> nodes;

  @NotNull String startingNodeId;

  Map<String, String> setupAbstractions;
  GraphLayoutInfo graphLayoutInfo;

  @Default @FdTtlIndex Date validUntil = Date.from(OffsetDateTime.now().plusMonths(TTL_MONTHS).toInstant());

  @Wither @CreatedDate Long createdAt;
  @Wither @Version Long version;

  public boolean isEmpty() {
    return EmptyPredicate.isEmpty(nodes);
  }

  public PlanNodeProto fetchStartingNode() {
    return fetchNode(startingNodeId);
  }

  public PlanNodeProto fetchNode(String nodeId) {
    Optional<PlanNodeProto> optional = nodes.stream().filter(pn -> pn.getUuid().equals(nodeId)).findFirst();
    if (optional.isPresent()) {
      return optional.get();
    }
    throw new InvalidRequestException("No node found with Id :" + nodeId);
  }
}
