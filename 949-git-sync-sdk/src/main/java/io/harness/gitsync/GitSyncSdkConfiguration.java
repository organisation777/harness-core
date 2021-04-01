package io.harness.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.AuthorizationServiceHeader;
import io.harness.EntityType;
import io.harness.Microservice;
import io.harness.annotations.dev.OwnedBy;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.redis.RedisConfig;
import io.harness.scm.ScmConnectionConfig;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class GitSyncSdkConfiguration {
  /**
   * Grpc server config which sdk will start.
   */
  GrpcServerConfig grpcServerConfig;
  /**
   * client to communicate to gms server.
   */
  Microservice microservice;
  GrpcClientConfig grpcClientConfig;
  Supplier<List<EntityType>> gitSyncSortOrder;
  RedisConfig eventsRedisConfig;
  DeployMode deployMode;
  ScmConnectionConfig scmConnectionConfig;
  AuthorizationServiceHeader serviceHeader;
  Set<GitSyncEntitiesConfiguration> gitSyncEntitiesConfiguration;
  public enum DeployMode { REMOTE, IN_PROCESS }
}
