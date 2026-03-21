package rule

import (
	"os"
	"path/filepath"
	"testing"
)

func TestEvaluatePatternExists(t *testing.T) {
	dir := t.TempDir()
	os.WriteFile(filepath.Join(dir, "README.md"), []byte("# readme"), 0644)

	engine := NewEngine(dir, map[string]*Rule{
		"has-readme": {
			ID:       "has-readme",
			Severity: SeverityError,
			Trigger:  []string{"always"},
			Pattern:  &Pattern{Exists: []string{"README.md"}},
		},
	})

	results := engine.CheckAll("always")
	if len(results) != 1 {
		t.Fatalf("expected 1 result, got %d", len(results))
	}
	if !results[0].Passed {
		t.Error("expected pass (README.md exists)")
	}
}

func TestEvaluatePatternMissing(t *testing.T) {
	dir := t.TempDir()

	engine := NewEngine(dir, map[string]*Rule{
		"has-license": {
			ID:       "has-license",
			Severity: SeverityError,
			Trigger:  []string{"always"},
			Pattern:  &Pattern{Exists: []string{"LICENSE"}},
		},
	})

	results := engine.CheckAll("always")
	if len(results) != 1 {
		t.Fatalf("expected 1 result, got %d", len(results))
	}
	if results[0].Passed {
		t.Error("expected fail (LICENSE missing)")
	}
}

func TestEvaluateCommand(t *testing.T) {
	dir := t.TempDir()

	engine := NewEngine(dir, map[string]*Rule{
		"pass": {
			ID:       "pass",
			Severity: SeverityError,
			Trigger:  []string{"always"},
			Check:    &RunSpec{Command: "true"},
		},
		"fail": {
			ID:       "fail",
			Severity: SeverityWarn,
			Trigger:  []string{"always"},
			Check:    &RunSpec{Command: "false"},
		},
	})

	results := engine.CheckAll("always")
	if len(results) != 2 {
		t.Fatalf("expected 2 results, got %d", len(results))
	}

	passed := 0
	failed := 0
	for _, r := range results {
		if r.Passed {
			passed++
		} else {
			failed++
		}
	}
	if passed != 1 || failed != 1 {
		t.Errorf("expected 1 pass, 1 fail, got %d pass, %d fail", passed, failed)
	}
}

func TestTriggerFiltering(t *testing.T) {
	dir := t.TempDir()

	engine := NewEngine(dir, map[string]*Rule{
		"always-rule": {
			ID:      "always-rule",
			Trigger: []string{"always"},
			Check:   &RunSpec{Command: "true"},
		},
		"commit-only": {
			ID:      "commit-only",
			Trigger: []string{"pre-commit"},
			Check:   &RunSpec{Command: "true"},
		},
	})

	results := engine.CheckAll("always")
	if len(results) != 1 {
		t.Errorf("expected 1 result for 'always' trigger, got %d", len(results))
	}

	// pre-commit trigger matches both "commit-only" (explicit) and "always-rule" (always matches)
	results = engine.CheckAll("pre-commit")
	if len(results) != 2 {
		t.Errorf("expected 2 results for 'pre-commit' trigger (includes 'always'), got %d", len(results))
	}
}
