package config

import (
	"github.com/architect-platform/architect/internal/core/action"
	"github.com/architect-platform/architect/internal/core/rule"
)

// Config represents the fully resolved project configuration.
type Config struct {
	Project      ProjectConfig            `yaml:"project"`
	Plugins      []PluginRef              `yaml:"plugins,omitempty"`
	Actions      map[string]*action.Action `yaml:"actions,omitempty"`
	Rules        map[string]*rule.Rule    `yaml:"rules,omitempty"`
	Hooks        map[string][]string      `yaml:"hooks,omitempty"`
	Environments map[string]EnvOverride   `yaml:"environments,omitempty"`
	Workspace    *WorkspaceConfig         `yaml:"workspace,omitempty"`
	Cloud        *CloudConfig             `yaml:"cloud,omitempty"`
	Middleware   map[string]*Middleware   `yaml:"middleware,omitempty"`
}

// ProjectConfig holds project metadata.
type ProjectConfig struct {
	Name         string   `yaml:"name"`
	Type         string   `yaml:"type,omitempty"` // application, library, workspace
	Version      string   `yaml:"version,omitempty"`
	Description  string   `yaml:"description,omitempty"`
	Dependencies []string `yaml:"dependencies,omitempty"`
}

// PluginRef identifies a plugin to load.
type PluginRef struct {
	ID      string                 `yaml:"id"`
	Config  map[string]interface{} `yaml:"config,omitempty"`
	Enabled *bool                  `yaml:"enabled,omitempty"`
}

// EnvOverride holds environment-specific config overrides.
type EnvOverride struct {
	Actions map[string]*action.Action `yaml:"actions,omitempty"`
	Env     map[string]string         `yaml:"env,omitempty"`
}

// WorkspaceConfig defines workspace/monorepo settings.
type WorkspaceConfig struct {
	Members []string `yaml:"members"`
	Exclude []string `yaml:"exclude,omitempty"`
}

// CloudConfig defines optional cloud reporting.
type CloudConfig struct {
	Enabled bool   `yaml:"enabled"`
	Org     string `yaml:"org,omitempty"`
}

// Middleware defines a middleware hook.
type Middleware struct {
	Before    []string `yaml:"before,omitempty"`
	After     []string `yaml:"after,omitempty"`
	Condition string   `yaml:"condition,omitempty"`
	Run       *action.RunSpec `yaml:"run,omitempty"`
}
