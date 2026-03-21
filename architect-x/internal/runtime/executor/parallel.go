package executor

import (
	"bytes"
	"context"
	"fmt"
	"os"
	"os/exec"
	"sync"
	"time"

	"github.com/architect-platform/architect/internal/core/action"
	"github.com/architect-platform/architect/pkg/events"
)

// Result holds the outcome of executing an action in the DAG.
type Result struct {
	ActionID string
	Success  bool
	Output   string
	Error    error
	Duration time.Duration
	Skipped  bool
}

// Executor runs a DAG of actions with parallel execution.
type Executor struct {
	Registry    *action.Registry
	EventBus    *events.Bus
	DryRun      bool
	MaxParallel int
	Env         map[string]string
	Middleware  []Middleware
}

// Middleware wraps action execution.
type Middleware func(ctx context.Context, a *action.Action, next func(context.Context, *action.Action) *Result) *Result

// New creates a parallel DAG executor.
func New(registry *action.Registry) *Executor {
	return &Executor{
		Registry:    registry,
		EventBus:    events.NewBus(),
		MaxParallel: 4,
		Env:         make(map[string]string),
	}
}

// Execute runs an action and all its dependencies as a DAG.
func (e *Executor) Execute(ctx context.Context, actionID string) ([]*Result, error) {
	dag, err := BuildDAG(e.Registry, actionID)
	if err != nil {
		return nil, err
	}

	if e.DryRun {
		return e.dryRun(dag), nil
	}

	return e.executeDAG(ctx, dag)
}

func (e *Executor) executeDAG(ctx context.Context, dag *DAG) ([]*Result, error) {
	// Work with a copy of in-degrees so we can decrement
	inDegree := make(map[string]int)
	for id, node := range dag.Nodes {
		inDegree[id] = node.InDegree
	}

	var (
		mu       sync.Mutex
		results  []*Result
		wg       sync.WaitGroup
		sem      = make(chan struct{}, e.maxParallel())
		failed   = make(map[string]bool)
		ready    = make(chan *Node, len(dag.Nodes))
	)

	// Seed with roots
	for _, root := range dag.Roots {
		ready <- root
	}

	// Track how many nodes we still expect
	remaining := len(dag.Nodes)

	for remaining > 0 {
		select {
		case <-ctx.Done():
			return results, ctx.Err()
		case node := <-ready:
			remaining--
			wg.Add(1)
			sem <- struct{}{}

			go func(n *Node) {
				defer wg.Done()
				defer func() { <-sem }()

				// Skip if a dependency failed
				mu.Lock()
				depFailed := false
				for _, depID := range n.Action.DependsOn {
					if failed[depID] {
						depFailed = true
						break
					}
				}
				mu.Unlock()

				var result *Result
				if depFailed && !n.Action.ContinueOnError {
					result = &Result{
						ActionID: n.Action.ID,
						Success:  false,
						Skipped:  true,
						Error:    fmt.Errorf("skipped: dependency failed"),
					}
				} else if n.Action.Run != nil {
					result = e.executeAction(ctx, n.Action)
				} else {
					// Container action (has steps but no run) — just pass through
					result = &Result{
						ActionID: n.Action.ID,
						Success:  true,
					}
				}

				mu.Lock()
				results = append(results, result)
				if !result.Success {
					failed[n.Action.ID] = true
				}
				mu.Unlock()

				// Unblock children
				mu.Lock()
				for _, child := range n.Children {
					inDegree[child.Action.ID]--
					if inDegree[child.Action.ID] == 0 {
						ready <- child
					}
				}
				mu.Unlock()
			}(node)
		}
	}

	wg.Wait()
	return results, nil
}

func (e *Executor) executeAction(ctx context.Context, a *action.Action) *Result {
	// Apply timeout
	if a.Timeout != "" {
		if dur, err := time.ParseDuration(a.Timeout); err == nil {
			var cancel context.CancelFunc
			ctx, cancel = context.WithTimeout(ctx, dur)
			defer cancel()
		}
	}

	e.EventBus.Emit(events.Event{
		Type:     events.ActionStarted,
		ActionID: a.ID,
	})

	// Run through middleware chain
	exec := e.buildChain(a)
	result := exec(ctx, a)

	evtType := events.ActionCompleted
	if !result.Success {
		evtType = events.ActionFailed
	}
	e.EventBus.Emit(events.Event{
		Type:     evtType,
		ActionID: a.ID,
		Success:  result.Success,
		Duration: result.Duration,
	})

	return result
}

func (e *Executor) buildChain(a *action.Action) func(context.Context, *action.Action) *Result {
	// Base executor
	base := func(ctx context.Context, a *action.Action) *Result {
		return e.runCommand(ctx, a)
	}

	// Wrap with middleware in reverse order
	chain := base
	for i := len(e.Middleware) - 1; i >= 0; i-- {
		mw := e.Middleware[i]
		next := chain
		chain = func(ctx context.Context, a *action.Action) *Result {
			return mw(ctx, a, next)
		}
	}

	return chain
}

func (e *Executor) runCommand(ctx context.Context, a *action.Action) *Result {
	start := time.Now()

	if a.Run == nil {
		return &Result{ActionID: a.ID, Success: true, Duration: time.Since(start)}
	}

	shell := a.Run.Shell
	if shell == "" {
		shell = "sh"
	}

	cmd := exec.CommandContext(ctx, shell, "-c", a.Run.Command)
	if a.Run.WorkingDir != "" {
		cmd.Dir = a.Run.WorkingDir
	}

	cmd.Env = os.Environ()
	for k, v := range e.Env {
		cmd.Env = append(cmd.Env, k+"="+v)
	}
	for k, v := range a.Env {
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
			ActionID: a.ID,
			Success:  false,
			Output:   output,
			Error:    fmt.Errorf("command failed: %w", err),
			Duration: duration,
		}
	}

	return &Result{
		ActionID: a.ID,
		Success:  true,
		Output:   output,
		Duration: duration,
	}
}

func (e *Executor) dryRun(dag *DAG) []*Result {
	order := dag.TopologicalOrder()
	var results []*Result
	for _, a := range order {
		output := ""
		if a.Run != nil {
			output = fmt.Sprintf("[dry-run] %s", a.Run.Command)
		} else if len(a.Steps) > 0 {
			output = fmt.Sprintf("[dry-run] steps: %v", a.Steps)
		}
		results = append(results, &Result{
			ActionID: a.ID,
			Success:  true,
			Output:   output,
		})
	}
	return results
}

func (e *Executor) maxParallel() int {
	if e.MaxParallel > 0 {
		return e.MaxParallel
	}
	return 4
}
