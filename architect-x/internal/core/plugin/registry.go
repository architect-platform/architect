package plugin

import (
	"fmt"
	"sync"

	"github.com/architect-platform/architect/internal/core/action"
	"github.com/architect-platform/architect/internal/core/rule"
)

// Registry manages loaded plugins.
type Registry struct {
	mu      sync.RWMutex
	plugins map[string]*Plugin
}

// NewRegistry creates a plugin registry.
func NewRegistry() *Registry {
	return &Registry{
		plugins: make(map[string]*Plugin),
	}
}

// Register adds a plugin to the registry.
func (r *Registry) Register(p *Plugin) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	if _, exists := r.plugins[p.ID]; exists {
		return fmt.Errorf("plugin %q already registered", p.ID)
	}
	r.plugins[p.ID] = p
	return nil
}

// Get returns a plugin by ID.
func (r *Registry) Get(id string) (*Plugin, bool) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	p, ok := r.plugins[id]
	return p, ok
}

// All returns all registered plugins.
func (r *Registry) All() []*Plugin {
	r.mu.RLock()
	defer r.mu.RUnlock()
	result := make([]*Plugin, 0, len(r.plugins))
	for _, p := range r.plugins {
		result = append(result, p)
	}
	return result
}

// ApplyToActionRegistry registers all plugin actions into an action registry.
func (r *Registry) ApplyToActionRegistry(ar *action.Registry) error {
	r.mu.RLock()
	defer r.mu.RUnlock()

	for _, p := range r.plugins {
		for _, a := range p.Actions {
			if a.Plugin == "" {
				a.Plugin = p.ID
			}
			if err := ar.Register(a); err != nil {
				return fmt.Errorf("plugin %s: %w", p.ID, err)
			}
		}
	}
	return nil
}

// CollectRules gathers all rules from all plugins.
func (r *Registry) CollectRules() map[string]*rule.Rule {
	r.mu.RLock()
	defer r.mu.RUnlock()

	rules := make(map[string]*rule.Rule)
	for _, p := range r.plugins {
		for id, rl := range p.Rules {
			rules[id] = rl
		}
	}
	return rules
}
