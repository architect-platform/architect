package architectplugin

const (
	JSONRPCVersion = "2.0"
	APPVersion     = "1.0.0"

	MethodInit        = "init"
	MethodListTasks   = "listTasks"
	MethodExecuteTask = "executeTask"
	MethodShutdown    = "shutdown"

	EventOutput    = "output"
	EventProgress  = "progress"
	EventError     = "error"
	EventCompleted = "completed"

	ErrorParseError    = -32700
	ErrorInvalid       = -32600
	ErrorMethodMissing = -32601
	ErrorInternal      = -32603
)

type Request struct {
	JSONRPC string `json:"jsonrpc"`
	ID      int    `json:"id"`
	Method  string `json:"method"`
	Params  any    `json:"params,omitempty"`
}

type Response struct {
	JSONRPC string    `json:"jsonrpc"`
	ID      *int      `json:"id"`
	Result  any       `json:"result,omitempty"`
	Error   *RPCError `json:"error,omitempty"`
}

type RPCError struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
	Data    any    `json:"data,omitempty"`
}

type InitParams struct {
	Config          map[string]any `json:"config,omitempty"`
	ProtocolVersion string         `json:"protocol_version"`
}

type InitResult struct {
	OK      bool   `json:"ok"`
	Name    string `json:"name,omitempty"`
	Version string `json:"version,omitempty"`
}

type TaskDescriptor struct {
	ID                   string   `json:"id"`
	Description          string   `json:"description,omitempty"`
	Phase                string   `json:"phase,omitempty"`
	Dependencies         []string `json:"dependencies,omitempty"`
	Permissions          []string `json:"permissions,omitempty"`
	RequiresConfirmation bool     `json:"requires_confirmation,omitempty"`
}

type ExecuteTaskParams struct {
	ID   string            `json:"id"`
	Args []string          `json:"args,omitempty"`
	Env  map[string]string `json:"env,omitempty"`
}

type TaskEvent struct {
	Type     string   `json:"type"`
	Data     string   `json:"data,omitempty"`
	Progress *float64 `json:"progress,omitempty"`
	ExitCode *int     `json:"exitCode,omitempty"`
}
