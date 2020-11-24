package grpc

import (
	"context"
	"fmt"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

func TestUpdateUnknownStatus(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	arg := &pb.UpdateStateRequest{}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewEngineHandler(log.Sugar())
	_, err := h.UpdateState(ctx, arg)
	assert.NotNil(t, err)
}

func TestUpdateToPause(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	arg := &pb.UpdateStateRequest{
		Action: pb.UpdateStateRequest_PAUSE,
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewEngineHandler(log.Sugar())
	_, err := h.UpdateState(ctx, arg)
	assert.Nil(t, err)
}

func TestUpdateToResume(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	arg := &pb.UpdateStateRequest{
		Action: pb.UpdateStateRequest_RESUME,
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewEngineHandler(log.Sugar())
	_, err := h.UpdateState(ctx, arg)
	assert.Nil(t, err)
}

func TestGetImageEntrypointWithNoImage(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	arg := &pb.GetImageEntrypointRequest{}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewEngineHandler(log.Sugar())
	_, err := h.GetImageEntrypoint(ctx, arg)
	assert.NotNil(t, err)
}

func TestGetImageEntrypointWithNoSecretSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	commands := []string{"git"}
	arg := &pb.GetImageEntrypointRequest{
		Id:    "git",
		Image: "plugins/git",
	}

	oldImgMetadata := getPublicImgMetadata
	getPublicImgMetadata = func(image string) ([]string, []string, error) {
		return commands, nil, nil
	}
	defer func() { getPublicImgMetadata = oldImgMetadata }()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewEngineHandler(log.Sugar())
	_, err := h.GetImageEntrypoint(ctx, arg)
	assert.Nil(t, err)
}

func TestGetImageEntrypointWithSecretSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	commands := []string{"git"}
	arg := &pb.GetImageEntrypointRequest{
		Id:     "git",
		Image:  "plugins/git",
		Secret: "foo",
	}

	oldImgMetadata := getPrivateImgMetadata
	getPrivateImgMetadata = func(image, secret string) ([]string, []string, error) {
		return commands, nil, nil
	}
	defer func() { getPrivateImgMetadata = oldImgMetadata }()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewEngineHandler(log.Sugar())
	_, err := h.GetImageEntrypoint(ctx, arg)
	assert.Nil(t, err)
}

func TestGetImageEntrypointWithSecretErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	arg := &pb.GetImageEntrypointRequest{
		Id:     "git",
		Image:  "plugins/git",
		Secret: "foo",
	}

	oldImgMetadata := getPrivateImgMetadata
	getPrivateImgMetadata = func(image, secret string) ([]string, []string, error) {
		return nil, nil, fmt.Errorf("failed to find entrypoint")
	}
	defer func() { getPrivateImgMetadata = oldImgMetadata }()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewEngineHandler(log.Sugar())
	_, err := h.GetImageEntrypoint(ctx, arg)
	assert.NotNil(t, err)
}
