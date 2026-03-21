package executor

import (
	"context"
	"strings"
	"testing"
	"time"

	"github.com/architect-platform/architect/internal/core/action"
)

func TestBuildDAGSimple(t *testing.T) {
	r := action.NewRegistry()
	r.Register(&action.Action{ID: "a", Run: &action.RunSpec{Command: "echo a"}})
	r.Register(&action.Action{ID: "b", DependsOn: []string{"a"}, Run: &action.RunSpec{Command: "echo b"}})
	r.Register(&action.Action{ID: "c", DependsOn: []string{"a"}, Run: &action.RunSpec{Command: "echo c"}})

	dag, err := BuildDAG(r, "b")
	if err != nil {
		t.Fatal(err)
	}

	if len(dag.Nodes) != 2 {
		t.Errorf("expected 2 nodes, got %d", len(dag.Nodes))
	}
	if len(dag.Roots) != 1 {
		t.Errorf("expected 1 root, got %d", len(dag.Roots))
	}
	if dag.Roots[0].Action.ID != "a" {
		t.Errorf("expected root 'a', got %q", dag.Roots[0].Action.ID)
	}
}

func TestBuildDAGWithSteps(t *testing.T) {
	r := action.NewRegistry()
	r.Register(&action.Action{ID: "build", Steps: []string{"compile", "link"}})
	r.Register(&action.Action{ID: "compile", Run: &action.RunSpec{Command: "echo compile"}})
	r.Register(&action.Action{ID: "link", Run: &action.RunSpec{Command: "echo link"}})

	dag, err := BuildDAG(r, "build")
	if err != nil {
		t.Fatal(err)
	}

	if len(dag.Nodes) != 3 {
		t.Errorf("expected 3 nodes, got %d", len(dag.Nodes))
	}
}

func TestBuildDAGCycleDetection(t *testing.T) {
	r := action.NewRegistry()
	r.Register(&action.Action{ID: "a", DependsOn: []string{"b"}, Run: &action.RunSpec{Command: "echo a"}})
	r.Register(&action.Action{ID: "b", DependsOn: []string{"a"}, Run: &action.RunSpec{Command: "echo b"}})

	_, err := BuildDAG(r, "a")
	if err == nil {
		t.Fatal("expected cycle detection error")
	}
	if !strings.Contains(err.Error(), "cycle") {
		t.Errorf("expected cycle error, got: %v", err)
	}
}

func TestTopologicalOrder(t *testing.T) {
	r := action.NewRegistry()
	r.Register(&action.Action{ID: "a", Run: &action.RunSpec{Command: "echo a"}})
	r.Register(&action.Action{ID: "b", DependsOn: []string{"a"}, Run: &action.RunSpec{Command: "echo b"}})
	r.Register(&action.Action{ID: "c", DependsOn: []string{"b"}, Run: &action.RunSpec{Command: "echo c"}})

	dag, err := BuildDAG(r, "c")
	if err != nil {
		t.Fatal(err)
	}

	order := dag.TopologicalOrder()
	if len(order) != 3 {
		t.Fatalf("expected 3 actions, got %d", len(order))
	}

	// a must come before b, b before c
	indexOf := func(id string) int {
		for i, a := range order {
			if a.ID == id {
				return i
			}
		}
		return -1
	}

	if indexOf("a") > indexOf("b") {
		t.Error("a should come before b")
	}
	if indexOf("b") > indexOf("c") {
		t.Error("b should come before c")
	}
}

func TestParallelExecution(t *testing.T) {
	r := action.NewRegistry()
	r.Register(&action.Action{ID: "root", Steps: []string{"a", "b"}, Parallel: true})
	r.Register(&action.Action{ID: "a", Run: &action.RunSpec{Command: "sleep 0.1 && echo a"}})
	r.Register(&action.Action{ID: "b", Run: &action.RunSpec{Command: "sleep 0.1 && echo b"}})

	e := New(r)
	e.MaxParallel = 4

	start := time.Now()
	results, err := e.Execute(context.Background(), "root")
	if err != nil {
		t.Fatal(err)
	}
	elapsed := time.Since(start)

	// With parallel execution, both should run concurrently
	// so total should be ~100ms not ~200ms
	if elapsed > 180*time.Millisecond {
		t.Errorf("expected parallel execution (~100ms), took %v", elapsed)
	}

	for _, r := range results {
		if !r.Success && !r.Skipped && r.ActionID != "root" {
			t.Errorf("action %s failed: %v", r.ActionID, r.Error)
		}
	}
}

func TestSequentialSteps(t *testing.T) {
	r := action.NewRegistry()
	// Sequential: first must finish before second
	r.Register(&action.Action{ID: "pipeline", Steps: []string{"first", "second"}})
	r.Register(&action.Action{ID: "first", Run: &action.RunSpec{Command: "echo first"}})
	r.Register(&action.Action{ID: "second", Run: &action.RunSpec{Command: "echo second"}})

	e := New(r)
	results, err := e.Execute(context.Background(), "pipeline")
	if err != nil {
		t.Fatal(err)
	}

	for _, r := range results {
		if !r.Success && r.Error != nil {
			t.Errorf("action %s failed: %v", r.ActionID, r.Error)
		}
	}
}

func TestDryRun(t *testing.T) {
	r := action.NewRegistry()
	r.Register(&action.Action{ID: "deploy", Run: &action.RunSpec{Command: "rm -rf /"}})

	e := New(r)
	e.DryRun = true

	results, err := e.Execute(context.Background(), "deploy")
	if err != nil {
		t.Fatal(err)
	}

	if len(results) != 1 {
		t.Fatalf("expected 1 result, got %d", len(results))
	}
	if !results[0].Success {
		t.Error("dry-run should succeed")
	}
	if !strings.Contains(results[0].Output, "[dry-run]") {
		t.Errorf("expected dry-run marker, got %q", results[0].Output)
	}
}

func TestMiddleware(t *testing.T) {
	r := action.NewRegistry()
	r.Register(&action.Action{ID: "test", Run: &action.RunSpec{Command: "echo hi"}})

	var middlewareCalled bool
	e := New(r)
	e.Middleware = append(e.Middleware, func(ctx context.Context, a *action.Action, next func(context.Context, *action.Action) *Result) *Result {
		middlewareCalled = true
		return next(ctx, a)
	})

	_, err := e.Execute(context.Background(), "test")
	if err != nil {
		t.Fatal(err)
	}

	if !middlewareCalled {
		t.Error("middleware was not called")
	}
}
