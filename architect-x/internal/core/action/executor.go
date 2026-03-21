package action

import (
	"bytes"
	"context"
	"fmt"
	"os"
	"os/exec"
	"strings"
	"time"
)

// Result holds the outcome of executing an action.
type Result struct {
	ActionID string
	Success  bool
	Output   string
	Error    error
	Duration time.Duration
}

// Executor runs actions.
type Executor struct {
	Registry *Registry
	DryRun   bool
	Env      map[string]string
}

// NewExecutor creates a new action executor.
func NewExecutor(registry *Registry) *Executor {
	return &Executor{
		Registry: registry,
		Env:      make(map[string]string),
	}
}

// Execute runs an action by ID.
func (e *Executor) Execute(ctx context.Context, actionID string) (*Result, error) {
	action, err := e.Registry.Get(actionID)
	if err != nil {
		return nil, err
	}
	return e.executeAction(ctx, action)
}

func (e *Executor) executeAction(ctx context.Context, action *Action) (*Result, error) {
	// Apply timeout if specified
	if action.Timeout != "" {
		dur, err := time.ParseDuration(action.Timeout)
		if err != nil {
			return nil, fmt.Errorf("invalid timeout %q for action %q: %w", action.Timeout, action.ID, err)
		}
		var cancel context.CancelFunc
		ctx, cancel = context.WithTimeout(ctx, dur)
		defer cancel()
	}

	// If action has steps, execute them
	if len(action.Steps) > 0 {
		return e.executeSteps(ctx, action)
	}

	// If action has a run spec, execute the command
	if action.Run != nil {
		return e.executeRun(ctx, action)
	}

	return &Result{
		ActionID: action.ID,
		Success:  true,
		Output:   "no-op",
	}, nil
}

func (e *Executor) executeSteps(ctx context.Context, action *Action) (*Result, error) {
	start := time.Now()
	var outputs []string

	for _, stepID := range action.Steps {
		stepAction, err := e.Registry.Get(stepID)
		if err != nil {
			if action.ContinueOnError {
				outputs = append(outputs, fmt.Sprintf("[%s] error: %v", stepID, err))
				continue
			}
			return &Result{
				ActionID: action.ID,
				Success:  false,
				Error:    fmt.Errorf("step %q: %w", stepID, err),
				Duration: time.Since(start),
			}, nil
		}

		result, err := e.executeAction(ctx, stepAction)
		if err != nil {
			if action.ContinueOnError {
				outputs = append(outputs, fmt.Sprintf("[%s] error: %v", stepID, err))
				continue
			}
			return &Result{
				ActionID: action.ID,
				Success:  false,
				Error:    err,
				Duration: time.Since(start),
			}, nil
		}

		if !result.Success {
			if action.ContinueOnError {
				outputs = append(outputs, fmt.Sprintf("[%s] failed: %v", stepID, result.Error))
				continue
			}
			return result, nil
		}

		if result.Output != "" {
			outputs = append(outputs, result.Output)
		}
	}

	return &Result{
		ActionID: action.ID,
		Success:  true,
		Output:   strings.Join(outputs, "\n"),
		Duration: time.Since(start),
	}, nil
}

func (e *Executor) executeRun(ctx context.Context, action *Action) (*Result, error) {
	start := time.Now()

	if e.DryRun {
		return &Result{
			ActionID: action.ID,
			Success:  true,
			Output:   fmt.Sprintf("[dry-run] %s", action.Run.Command),
			Duration: time.Since(start),
		}, nil
	}

	shell := action.Run.Shell
	if shell == "" {
		shell = "sh"
	}

	cmd := exec.CommandContext(ctx, shell, "-c", action.Run.Command)

	// Set working directory
	if action.Run.WorkingDir != "" {
		cmd.Dir = action.Run.WorkingDir
	}

	// Merge environment
	cmd.Env = os.Environ()
	for k, v := range e.Env {
		cmd.Env = append(cmd.Env, k+"="+v)
	}
	for k, v := range action.Env {
		cmd.Env = append(cmd.Env, k+"="+v)
	}

	var stdout, stderr bytes.Buffer
	cmd.Stdout = &stdout
	cmd.Stderr = &stderr

	err := cmd.Run()
	duration := time.Since(start)

	output := stdout.String()
	if stderr.Len() > 0 {
		if output != "" {
			output += "\n"
		}
		output += stderr.String()
	}

	if err != nil {
		return &Result{
			ActionID: action.ID,
			Success:  false,
			Output:   output,
			Error:    fmt.Errorf("command failed: %w", err),
			Duration: duration,
		}, nil
	}

	return &Result{
		ActionID: action.ID,
		Success:  true,
		Output:   output,
		Duration: duration,
	}, nil
}
