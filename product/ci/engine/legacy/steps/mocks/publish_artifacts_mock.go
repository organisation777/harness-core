// Code generated by MockGen. DO NOT EDIT.
// Source: publish_artifacts.go

// Package steps is a generated GoMock package.
package steps

import (
	context "context"
	gomock "github.com/golang/mock/gomock"
	reflect "reflect"
)

// MockPublishArtifactsStep is a mock of PublishArtifactsStep interface.
type MockPublishArtifactsStep struct {
	ctrl     *gomock.Controller
	recorder *MockPublishArtifactsStepMockRecorder
}

// MockPublishArtifactsStepMockRecorder is the mock recorder for MockPublishArtifactsStep.
type MockPublishArtifactsStepMockRecorder struct {
	mock *MockPublishArtifactsStep
}

// NewMockPublishArtifactsStep creates a new mock instance.
func NewMockPublishArtifactsStep(ctrl *gomock.Controller) *MockPublishArtifactsStep {
	mock := &MockPublishArtifactsStep{ctrl: ctrl}
	mock.recorder = &MockPublishArtifactsStepMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockPublishArtifactsStep) EXPECT() *MockPublishArtifactsStepMockRecorder {
	return m.recorder
}

// Run mocks base method.
func (m *MockPublishArtifactsStep) Run(ctx context.Context) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Run", ctx)
	ret0, _ := ret[0].(error)
	return ret0
}

// Run indicates an expected call of Run.
func (mr *MockPublishArtifactsStepMockRecorder) Run(ctx interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Run", reflect.TypeOf((*MockPublishArtifactsStep)(nil).Run), ctx)
}
