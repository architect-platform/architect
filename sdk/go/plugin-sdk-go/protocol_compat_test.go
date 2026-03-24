package architectplugin

import (
	"bytes"
	"context"
	"encoding/json"
	"os"
	"path/filepath"
	"runtime"
	"testing"
)

type fixturePlugin struct{}

func (fixturePlugin) Init(_ context.Context, _ map[string]any) (InitResult, error) {
	return InitResult{OK: true, Name: "test-plugin", Version: "1.0.0"}, nil
}

func (fixturePlugin) ListTasks(_ context.Context) ([]TaskDescriptor, error) {
	return []TaskDescriptor{
		{
			ID:                   "build",
			Description:          "Build the project",
			Phase:                "build",
			Dependencies:         []string{"init"},
			Permissions:          []string{"process:exec"},
			RequiresConfirmation: false,
		},
		{
			ID:                   "deploy",
			Description:          "Deploy to production",
			Phase:                "release",
			Dependencies:         []string{"build"},
			Permissions:          []string{"process:exec", "network:outbound"},
			RequiresConfirmation: true,
		},
	}, nil
}

func (fixturePlugin) ExecuteTask(_ context.Context, _ ExecuteTaskParams, w TaskEventWriter) (int, error) {
	if err := w.Output("Compiling sources..."); err != nil {
		return 1, err
	}
	if err := w.Progress(0.5); err != nil {
		return 1, err
	}
	return 0, nil
}

func loadFixtures(t *testing.T) map[string]any {
	t.Helper()
	_, thisFile, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("cannot determine test file path")
	}
	fixturesPath := filepath.Join(filepath.Dir(thisFile), "..", "..", "..", "docs", "protocol-fixtures.json")
	data, err := os.ReadFile(fixturesPath)
	if err != nil {
		t.Fatalf("read fixtures: %v", err)
	}
	var fixtures map[string]any
	if err := json.Unmarshal(data, &fixtures); err != nil {
		t.Fatalf("parse fixtures: %v", err)
	}
	return fixtures
}

func TestProtocolConstantsMatchFixtures(t *testing.T) {
	fixtures := loadFixtures(t)
	if JSONRPCVersion != fixtures["json_rpc_version"] {
		t.Errorf("JSONRPCVersion = %q, want %q", JSONRPCVersion, fixtures["json_rpc_version"])
	}
	if APPVersion != fixtures["protocol_version"] {
		t.Errorf("APPVersion = %q, want %q", APPVersion, fixtures["protocol_version"])
	}
	if MethodInit != "init" {
		t.Errorf("MethodInit = %q, want init", MethodInit)
	}
	if MethodListTasks != "listTasks" {
		t.Errorf("MethodListTasks = %q, want listTasks", MethodListTasks)
	}
	if MethodExecuteTask != "executeTask" {
		t.Errorf("MethodExecuteTask = %q, want executeTask", MethodExecuteTask)
	}
	if MethodShutdown != "shutdown" {
		t.Errorf("MethodShutdown = %q, want shutdown", MethodShutdown)
	}
	if EventOutput != "output" {
		t.Errorf("EventOutput = %q, want output", EventOutput)
	}
	if EventCompleted != "completed" {
		t.Errorf("EventCompleted = %q, want completed", EventCompleted)
	}
	if ErrorInvalid != -32600 {
		t.Errorf("ErrorInvalid = %d, want -32600", ErrorInvalid)
	}
}

