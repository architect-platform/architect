package events

import "testing"

func TestBusEmitAndSubscribe(t *testing.T) {
	bus := NewBus()
	var received []Event

	bus.Subscribe(func(e Event) {
		received = append(received, e)
	})

	bus.Emit(Event{Type: ActionStarted, ActionID: "build"})
	bus.Emit(Event{Type: ActionCompleted, ActionID: "build", Success: true})

	if len(received) != 2 {
		t.Fatalf("expected 2 events, got %d", len(received))
	}
	if received[0].Type != ActionStarted {
		t.Errorf("expected ActionStarted, got %v", received[0].Type)
	}
	if received[1].Type != ActionCompleted {
		t.Errorf("expected ActionCompleted, got %v", received[1].Type)
	}
	if received[0].Timestamp.IsZero() {
		t.Error("expected non-zero timestamp")
	}
}
