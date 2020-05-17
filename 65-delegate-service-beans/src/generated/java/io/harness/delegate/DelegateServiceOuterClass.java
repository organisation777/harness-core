// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/delegate_service.proto

package io.harness.delegate;

@javax.annotation.Generated(value = "protoc", comments = "annotations:DelegateServiceOuterClass.java.pb.meta")
public final class DelegateServiceOuterClass {
  private DelegateServiceOuterClass() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_SubmitTaskRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_SubmitTaskRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_SubmitTaskResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_SubmitTaskResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_CancelTaskRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_CancelTaskRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_CancelTaskResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_CancelTaskResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_TaskProgressRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_TaskProgressRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_TaskProgressResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_TaskProgressResponse_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\n*io/harness/delegate/delegate_service.p"
        + "roto\022\023io.harness.delegate\032\036io/harness/de"
        + "legate/task.proto\032\"io/harness/delegate/p"
        + "rogress.proto\"\313\001\n\021SubmitTaskRequest\022F\n\022s"
        + "etup_abstractions\030\001 \001(\0132*.io.harness.del"
        + "egate.TaskSetupAbstractions\0221\n\007details\030\002"
        + " \001(\0132 .io.harness.delegate.TaskDetails\022;"
        + "\n\014capabilities\030\003 \001(\0132%.io.harness.delega"
        + "te.TaskCapabilities\"A\n\022SubmitTaskRespons"
        + "e\022+\n\006taskId\030\001 \001(\0132\033.io.harness.delegate."
        + "TaskId\"@\n\021CancelTaskRequest\022+\n\006taskId\030\001 "
        + "\001(\0132\033.io.harness.delegate.TaskId\"X\n\022Canc"
        + "elTaskResponse\022B\n\021canceled_at_stage\030\001 \001("
        + "\0162\'.io.harness.delegate.TaskExecutionSta"
        + "ge\"B\n\023TaskProgressRequest\022+\n\006taskId\030\001 \001("
        + "\0132\033.io.harness.delegate.TaskId\"[\n\024TaskPr"
        + "ogressResponse\022C\n\022currently_at_stage\030\001 \001"
        + "(\0162\'.io.harness.delegate.TaskExecutionSt"
        + "age2\242\003\n\017DelegateService\022]\n\nSubmitTask\022&."
        + "io.harness.delegate.SubmitTaskRequest\032\'."
        + "io.harness.delegate.SubmitTaskResponse\022]"
        + "\n\nCancelTask\022&.io.harness.delegate.Cance"
        + "lTaskRequest\032\'.io.harness.delegate.Cance"
        + "lTaskResponse\022c\n\014TaskProgress\022(.io.harne"
        + "ss.delegate.TaskProgressRequest\032).io.har"
        + "ness.delegate.TaskProgressResponse\022l\n\023Ta"
        + "skProgressUpdates\022(.io.harness.delegate."
        + "TaskProgressRequest\032).io.harness.delegat"
        + "e.TaskProgressResponse0\001B\002P\001b\006proto3"};
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
            io.harness.delegate.Task.getDescriptor(),
            io.harness.delegate.Progress.getDescriptor(),
        },
        assigner);
    internal_static_io_harness_delegate_SubmitTaskRequest_descriptor = getDescriptor().getMessageTypes().get(0);
    internal_static_io_harness_delegate_SubmitTaskRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_SubmitTaskRequest_descriptor,
            new java.lang.String[] {
                "SetupAbstractions",
                "Details",
                "Capabilities",
            });
    internal_static_io_harness_delegate_SubmitTaskResponse_descriptor = getDescriptor().getMessageTypes().get(1);
    internal_static_io_harness_delegate_SubmitTaskResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_SubmitTaskResponse_descriptor,
            new java.lang.String[] {
                "TaskId",
            });
    internal_static_io_harness_delegate_CancelTaskRequest_descriptor = getDescriptor().getMessageTypes().get(2);
    internal_static_io_harness_delegate_CancelTaskRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_CancelTaskRequest_descriptor,
            new java.lang.String[] {
                "TaskId",
            });
    internal_static_io_harness_delegate_CancelTaskResponse_descriptor = getDescriptor().getMessageTypes().get(3);
    internal_static_io_harness_delegate_CancelTaskResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_CancelTaskResponse_descriptor,
            new java.lang.String[] {
                "CanceledAtStage",
            });
    internal_static_io_harness_delegate_TaskProgressRequest_descriptor = getDescriptor().getMessageTypes().get(4);
    internal_static_io_harness_delegate_TaskProgressRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_TaskProgressRequest_descriptor,
            new java.lang.String[] {
                "TaskId",
            });
    internal_static_io_harness_delegate_TaskProgressResponse_descriptor = getDescriptor().getMessageTypes().get(5);
    internal_static_io_harness_delegate_TaskProgressResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_TaskProgressResponse_descriptor,
            new java.lang.String[] {
                "CurrentlyAtStage",
            });
    io.harness.delegate.Task.getDescriptor();
    io.harness.delegate.Progress.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
