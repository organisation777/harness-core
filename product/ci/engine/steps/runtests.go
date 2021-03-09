package steps

import (
	"context"
	"fmt"
	"time"

	grpc_retry "github.com/grpc-ecosystem/go-grpc-middleware/retry"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/utils"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/common/external"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

var (
	getWrkspcPath = external.GetWrkspcPath
	getChFiles    = external.GetChangedFiles
)

// RunTestsStep represents interface to execute a runTests step
type runTestsStep struct {
	id            string           // Id of the step
	name          string           // Name of the step
	runTestsInfo  *pb.RunTestsStep // Run tests step information
	containerPort uint32
	so            output.StageOutput // Output variables of the stage
	log           *zap.SugaredLogger // Logger
}

// RunTestsStep represents interface to execute a run step
type RunTestsStep interface {
	Run(ctx context.Context) (*output.StepOutput, int32, error)
}

// NewRunTestsStep creates a run step executor
func NewRunTestsStep(step *pb.UnitStep, so output.StageOutput, log *zap.SugaredLogger) RunTestsStep {
	return &runTestsStep{
		id:           step.GetId(),
		name:         step.GetDisplayName(),
		runTestsInfo: step.GetRunTests(),
		so:           so,
		log:          log,
	}
}

func (e *runTestsStep) getDiffFiles(ctx context.Context) ([]string, error) {
	workspace, err := getWrkspcPath()
	if err != nil {
		return []string{}, err
	}
	chFiles, err := getChFiles(ctx, workspace, e.log)
	if err != nil {
		e.log.Errorw("failed to get changed filed in runTests step", "step_id", e.id, zap.Error(err))
		return []string{}, err
	}

	e.log.Infow(fmt.Sprintf("using changed files list %s to figure out which tests to run", chFiles), "step_id", e.id)
	return chFiles, nil
}

// Run executes tests with provided args with retries and timeout handling
func (e *runTestsStep) Run(ctx context.Context) (*output.StepOutput, int32, error) {
	if err := e.validate(); err != nil {
		e.log.Errorw("failed to validate runTestsStep step", "step_id", e.id, zap.Error(err))
		return nil, int32(1), err
	}
	// TODO: Add JEXL resolution to fields that need to be resolved
	return e.execute(ctx)
}

// validate the container port and language
func (e *runTestsStep) validate() error {
	if e.runTestsInfo.GetContainerPort() == 0 {
		return fmt.Errorf("runTestsStep container port is not set")
	}

	if e.runTestsInfo.GetLanguage() != "java" {
		e.log.Errorw(fmt.Sprintf("only java is supported as the codebase language. Received language is: %s", e.runTestsInfo.GetLanguage()), "step_id", e.id)
		return fmt.Errorf("unsupported language in test intelligence step")
	}
	return nil
}

// execute step and send the rpc call to addon server for running tests
func (e *runTestsStep) execute(ctx context.Context) (*output.StepOutput, int32, error) {
	st := time.Now()

	diffFiles, err := e.getDiffFiles(ctx)
	if err != nil {
		return nil, int32(1), err
	}

	addonClient, err := newAddonClient(uint(e.runTestsInfo.GetContainerPort()), e.log)
	if err != nil {
		e.log.Errorw("unable to create CI addon client", "step_id", e.id, zap.Error(err))
		return nil, int32(1), errors.Wrap(err, "could not create CI Addon client")
	}
	defer addonClient.CloseConn()

	c := addonClient.Client()
	arg := e.getExecuteStepArg(diffFiles)
	ret, err := c.ExecuteStep(ctx, arg, grpc_retry.WithMax(maxAddonRetries))
	if err != nil {
		e.log.Errorw("execute run tests step RPC failed", "step_id", e.id, "elapsed_time_ms",
			utils.TimeSince(st), zap.Error(err))
		return nil, int32(1), err
	}
	e.log.Infow("successfully executed run tests step", "elapsed_time_ms", utils.TimeSince(st))
	stepOutput := &output.StepOutput{}
	stepOutput.Output.Variables = ret.GetOutput()
	return stepOutput, ret.GetNumRetries(), nil
}

func (e *runTestsStep) getExecuteStepArg(diffFiles []string) *addonpb.ExecuteStepRequest {
	// not the best practice, can take up proxying git calls later
	e.runTestsInfo.DiffFiles = diffFiles
	return &addonpb.ExecuteStepRequest{
		Step: &pb.UnitStep{
			Id:          e.id,
			DisplayName: e.name,
			Step: &pb.UnitStep_RunTests{
				RunTests: e.runTestsInfo,
			},
		},
	}
}
