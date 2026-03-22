package architectplugin

import (
	"bytes"
	"context"
	"encoding/json"
	"testing"
)

type stubPlugin struct{}

func (stubPlugin) Init(_ context.Context, config map[string]any) (InitResult, error) {
	if config["plugin"] != true {
		return InitResult{}, nil
	}
	return InitResult{OK: true, Name: "example", Version: "1.0.0"}, nil
}

func (stubPlugin) ListTasks(_ context.Context) ([]TaskDescriptor, error) {
	return []TaskDescriptor{{ID: "build", Description: "Build"}}, nil
}

func (stubPlugin) ExecuteTask(_ context.Context, _ ExecuteTaskParams, writer TaskEventWriter) (int, error) {
	if err := writer.Output("starting"); err != nil {
		return 1, err
	}
	if err := writer.Progress(0.5); err != nil {
		return 1, err
	}
	return 0, nil
}

func TestServerHandleInitAndListTasks(t *testing.T) {
	var output bytes.Buffer
	server := NewServer(stubPlugin{}).WithIO(nil, &output, func(int) {})

	if err := server.HandleRequest(context.Background(), Request{
		JSONRPC: JSONRPCVersion,
		ID:      1,
		Method:  MethodInit,
		Params: InitParams{
			Config:          map[string]any{"plugin": true},
			ProtocolVersion: APPVersion,
		},
	}); err != nil {
		t.Fatalf("handle init: %v", err)
	}

	if err := server.HandleRequest(context.Background(), Request{
		JSONRPC: JSONRPCVersion,
		ID:      2,
		Method:  MethodListTasks,
	}); err != nil {
		t.Fatalf("handle listTasks: %v", err)
	}

	lines := bytes.Split(bytes.TrimSpace(output.Bytes()), []byte("\n"))
	if len(lines) != 2 {
		t.Fatalf("expected 2 responses, got %d", len(lines))
	}

	var initResponse Response
	if err := json.Unmarshal(lines[0], &initResponse); err != nil {
		t.Fatalf("decode init response: %v", err)
	}
	resultMap, ok := initResponse.Result.(map[string]any)
	if !ok {
		t.Fatalf("expected init result map, got %T", initResponse.Result)
	}
	if resultMap["name"] != "example" {
		t.Fatalf("expected plugin name example, got %v", resultMap["name"])
	}

	var taskResponse Response
	if err := json.Unmarshal(lines[1], &taskResponse); err != nil {
		t.Fatalf("decode listTasks response: %v", err)
	}
	tasks, ok := taskResponse.Result.([]any)
	if !ok || len(tasks) != 1 {
		t.Fatalf("expected one task, got %#v", taskResponse.Result)
	}
}

func TestServerHandleExecuteTask(t *testing.T) {
	var output bytes.Buffer
	server := NewServer(stubPlugin{}).WithIO(nil, &output, func(int) {})

	if err := server.HandleRequest(context.Background(), Request{
		JSONRPC: JSONRPCVersion,
		ID:      3,
		Method:  MethodExecuteTask,
		Params: ExecuteTaskParams{
			ID:   "build",
			Args: []string{"--scan"},
			Env:  map[string]string{"ARCHITECT_PROFILE": "default"},
		},
	}); err != nil {
		t.Fatalf("handle executeTask: %v", err)
	}

	lines := bytes.Split(bytes.TrimSpace(output.Bytes()), []byte("\n"))
	if len(lines) != 4 {
		t.Fatalf("expected 4 lines, got %d", len(lines))
	}

	var ack Response
	if err := json.Unmarshal(lines[0], &ack); err != nil {
		t.Fatalf("decode ack: %v", err)
	}
	if ack.ID == nil || *ack.ID != 3 {
		t.Fatalf("expected ack id 3, got %#v", ack.ID)
	}

	var outputEvent TaskEvent
	if err := json.Unmarshal(lines[1], &outputEvent); err != nil {
		t.Fatalf("decode output event: %v", err)
	}
	if outputEvent.Type != EventOutput || outputEvent.Data != "starting" {
		t.Fatalf("unexpected output event: %#v", outputEvent)
	}

	var progressEvent TaskEvent
	if err := json.Unmarshal(lines[2], &progressEvent); err != nil {
		t.Fatalf("decode progress event: %v", err)
	}
	if progressEvent.Type != EventProgress || progressEvent.Progress == nil || *progressEvent.Progress != 0.5 {
		t.Fatalf("unexpected progress event: %#v", progressEvent)
	}

	var completedEvent TaskEvent
	if err := json.Unmarshal(lines[3], &completedEvent); err != nil {
		t.Fatalf("decode completed event: %v", err)
	}
	if completedEvent.Type != EventCompleted || completedEvent.ExitCode == nil || *completedEvent.ExitCode != 0 {
		t.Fatalf("unexpected completed event: %#v", completedEvent)
	}
}
