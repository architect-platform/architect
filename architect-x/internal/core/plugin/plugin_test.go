package plugin

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/architect-platform/architect/internal/core/action"
)

func TestLoadFromDir(t *testing.T) {
	dir := t.TempDir()
	content := `
id: test-plugin
version: "1.0.0"
description: "A test plugin"

actions:
  test-action:
    description: "Test action"
    attach_to: build
    run:
      command: "echo test"

rules:
  test-rule:
    description: "Test rule"
    severity: warn
    trigger: [always]
    check:
      command: "true"
`
	os.WriteFile(filepath.Join(dir, "plugin.yml"), []byte(content), 0644)

	loader := NewLoader()
	p, err := loader.LoadFromDir(dir)
	if err != nil {
		t.Fatal(err)
	}

	if p.ID != "test-plugin" {
		t.Errorf("expected ID 'test-plugin', got %q", p.ID)
	}
	if len(p.Actions) != 1 {
		t.Errorf("expected 1 action, got %d", len(p.Actions))
	}
	if len(p.Rules) != 1 {
		t.Errorf("expected 1 rule, got %d", len(p.Rules))
	}
}

func TestRegistryApplyToActionRegistry(t *testing.T) {
	pr := NewRegistry()
	pr.Register(&Plugin{
		ID: "p1",
		Actions: map[string]*action.Action{
			"a1": {ID: "a1", Run: &action.RunSpec{Command: "echo a1"}},
		},
	})

	ar := action.NewRegistry()
	if err := pr.ApplyToActionRegistry(ar); err != nil {
		t.Fatal(err)
	}

	a, err := ar.Get("a1")
	if err != nil {
		t.Fatal(err)
	}
	if a.Plugin != "p1" {
		t.Errorf("expected plugin 'p1', got %q", a.Plugin)
	}
}

func TestResolveLocalPath(t *testing.T) {
	dir := t.TempDir()
	pluginDir := filepath.Join(dir, "my-plugin")
	os.MkdirAll(pluginDir, 0755)
	os.WriteFile(filepath.Join(pluginDir, "plugin.yml"), []byte("id: my-plugin"), 0644)

	loader := NewLoader()
	resolved, err := loader.Resolve(pluginDir)
	if err != nil {
		t.Fatal(err)
	}
	if resolved != pluginDir {
		t.Errorf("expected %q, got %q", pluginDir, resolved)
	}
}
