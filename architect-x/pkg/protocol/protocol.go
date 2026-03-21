package protocol

// Request is a JSON-RPC request for binary plugin communication.
type Request struct {
	Method string      `json:"method"`
	Params interface{} `json:"params,omitempty"`
	ID     int         `json:"id"`
}

// Response is a JSON-RPC response from a binary plugin.
type Response struct {
	Result interface{} `json:"result,omitempty"`
	Error  *Error      `json:"error,omitempty"`
	ID     int         `json:"id"`
}

// Error represents a JSON-RPC error.
type Error struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
}

// InitializeParams are sent to a plugin on startup.
type InitializeParams struct {
	Config map[string]interface{} `json:"config"`
}

// InitializeResult is returned by a plugin after initialization.
type InitializeResult struct {
	Actions []ActionDef `json:"actions"`
	Rules   []RuleDef   `json:"rules"`
}

// ActionDef is a plugin-provided action definition.
type ActionDef struct {
	ID          string   `json:"id"`
	Description string   `json:"description"`
	AttachTo    string   `json:"attach_to,omitempty"`
	DependsOn   []string `json:"depends_on,omitempty"`
}

// RuleDef is a plugin-provided rule definition.
type RuleDef struct {
	ID          string   `json:"id"`
	Description string   `json:"description"`
	Severity    string   `json:"severity"`
	Trigger     []string `json:"trigger"`
}

// ExecuteParams are sent when executing a plugin action.
type ExecuteParams struct {
	Action string                 `json:"action"`
	Inputs map[string]interface{} `json:"inputs,omitempty"`
	Env    map[string]string      `json:"env,omitempty"`
}

// ExecuteResult is returned after executing a plugin action.
type ExecuteResult struct {
	Success bool   `json:"success"`
	Output  string `json:"output,omitempty"`
}
