package action

import (
	"context"
	"strings"
	"testing"
)

func TestRegistryRegisterAndGet(t *testing.T) {
	r := NewRegistry()
	a := &Action{ID: "test-action", Description: "A test"}

	if err := r.Register(a); err != nil {
		t.Fatalf("Register failed: %v", err)
	}

	got, err := r.Get("test-action")
	if err != nil {
		t.Fatalf("Get failed: %v", err)
	}
	if got.ID != "test-action" {
		t.Errorf("expected ID 'test-action', got %q", got.ID)
	}
}

func TestRegistryDuplicateRegister(t *testing.T) {
	r := NewRegistry()
	a := &Action{ID: "dup"}
	if err := r.Register(a); err != nil {
		t.Fatal(err)
	}
	if err := r.Register(&Action{ID: "dup"}); err == nil {
		t.Fatal("expected error for duplicate registration")
	}
}

func TestRegistryGetNotFound(t *testing.T) {
	r := NewRegistry()
	_, err := r.Get("nonexistent")
	if err == nil {
		t.Fatal("expected error for missing action")
	}
}

func TestRegistryAll(t *testing.T) {
	r := NewRegistry()
	r.Register(&Action{ID: "a"})
	r.Register(&Action{ID: "b"})

	all := r.All()
	if len(all) != 2 {
		t.Fatalf("expected 2 actions, got %d", len(all))
	}
}

func TestRegistryResolveAttachments(t *testing.T) {
	r := NewRegistry()
	r.Register(&Action{ID: "build", Steps: []string{}})
	r.Register(&Action{ID: "compile", AttachTo: "build"})

	if err := r.ResolveAttachments(); err != nil {
		t.Fatalf("ResolveAttachments failed: %v", err)
	}

	build, _ := r.Get("build")
	if len(build.Steps) != 1 || build.Steps[0] != "compile" {
		t.Errorf("expected build.Steps=[compile], got %v", build.Steps)
	}
}

func TestExecutorRunCommand(t *testing.T) {
	r := NewRegistry()
	r.Register(&Action{
		ID:  "hello",
		Run: &RunSpec{Command: "echo hello world"},
	})

	e := NewExecutor(r)
	result, err := e.Execute(context.Background(), "hello")
	if err != nil {
		t.Fatal(err)
	}
	if !result.Success {
		t.Fatalf("expected success, got error: %v", result.Error)
	}
	if !strings.Contains(result.Output, "hello world") {
		t.Errorf("expected output to contain 'hello world', got %q", result.Output)
	}
}

func TestExecutorRunSteps(t *testing.T) {
	r := NewRegistry()
	r.Register(&Action{
		ID:    "all",
		Steps: []string{"step1", "step2"},
	})
	r.Register(&Action{ID: "step1", Run: &RunSpec{Command: "echo one"}})
	r.Register(&Action{ID: "step2", Run: &RunSpec{Command: "echo two"}})

	e := NewExecutor(r)
	result, err := e.Execute(context.Background(), "all")
	if err != nil {
		t.Fatal(err)
	}
	if !result.Success {
		t.Fatalf("expected success: %v", result.Error)
	}
	if !strings.Contains(result.Output, "one") || !strings.Contains(result.Output, "two") {
		t.Errorf("expected both step outputs, got %q", result.Output)
	}
}

func TestExecutorDryRun(t *testing.T) {
	r := NewRegistry()
	r.Register(&Action{ID: "deploy", Run: &RunSpec{Command: "rm -rf /"}})

	e := NewExecutor(r)
	e.DryRun = true
	result, err := e.Execute(context.Background(), "deploy")
	if err != nil {
		t.Fatal(err)
	}
	if !result.Success {
		t.Fatal("dry-run should succeed")
	}
	if !strings.Contains(result.Output, "[dry-run]") {
		t.Errorf("expected dry-run marker in output, got %q", result.Output)
	}
}

func TestExecutorFailedCommand(t *testing.T) {
	r := NewRegistry()
	r.Register(&Action{ID: "fail", Run: &RunSpec{Command: "exit 1"}})

	e := NewExecutor(r)
	result, err := e.Execute(context.Background(), "fail")
	if err != nil {
		t.Fatal(err)
	}
	if result.Success {
		t.Fatal("expected failure")
	}
}

func TestExecutorContinueOnError(t *testing.T) {
	r := NewRegistry()
	r.Register(&Action{
		ID:              "all",
		Steps:           []string{"fail", "pass"},
		ContinueOnError: true,
	})
	r.Register(&Action{ID: "fail", Run: &RunSpec{Command: "exit 1"}})
	r.Register(&Action{ID: "pass", Run: &RunSpec{Command: "echo ok"}})

	e := NewExecutor(r)
	result, err := e.Execute(context.Background(), "all")
	if err != nil {
		t.Fatal(err)
	}
	if !result.Success {
		t.Fatal("continue_on_error should result in success")
	}
}
