package config

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/architect-platform/architect/internal/core/action"
	"github.com/architect-platform/architect/internal/core/rule"
	"gopkg.in/yaml.v3"
)

const (
	DefaultConfigFile = "architect.yml"
	FragmentDir       = ".architect"
	GlobalConfigDir   = ".config/architect"
	GlobalConfigFile  = "config.yml"
)

// Loader handles loading and merging configuration from multiple sources.
type Loader struct {
	ProjectDir string
	EnvName    string
	Overrides  map[string]string // --set key=value
}

// NewLoader creates a config loader for the given project directory.
func NewLoader(projectDir string) *Loader {
	return &Loader{
		ProjectDir: projectDir,
		Overrides:  make(map[string]string),
	}
}

// Load reads and merges configuration following the cascade:
// 1. Built-in defaults
// 2. Global config (~/.config/architect/config.yml)
// 3. Project config (architect.yml)
// 4. Fragment configs (.architect/*.yml)
// 5. Environment overrides
// 6. ARCHITECT_* env vars
// 7. --set CLI flags
func (l *Loader) Load() (*Config, error) {
	cfg := &Config{}

	// 1. Built-in defaults (minimal)
	cfg.Project.Type = "application"

	// 2. Global config
	home, err := os.UserHomeDir()
	if err == nil {
		globalPath := filepath.Join(home, GlobalConfigDir, GlobalConfigFile)
		if _, err := os.Stat(globalPath); err == nil {
			globalCfg, err := loadFile(globalPath)
			if err != nil {
				return nil, fmt.Errorf("loading global config: %w", err)
			}
			mergeCfg(cfg, globalCfg)
		}
	}

	// 3. Project config
	projectPath := filepath.Join(l.ProjectDir, DefaultConfigFile)
	if _, err := os.Stat(projectPath); err == nil {
		projectCfg, err := loadFile(projectPath)
		if err != nil {
			return nil, fmt.Errorf("loading project config %s: %w", projectPath, err)
		}
		mergeCfg(cfg, projectCfg)
	} else if !os.IsNotExist(err) {
		return nil, fmt.Errorf("checking project config: %w", err)
	}

	// 4. Fragment configs
	fragmentDir := filepath.Join(l.ProjectDir, FragmentDir)
	if entries, err := os.ReadDir(fragmentDir); err == nil {
		for _, entry := range entries {
			if entry.IsDir() || !isYAML(entry.Name()) {
				continue
			}
			fragCfg, err := loadFile(filepath.Join(fragmentDir, entry.Name()))
			if err != nil {
				return nil, fmt.Errorf("loading fragment %s: %w", entry.Name(), err)
			}
			mergeCfg(cfg, fragCfg)
		}
	}

	// 5. Environment overrides
	if l.EnvName != "" {
		if envOverride, ok := cfg.Environments[l.EnvName]; ok {
			applyEnvOverride(cfg, &envOverride)
		}
	}

	// 6. ARCHITECT_* env vars
	applyEnvVars(cfg)

	// 7. --set CLI flags
	applyOverrides(cfg, l.Overrides)

	// Assign IDs to actions from map keys
	for id, a := range cfg.Actions {
		if a.ID == "" {
			a.ID = id
		}
	}
	for id, r := range cfg.Rules {
		if r.ID == "" {
			r.ID = id
		}
	}

	return cfg, nil
}

// FindProjectDir walks up from the current directory to find architect.yml.
func FindProjectDir() (string, error) {
	dir, err := os.Getwd()
	if err != nil {
		return "", err
	}

	for {
		if _, err := os.Stat(filepath.Join(dir, DefaultConfigFile)); err == nil {
			return dir, nil
		}
		parent := filepath.Dir(dir)
		if parent == dir {
			return "", fmt.Errorf("no %s found (searched up to root)", DefaultConfigFile)
		}
		dir = parent
	}
}

func loadFile(path string) (*Config, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	var cfg Config
	if err := yaml.Unmarshal(data, &cfg); err != nil {
		return nil, fmt.Errorf("parsing %s: %w", path, err)
	}
	return &cfg, nil
}

func mergeCfg(base, overlay *Config) {
	if overlay.Project.Name != "" {
		base.Project.Name = overlay.Project.Name
	}
	if overlay.Project.Type != "" {
		base.Project.Type = overlay.Project.Type
	}
	if overlay.Project.Version != "" {
		base.Project.Version = overlay.Project.Version
	}
	if overlay.Project.Description != "" {
		base.Project.Description = overlay.Project.Description
	}
	if len(overlay.Project.Dependencies) > 0 {
		base.Project.Dependencies = overlay.Project.Dependencies
	}

	if len(overlay.Plugins) > 0 {
		base.Plugins = append(base.Plugins, overlay.Plugins...)
	}

	if overlay.Actions != nil {
		if base.Actions == nil {
			base.Actions = make(map[string]*action.Action)
		}
		for k, v := range overlay.Actions {
			base.Actions[k] = v
		}
	}

	if overlay.Rules != nil {
		if base.Rules == nil {
			base.Rules = make(map[string]*rule.Rule)
		}
		for k, v := range overlay.Rules {
			base.Rules[k] = v
		}
	}

	if overlay.Hooks != nil {
		if base.Hooks == nil {
			base.Hooks = make(map[string][]string)
		}
		for k, v := range overlay.Hooks {
			base.Hooks[k] = v
		}
	}

	if overlay.Environments != nil {
		if base.Environments == nil {
			base.Environments = make(map[string]EnvOverride)
		}
		for k, v := range overlay.Environments {
			base.Environments[k] = v
		}
	}

	if overlay.Workspace != nil {
		base.Workspace = overlay.Workspace
	}
	if overlay.Cloud != nil {
		base.Cloud = overlay.Cloud
	}

	if overlay.Middleware != nil {
		if base.Middleware == nil {
			base.Middleware = make(map[string]*Middleware)
		}
		for k, v := range overlay.Middleware {
			base.Middleware[k] = v
		}
	}
}

func applyEnvOverride(cfg *Config, env *EnvOverride) {
	if env.Actions != nil {
		if cfg.Actions == nil {
			cfg.Actions = make(map[string]*action.Action)
		}
		for k, v := range env.Actions {
			cfg.Actions[k] = v
		}
	}
}

func applyEnvVars(cfg *Config) {
	for _, env := range os.Environ() {
		if !strings.HasPrefix(env, "ARCHITECT_") {
			continue
		}
		parts := strings.SplitN(env, "=", 2)
		if len(parts) != 2 {
			continue
		}
		key := strings.ToLower(strings.TrimPrefix(parts[0], "ARCHITECT_"))
		key = strings.ReplaceAll(key, "_", ".")
		_ = key // TODO: apply nested key to config
	}
}

func applyOverrides(cfg *Config, overrides map[string]string) {
	// TODO: apply --set key=value overrides to config tree
	_ = cfg
	_ = overrides
}

func isYAML(name string) bool {
	return strings.HasSuffix(name, ".yml") || strings.HasSuffix(name, ".yaml")
}
