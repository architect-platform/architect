package action

import (
	"fmt"
	"sync"
)

// Registry holds all registered actions and supports lookup.
type Registry struct {
	mu      sync.RWMutex
	actions map[string]*Action
}

// NewRegistry creates a new action registry.
func NewRegistry() *Registry {
	return &Registry{
		actions: make(map[string]*Action),
	}
}

// Register adds an action to the registry.
func (r *Registry) Register(a *Action) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	if _, exists := r.actions[a.ID]; exists {
		return fmt.Errorf("action %q already registered", a.ID)
	}
	r.actions[a.ID] = a
	return nil
}

// Get returns an action by ID.
func (r *Registry) Get(id string) (*Action, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	a, ok := r.actions[id]
	if !ok {
		return nil, fmt.Errorf("action %q not found", id)
	}
	return a, nil
}

// All returns all registered actions.
func (r *Registry) All() []*Action {
	r.mu.RLock()
	defer r.mu.RUnlock()

	result := make([]*Action, 0, len(r.actions))
	for _, a := range r.actions {
		result = append(result, a)
	}
	return result
}

// IDs returns all registered action IDs.
func (r *Registry) IDs() []string {
	r.mu.RLock()
	defer r.mu.RUnlock()

	ids := make([]string, 0, len(r.actions))
	for id := range r.actions {
		ids = append(ids, id)
	}
	return ids
}

// ResolveAttachments processes attach_to fields, adding actions as steps to their targets.
func (r *Registry) ResolveAttachments() error {
	r.mu.Lock()
	defer r.mu.Unlock()

	for _, a := range r.actions {
		if a.AttachTo == "" {
			continue
		}
		target, ok := r.actions[a.AttachTo]
		if !ok {
			return fmt.Errorf("action %q attaches to %q, but %q does not exist", a.ID, a.AttachTo, a.AttachTo)
		}
		// Add as a step of the target action
		target.Steps = append(target.Steps, a.ID)
	}
	return nil
}
