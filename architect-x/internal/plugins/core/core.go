package core

import (
	"github.com/architect-platform/architect/internal/core/action"
	"github.com/architect-platform/architect/internal/core/plugin"
)

// DefaultWorkflowStages defines the standard workflow graph.
var DefaultWorkflowStages = []string{"init", "lint", "verify", "build", "test", "release", "publish"}

// NewCorePlugin creates the built-in core plugin that provides the default workflow graph.
func NewCorePlugin() *plugin.Plugin {
	actions := make(map[string]*action.Action)

	// Create the default workflow stages as empty container actions
	for i, stage := range DefaultWorkflowStages {
		a := &action.Action{
			ID:          stage,
			Description: defaultDescription(stage),
			Steps:       []string{},
		}
		// Each stage depends on the previous one
		if i > 0 {
			a.DependsOn = []string{DefaultWorkflowStages[i-1]}
		}
		actions[stage] = a
	}

	// CI pipeline: runs all stages through test
	actions["ci"] = &action.Action{
		ID:          "ci",
		Description: "Run full CI pipeline",
		Steps:       []string{"init", "lint", "verify", "build", "test"},
	}

	// CD pipeline: runs everything
	actions["cd"] = &action.Action{
		ID:          "cd",
		Description: "Run full CD pipeline",
		Steps:       DefaultWorkflowStages,
	}

	return &plugin.Plugin{
		ID:          "core",
		Version:     "1.0.0",
		Description: "Built-in core plugin providing default workflow stages",
		Actions:     actions,
	}
}

func defaultDescription(stage string) string {
	descriptions := map[string]string{
		"init":    "Initialize project dependencies",
		"lint":    "Run linters and formatters",
		"verify":  "Verify project structure and conventions",
		"build":   "Build the project",
		"test":    "Run tests",
		"release": "Prepare release artifacts",
		"publish": "Publish artifacts",
	}
	if desc, ok := descriptions[stage]; ok {
		return desc
	}
	return stage
}