func TestServerProcessesFixtureInitAndListTasks(t *testing.T) {
	fixtures := loadFixtures(t)
	requests := fixtures["requests"].(map[string]any)

	var output bytes.Buffer
	server := NewServer(fixturePlugin{}).WithIO(nil, &output, func(int) {})

	initReq := requests["init"].(map[string]any)
	if err := server.HandleRequest(context.Background(), Request{
		JSONRPC: initReq["jsonrpc"].(string),
		ID:      int(initReq["id"].(float64)),
		Method:  initReq["method"].(string),
		Params: InitParams{
			Config:          initReq["params"].(map[string]any)["config"].(map[string]any),
			ProtocolVersion: initReq["params"].(map[string]any)["protocol_version"].(string),
		},
	}); err != nil {
		t.Fatalf("handle init: %v", err)
	}

	listReq := requests["listTasks"].(map[string]any)
	if err := server.HandleRequest(context.Background(), Request{
		JSONRPC: listReq["jsonrpc"].(string),
		ID:      int(listReq["id"].(float64)),
		Method:  listReq["method"].(string),
	}); err != nil {
		t.Fatalf("handle listTasks: %v", err)
	}

	lines := bytes.Split(bytes.TrimSpace(output.Bytes()), []byte("\n"))
	if len(lines) != 2 {
		t.Fatalf("expected 2 responses, got %d", len(lines))
	}

	var initResp Response
	if err := json.Unmarshal(lines[0], &initResp); err != nil {
		t.Fatalf("decode init: %v", err)
	}
	result := initResp.Result.(map[string]any)
	if result["ok"] != true || result["name"] != "test-plugin" {
		t.Errorf("init result = %v, want ok=true name=test-plugin", result)
	}

	var listResp Response
	if err := json.Unmarshal(lines[1], &listResp); err != nil {
		t.Fatalf("decode listTasks: %v", err)
	}
	tasks := listResp.Result.([]any)
	if len(tasks) != 2 {
		t.Fatalf("expected 2 tasks, got %d", len(tasks))
	}
	first := tasks[0].(map[string]any)
	if first["id"] != "build" {
		t.Errorf("first task id = %v, want build", first["id"])
	}
	second := tasks[1].(map[string]any)
	if second["requires_confirmation"] != true {
		t.Errorf("second task requires_confirmation = %v, want true", second["requires_confirmation"])
	}
}

func TestServerStreamsFixtureCompatibleEvents(t *testing.T) {
	fixtures := loadFixtures(t)
	requests := fixtures["requests"].(map[string]any)
	events := fixtures["events"].(map[string]any)

	var output bytes.Buffer
	server := NewServer(fixturePlugin{}).WithIO(nil, &output, func(int) {})

	execReq := requests["executeTask"].(map[string]any)
	params := execReq["params"].(map[string]any)
	args := make([]string, 0)
	for _, a := range params["args"].([]any) {
		args = append(args, a.(string))
	}
	envRaw := params["env"].(map[string]any)
	env := make(map[string]string)
	for k, v := range envRaw {
		env[k] = v.(string)
	}

	if err := server.HandleRequest(context.Background(), Request{
		JSONRPC: execReq["jsonrpc"].(string),
		ID:      int(execReq["id"].(float64)),
		Method:  execReq["method"].(string),
		Params:  ExecuteTaskParams{ID: params["id"].(string), Args: args, Env: env},
	}); err != nil {
		t.Fatalf("handle executeTask: %v", err)
	}

	lines := bytes.Split(bytes.TrimSpace(output.Bytes()), []byte("\n"))
	if len(lines) != 4 {
		t.Fatalf("expected 4 lines (ack + output + progress + completed), got %d", len(lines))
	}

	// Ack
	var ack Response
	if err := json.Unmarshal(lines[0], &ack); err != nil {
		t.Fatalf("decode ack: %v", err)
	}
	if ack.ID == nil || *ack.ID != 3 {
		t.Errorf("ack id = %v, want 3", ack.ID)
	}

	// Output event
	var outputEvt TaskEvent
	if err := json.Unmarshal(lines[1], &outputEvt); err != nil {
		t.Fatalf("decode output: %v", err)
	}
	wantOutput := events["output"].(map[string]any)
	if outputEvt.Type != wantOutput["type"] || outputEvt.Data != wantOutput["data"] {
		t.Errorf("output event = %+v, want type=%v data=%v", outputEvt, wantOutput["type"], wantOutput["data"])
	}

	// Progress event
	var progressEvt TaskEvent
	if err := json.Unmarshal(lines[2], &progressEvt); err != nil {
		t.Fatalf("decode progress: %v", err)
	}
	if progressEvt.Type != "progress" || progressEvt.Progress == nil || *progressEvt.Progress != 0.5 {
		t.Errorf("progress event = %+v, want progress=0.5", progressEvt)
	}

	// Completed event
	var completedEvt TaskEvent
	if err := json.Unmarshal(lines[3], &completedEvt); err != nil {
		t.Fatalf("decode completed: %v", err)
	}
	wantCompleted := events["completed_success"].(map[string]any)
	if completedEvt.Type != wantCompleted["type"] || completedEvt.ExitCode == nil || *completedEvt.ExitCode != 0 {
		t.Errorf("completed event = %+v, want type=%v exitCode=0", completedEvt, wantCompleted["type"])
	}
}
