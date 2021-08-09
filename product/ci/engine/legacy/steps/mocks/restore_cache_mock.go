// Code generated by MockGen. DO NOT EDIT.
// Source: restore_cache.go

// Package steps is a generated GoMock package.
package steps

import (
	context "context"
	gomock "github.com/golang/mock/gomock"
	reflect "reflect"
)

// MockRestoreCacheStep is a mock of RestoreCacheStep interface.
type MockRestoreCacheStep struct {
	ctrl     *gomock.Controller
	recorder *MockRestoreCacheStepMockRecorder
}

// MockRestoreCacheStepMockRecorder is the mock recorder for MockRestoreCacheStep.
type MockRestoreCacheStepMockRecorder struct {
	mock *MockRestoreCacheStep
}

// NewMockRestoreCacheStep creates a new mock instance.
func NewMockRestoreCacheStep(ctrl *gomock.Controller) *MockRestoreCacheStep {
	mock := &MockRestoreCacheStep{ctrl: ctrl}
	mock.recorder = &MockRestoreCacheStepMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockRestoreCacheStep) EXPECT() *MockRestoreCacheStepMockRecorder {
	return m.recorder
}

// Run mocks base method.
func (m *MockRestoreCacheStep) Run(ctx context.Context) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Run", ctx)
	ret0, _ := ret[0].(error)
	return ret0
}

// Run indicates an expected call of Run.
func (mr *MockRestoreCacheStepMockRecorder) Run(ctx interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Run", reflect.TypeOf((*MockRestoreCacheStep)(nil).Run), ctx)
}
