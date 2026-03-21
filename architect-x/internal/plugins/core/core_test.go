package core

import (
	"testing"
)

func TestCorePluginHasDefaultStages(t *testing.T) {
	p := NewCorePlugin()

	if p.ID != "core" {
		t.Errorf("expected ID 'core', got %q", p.ID)
	}

	expectedStages := []string{"init", "lint", "verify", "build", "test", "release", "publish"}
	for _, stage := range expectedStages {
		if _, ok := p.Actions[stage]; !ok {
			t.Errorf("missing expected stage %q", stage)
		}
	}

	// Check dependency chain
	build := p.Actions["build"]
	if len(build.DependsOn) == 0 || build.DependsOn[0] != "verify" {
		t.Errorf("expected build to depend on verify, got %v", build.DependsOn)
	}

	// Check CI action
	ci := p.Actions["ci"]
	if ci == nil {
		t.Fatal("expected ci action")
	}
	if len(ci.Steps) != 5 {
		t.Errorf("expected 5 steps in ci, got %d", len(ci.Steps))
	}
}
