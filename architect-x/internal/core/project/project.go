package project

import (
	"github.com/architect-platform/architect/internal/core/action"
	"github.com/architect-platform/architect/internal/core/config"
	"github.com/architect-platform/architect/internal/core/plugin"
	corePlugin "github.com/architect-platform/architect/internal/plugins/core"
)

// Project represents a loaded, ready-to-execute project.
type Project struct {
	Dir            string
	Config         *config.Config
	Registry       *action.Registry
	PluginRegistry *plugin.Registry
}

// Load reads config and builds the action registry for a project directory.
func Load(dir string, envName string, overrides map[string]string) (*Project, error) {
	loader := config.NewLoader(dir)
	loader.EnvName = envName
	if overrides != nil {
		loader.Overrides = overrides
	}

	cfg, err := loader.Load()
	if err != nil {
		return nil, err
	}

	registry := action.NewRegistry()
	pluginRegistry := plugin.NewRegistry()

	// Register the core plugin (default workflow stages)
	core := corePlugin.NewCorePlugin()
	if err := pluginRegistry.Register(core); err != nil {
		return nil, err
	}

	// Load external plugins
	pluginLoader := plugin.NewLoader()
	for _, ref := range cfg.Plugins {
		pluginDir, err := pluginLoader.Resolve(ref.ID)
		if err != nil {
			// Skip plugins that can't be found (they may be registry plugins for later phases)
			continue
		}
		p, err := pluginLoader.LoadFromDir(pluginDir)
		if err != nil {
			return nil, err
		}
		if err := pluginRegistry.Register(p); err != nil {
			return nil, err
		}
	}

	// Apply plugin actions to registry
	if err := pluginRegistry.ApplyToActionRegistry(registry); err != nil {
		return nil, err
	}

	// Register project-level actions (override plugin actions)
	for _, a := range cfg.Actions {
		existing, err := registry.Get(a.ID)
		if err == nil {
			// Merge: project config overrides plugin defaults
			if a.Run != nil {
				existing.Run = a.Run
			}
			if a.Description != "" {
				existing.Description = a.Description
			}
			if len(a.Steps) > 0 {
				existing.Steps = a.Steps
			}
			if len(a.DependsOn) > 0 {
				existing.DependsOn = a.DependsOn
			}
			if len(a.Env) > 0 {
				existing.Env = a.Env
			}
		} else {
			if err := registry.Register(a); err != nil {
				return nil, err
			}
		}
	}

	// Merge plugin rules into config
	pluginRules := pluginRegistry.CollectRules()
	if cfg.Rules == nil {
		cfg.Rules = pluginRules
	} else {
		for id, r := range pluginRules {
			if _, exists := cfg.Rules[id]; !exists {
				cfg.Rules[id] = r
			}
		}
	}

	// Resolve attach_to relationships
	if err := registry.ResolveAttachments(); err != nil {
		return nil, err
	}

	return &Project{
		Dir:            dir,
		Config:         cfg,
		Registry:       registry,
		PluginRegistry: pluginRegistry,
	}, nil
}
