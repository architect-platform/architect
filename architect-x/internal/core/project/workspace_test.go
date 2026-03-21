package project

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/architect-platform/architect/internal/core/config"
)

func TestWorkspaceDiscovery(t *testing.T) {
	root := t.TempDir()

	// Create workspace config
	writeFile(t, root, "architect.yml", `
project:
  name: my-monorepo
  type: workspace
workspace:
  members: ["packages/*"]
`)

	// Create members
	mkMember(t, root, "packages/core", "core", nil)
	mkMember(t, root, "packages/web", "web", []string{"core"})
	mkMember(t, root, "packages/api", "api", []string{"core"})

	loader := config.NewLoader(root)
	cfg, err := loader.Load()
	if err != nil {
		t.Fatal(err)
	}

	ws, err := LoadWorkspace(root, cfg)
	if err != nil {
		t.Fatal(err)
	}

	if len(ws.Members) != 3 {
		t.Fatalf("expected 3 members, got %d", len(ws.Members))
	}
}

func TestWorkspaceTopologicalOrder(t *testing.T) {
	root := t.TempDir()

	writeFile(t, root, "architect.yml", `
project:
  name: mono
  type: workspace
workspace:
  members: ["packages/*"]
`)

	mkMember(t, root, "packages/core", "core", nil)
	mkMember(t, root, "packages/ui", "ui", []string{"core"})
	mkMember(t, root, "packages/app", "app", []string{"core", "ui"})

	loader := config.NewLoader(root)
	cfg, _ := loader.Load()
	ws, _ := LoadWorkspace(root, cfg)

	order, err := ws.TopologicalOrder()
	if err != nil {
		t.Fatal(err)
	}

	// core must come before ui, ui before app
	indexOf := func(name string) int {
		for i, m := range order {
			if m.Name == name {
				return i
			}
		}
		return -1
	}

	if indexOf("core") > indexOf("ui") {
		t.Error("core should come before ui")
	}
	if indexOf("ui") > indexOf("app") {
		t.Error("ui should come before app")
	}
}

func TestWorkspaceFilter(t *testing.T) {
	root := t.TempDir()

	writeFile(t, root, "architect.yml", `
project:
  name: mono
  type: workspace
workspace:
  members: ["packages/*", "apps/*"]
`)

	mkMember(t, root, "packages/core", "core", nil)
	mkMember(t, root, "packages/ui", "ui", nil)
	mkMember(t, root, "apps/web", "web", nil)

	loader := config.NewLoader(root)
	cfg, _ := loader.Load()
	ws, _ := LoadWorkspace(root, cfg)

	filtered := ws.Filter("apps/*")
	if len(filtered) != 1 {
		t.Errorf("expected 1 match for 'apps/*', got %d", len(filtered))
	}
}

func TestWorkspaceExclude(t *testing.T) {
	root := t.TempDir()

	writeFile(t, root, "architect.yml", `
project:
  name: mono
  type: workspace
workspace:
  members: ["packages/*"]
  exclude: ["packages/deprecated-*"]
`)

	mkMember(t, root, "packages/core", "core", nil)
	mkMember(t, root, "packages/deprecated-old", "old", nil)

	loader := config.NewLoader(root)
	cfg, _ := loader.Load()
	ws, _ := LoadWorkspace(root, cfg)

	if len(ws.Members) != 1 {
		t.Errorf("expected 1 member (excluded deprecated), got %d", len(ws.Members))
	}
}

func mkMember(t *testing.T, root, relPath, name string, deps []string) {
	t.Helper()
	dir := filepath.Join(root, relPath)
	os.MkdirAll(dir, 0755)

	depsYAML := ""
	if len(deps) > 0 {
		depsYAML = "\n  dependencies:"
		for _, d := range deps {
			depsYAML += "\n    - " + d
		}
	}

	content := "project:\n  name: " + name + depsYAML + "\n"
	writeFile(t, dir, "architect.yml", content)
}

func writeFile(t *testing.T, dir, name, content string) {
	t.Helper()
	os.WriteFile(filepath.Join(dir, name), []byte(content), 0644)
}
