package events

import "time"

// Type identifies the kind of event.
type Type string

const (
	ActionStarted   Type = "action.started"
	ActionCompleted Type = "action.completed"
	ActionFailed    Type = "action.failed"
	ActionSkipped   Type = "action.skipped"
	RuleChecked     Type = "rule.checked"
	RuleFailed      Type = "rule.failed"
	ProjectLoaded   Type = "project.loaded"
)

// Event represents something that happened during execution.
type Event struct {
	Type      Type                   `json:"type"`
	Timestamp time.Time              `json:"timestamp"`
	ActionID  string                 `json:"action_id,omitempty"`
	RuleID    string                 `json:"rule_id,omitempty"`
	Success   bool                   `json:"success,omitempty"`
	Duration  time.Duration          `json:"duration,omitempty"`
	Message   string                 `json:"message,omitempty"`
	Data      map[string]interface{} `json:"data,omitempty"`
}

// Handler processes events.
type Handler func(Event)

// Bus distributes events to handlers.
type Bus struct {
	handlers []Handler
}

// NewBus creates an event bus.
func NewBus() *Bus {
	return &Bus{}
}

// Subscribe registers a handler for all events.
func (b *Bus) Subscribe(h Handler) {
	b.handlers = append(b.handlers, h)
}

// Emit sends an event to all handlers.
func (b *Bus) Emit(e Event) {
	if e.Timestamp.IsZero() {
		e.Timestamp = time.Now()
	}
	for _, h := range b.handlers {
		h(e)
	}
}
