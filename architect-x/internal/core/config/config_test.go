package config

import (
	"os"
	"path/filepath"
	"testing"
)

func TestLoaderBasic(t *testing.T) {
	dir := t.TempDir()
	writeYAML(t, dir, "architect.yml", `
project:
  name: test-project
  type: application

actions:
  build:
    description: "Build it"
    run:
      command: "echo building"
`)

	loader := NewLoader(dir)
	cfg, err := loader.Load()
	if err != nil {
		t.Fatal(err)
	}

	if cfg.Project.Name != "test-project" {
		t.Errorf("expected name 'test-project', got %q", cfg.Project.Name)
	}
	if len(cfg.Actions) != 1 {
		t.Errorf("expected 1 action, got %d", len(cfg.Actions))
	}
}

func TestLoaderFragments(t *testing.T) {
	dir := t.TempDir()
	writeYAML(t, dir, "architect.yml", `
project:
  name: frag-test
`)
	fragDir := filepath.Join(dir, ".architect")
	os.MkdirAll(fragDir, 0755)
	writeYAML(t, fragDir, "extra.yml", `
actions:
  lint:
    run:
      command: "echo lint"
`)

	loader := NewLoader(dir)
	cfg, err := loader.Load()
	if err != nil {
		t.Fatal(err)
	}

	if len(cfg.Actions) != 1 {
		t.Errorf("expected 1 action from fragment, got %d", len(cfg.Actions))
	}
}

func TestLoaderEnvironmentOverride(t *testing.T) {
	dir := t.TempDir()
	writeYAML(t, dir, "architect.yml", `
project:
  name: env-test

actions:
  deploy:
    run:
      command: "echo deploy-default"

environments:
  prod:
    actions:
      deploy:
        run:
          command: "echo deploy-prod"
`)

	loader := NewLoader(dir)
	loader.EnvName = "prod"
	cfg, err := loader.Load()
	if err != nil {
		t.Fatal(err)
	}

	deploy := cfg.Actions["deploy"]
	if deploy == nil || deploy.Run == nil {
		t.Fatal("expected deploy action")
	}
	if deploy.Run.Command != "echo deploy-prod" {
		t.Errorf("expected prod override, got %q", deploy.Run.Command)
	}
}

func TestFindProjectDir(t *testing.T) {
	dir := t.TempDir()
	// Resolve symlinks (macOS /var -> /private/var)
	dir, _ = filepath.EvalSymlinks(dir)

	writeYAML(t, dir, "architect.yml", `project: { name: find-test }`)

	sub := filepath.Join(dir, "a", "b", "c")
	os.MkdirAll(sub, 0755)

	// Change to subdirectory
	orig, _ := os.Getwd()
	defer os.Chdir(orig)
	os.Chdir(sub)

	found, err := FindProjectDir()
	if err != nil {
		t.Fatal(err)
	}
	if found != dir {
		t.Errorf("expected %q, got %q", dir, found)
	}
}

func TestInterpolator(t *testing.T) {
	cfg := &Config{
		Project: ProjectConfig{
			Name:    "my-app",
			Version: "1.0.0",
		},
	}
	interp := NewInterpolator(cfg)

	tests := []struct {
		input    string
		expected string
	}{
		{"Hello {{ project.name }}", "Hello my-app"},
		{"v{{ project.version }}", "v1.0.0"},
		{"no vars here", "no vars here"},
		{"{{ unknown.key }}", "{{ unknown.key }}"},
	}

	for _, tt := range tests {
		got := interp.Interpolate(tt.input)
		if got != tt.expected {
			t.Errorf("Interpolate(%q) = %q, want %q", tt.input, got, tt.expected)
		}
	}
}

func TestValidatorKnownFields(t *testing.T) {
	dir := t.TempDir()
	writeYAML(t, dir, "architect.yml", `
project:
  name: valid
actinos:
  build:
    run:
      command: "echo hi"
`)

	validator := NewValidator()
	errors, err := validator.ValidateFile(filepath.Join(dir, "architect.yml"))
	if err != nil {
		t.Fatal(err)
	}

	if len(errors) == 0 {
		t.Fatal("expected validation error for typo 'actinos'")
	}

	found := false
	for _, e := range errors {
		if e.Suggest == "actions" {
			found = true
		}
	}
	if !found {
		t.Error("expected suggestion 'actions' for typo 'actinos'")
	}
}

func writeYAML(t *testing.T, dir, name, content string) {
	t.Helper()
	path := filepath.Join(dir, name)
	if err := os.WriteFile(path, []byte(content), 0644); err != nil {
		t.Fatal(err)
	}
}
