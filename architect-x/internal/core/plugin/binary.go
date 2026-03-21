package plugin

import (
	"bufio"
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"sync"

	"github.com/architect-platform/architect/internal/core/action"
	"github.com/architect-platform/architect/internal/core/rule"
	"github.com/architect-platform/architect/pkg/protocol"
)

// BinaryPlugin manages a plugin process communicating via JSON-RPC over stdin/stdout.
type BinaryPlugin struct {
	Path    string
	cmd     *exec.Cmd
	stdin   *json.Encoder
	stdout  *bufio.Scanner
	mu      sync.Mutex
	nextID  int
	running bool
}

// NewBinaryPlugin creates a new binary plugin process handler.
func NewBinaryPlugin(path string) *BinaryPlugin {
	return &BinaryPlugin{Path: path}
}

// Start launches the plugin process.
func (bp *BinaryPlugin) Start() error {
	bp.mu.Lock()
	defer bp.mu.Unlock()

	bp.cmd = exec.Command(bp.Path)
	bp.cmd.Stderr = os.Stderr

	stdin, err := bp.cmd.StdinPipe()
	if err != nil {
		return fmt.Errorf("creating stdin pipe: %w", err)
	}
	bp.stdin = json.NewEncoder(stdin)

	stdout, err := bp.cmd.StdoutPipe()
	if err != nil {
		return fmt.Errorf("creating stdout pipe: %w", err)
	}
	bp.stdout = bufio.NewScanner(stdout)
	bp.stdout.Buffer(make([]byte, 0, 1024*1024), 1024*1024) // 1MB buffer

	if err := bp.cmd.Start(); err != nil {
		return fmt.Errorf("starting plugin %s: %w", bp.Path, err)
	}

	bp.running = true
	return nil
}

// Initialize sends the initialize request and returns plugin definitions.
func (bp *BinaryPlugin) Initialize(config map[string]interface{}) (*Plugin, error) {
	resp, err := bp.call("initialize", protocol.InitializeParams{Config: config})
	if err != nil {
		return nil, err
	}

	var initResult protocol.InitializeResult
	data, _ := json.Marshal(resp.Result)
	if err := json.Unmarshal(data, &initResult); err != nil {
		return nil, fmt.Errorf("parsing initialize result: %w", err)
	}

	p := &Plugin{
		Actions: make(map[string]*action.Action),
		Rules:   make(map[string]*rule.Rule),
	}

	for _, ad := range initResult.Actions {
		p.Actions[ad.ID] = &action.Action{
			ID:          ad.ID,
			Description: ad.Description,
			AttachTo:    ad.AttachTo,
			DependsOn:   ad.DependsOn,
		}
	}

	for _, rd := range initResult.Rules {
		sev := rule.SeverityWarn
		switch rd.Severity {
		case "error":
			sev = rule.SeverityError
		case "info":
			sev = rule.SeverityInfo
		}
		p.Rules[rd.ID] = &rule.Rule{
			ID:          rd.ID,
			Description: rd.Description,
			Severity:    sev,
			Trigger:     rd.Trigger,
		}
	}

	return p, nil
}

// Execute runs an action through the plugin process.
func (bp *BinaryPlugin) Execute(actionID string, inputs map[string]interface{}, env map[string]string) (*protocol.ExecuteResult, error) {
	resp, err := bp.call("execute", protocol.ExecuteParams{
		Action: actionID,
		Inputs: inputs,
		Env:    env,
	})
	if err != nil {
		return nil, err
	}

	var result protocol.ExecuteResult
	data, _ := json.Marshal(resp.Result)
	if err := json.Unmarshal(data, &result); err != nil {
		return nil, fmt.Errorf("parsing execute result: %w", err)
	}

	return &result, nil
}

// Stop terminates the plugin process.
func (bp *BinaryPlugin) Stop() error {
	bp.mu.Lock()
	defer bp.mu.Unlock()

	if !bp.running {
		return nil
	}

	bp.running = false
	if bp.cmd.Process != nil {
		return bp.cmd.Process.Kill()
	}
	return nil
}

func (bp *BinaryPlugin) call(method string, params interface{}) (*protocol.Response, error) {
	bp.mu.Lock()
	defer bp.mu.Unlock()

	bp.nextID++
	req := protocol.Request{
		Method: method,
		Params: params,
		ID:     bp.nextID,
	}

	if err := bp.stdin.Encode(req); err != nil {
		return nil, fmt.Errorf("sending request: %w", err)
	}

	if !bp.stdout.Scan() {
		return nil, fmt.Errorf("reading response: %w", bp.stdout.Err())
	}

	var resp protocol.Response
	if err := json.Unmarshal(bp.stdout.Bytes(), &resp); err != nil {
		return nil, fmt.Errorf("parsing response: %w", err)
	}

	if resp.Error != nil {
		return nil, fmt.Errorf("plugin error [%d]: %s", resp.Error.Code, resp.Error.Message)
	}

	return &resp, nil
}
