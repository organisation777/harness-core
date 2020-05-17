// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/task.proto

package io.harness.delegate;

@javax.annotation.Generated(value = "protoc", comments = "annotations:Task.java.pb.meta")
public final class Task {
  private Task() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor internal_static_io_harness_delegate_TaskId_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_TaskId_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_TaskSetupAbstractions_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_TaskSetupAbstractions_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_TaskSetupAbstractions_ValuesEntry_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_TaskSetupAbstractions_ValuesEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_io_harness_delegate_TaskDetails_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_TaskDetails_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_TaskDetails_ExpressionsEntry_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_TaskDetails_ExpressionsEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_TaskCapabilities_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_TaskCapabilities_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\n\036io/harness/delegate/task.proto\022\023io.har"
        + "ness.delegate\032\036google/protobuf/duration."
        + "proto\032$io/harness/delegate/capability.pr"
        + "oto\"\024\n\006TaskId\022\n\n\002id\030\001 \001(\t\"\216\001\n\025TaskSetupA"
        + "bstractions\022F\n\006values\030\001 \003(\01326.io.harness"
        + ".delegate.TaskSetupAbstractions.ValuesEn"
        + "try\032-\n\013ValuesEntry\022\013\n\003key\030\001 \001(\t\022\r\n\005value"
        + "\030\002 \001(\t:\0028\001\"\227\002\n\013TaskDetails\022\014\n\004type\030\001 \001(\t"
        + "\022\030\n\016kryoParameters\030\002 \001(\014H\000\0224\n\021execution_"
        + "timeout\030\003 \001(\0132\031.google.protobuf.Duration"
        + "\022F\n\013expressions\030\004 \003(\01321.io.harness.deleg"
        + "ate.TaskDetails.ExpressionsEntry\022 \n\030expr"
        + "ession_functor_token\030\005 \001(\003\0322\n\020Expression"
        + "sEntry\022\013\n\003key\030\001 \001(\t\022\r\n\005value\030\002 \001(\t:\0028\001B\014"
        + "\n\nParameters\"A\n\020TaskCapabilities\022-\n\004list"
        + "\030\001 \003(\0132\037.io.harness.delegate.CapabilityB"
        + "\002P\001b\006proto3"};
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
            com.google.protobuf.DurationProto.getDescriptor(),
            io.harness.delegate.CapabilityOuterClass.getDescriptor(),
        },
        assigner);
    internal_static_io_harness_delegate_TaskId_descriptor = getDescriptor().getMessageTypes().get(0);
    internal_static_io_harness_delegate_TaskId_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_TaskId_descriptor,
            new java.lang.String[] {
                "Id",
            });
    internal_static_io_harness_delegate_TaskSetupAbstractions_descriptor = getDescriptor().getMessageTypes().get(1);
    internal_static_io_harness_delegate_TaskSetupAbstractions_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_TaskSetupAbstractions_descriptor,
            new java.lang.String[] {
                "Values",
            });
    internal_static_io_harness_delegate_TaskSetupAbstractions_ValuesEntry_descriptor =
        internal_static_io_harness_delegate_TaskSetupAbstractions_descriptor.getNestedTypes().get(0);
    internal_static_io_harness_delegate_TaskSetupAbstractions_ValuesEntry_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_TaskSetupAbstractions_ValuesEntry_descriptor,
            new java.lang.String[] {
                "Key",
                "Value",
            });
    internal_static_io_harness_delegate_TaskDetails_descriptor = getDescriptor().getMessageTypes().get(2);
    internal_static_io_harness_delegate_TaskDetails_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_TaskDetails_descriptor,
            new java.lang.String[] {
                "Type",
                "KryoParameters",
                "ExecutionTimeout",
                "Expressions",
                "ExpressionFunctorToken",
                "Parameters",
            });
    internal_static_io_harness_delegate_TaskDetails_ExpressionsEntry_descriptor =
        internal_static_io_harness_delegate_TaskDetails_descriptor.getNestedTypes().get(0);
    internal_static_io_harness_delegate_TaskDetails_ExpressionsEntry_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_TaskDetails_ExpressionsEntry_descriptor,
            new java.lang.String[] {
                "Key",
                "Value",
            });
    internal_static_io_harness_delegate_TaskCapabilities_descriptor = getDescriptor().getMessageTypes().get(3);
    internal_static_io_harness_delegate_TaskCapabilities_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_TaskCapabilities_descriptor,
            new java.lang.String[] {
                "List",
            });
    com.google.protobuf.DurationProto.getDescriptor();
    io.harness.delegate.CapabilityOuterClass.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
