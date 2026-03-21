package plugin

import (
	"github.com/architect-platform/architect/internal/core/action"
	"github.com/architect-platform/architect/internal/core/rule"
)

// Plugin represents a loaded plugin with its provided actions and rules.
type Plugin struct {
	ID          string                 `yaml:"id"`
	Version     string                 `yaml:"version,omitempty"`
	Description string                 `yaml:"description,omitempty"`
	Config      map[string]interface{} `yaml:"config,omitempty"`
	Actions     map[string]*action.Action `yaml:"actions,omitempty"`
	Rules       map[string]*rule.Rule     `yaml:"rules,omitempty"`
}
