// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/ng_progress.proto

package io.harness.delegate;

@javax.annotation.Generated(value = "protoc", comments = "annotations:NgProgress.java.pb.meta")
public final class NgProgress {
  private NgProgress() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\n%io/harness/delegate/ng_progress.proto\022"
        + "\023io.harness.delegate*\200\001\n\024NgTaskExecution"
        + "Stage\022\024\n\020TYPE_UNSPECIFIED\020\000\022\014\n\010QUEUEING\020"
        + "\001\022\016\n\nVALIDATING\020\002\022\r\n\tEXECUTING\020\003\022\014\n\010FINI"
        + "SHED\020\004\022\n\n\006FAILED\020\005\022\013\n\007ABORTED\020\006B\002P\001b\006pro"
        + "to3"};
    descriptor = com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
        descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] {});
  }

  // @@protoc_insertion_point(outer_class_scope)
}
