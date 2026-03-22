package architectplugin

import (
	"bufio"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"sync"
)

type Server struct {
	plugin Plugin
	input  io.Reader
	output io.Writer
	exit   func(code int)
	mu     sync.Mutex

	shuttingDown bool
}

func NewServer(plugin Plugin) *Server {
	return &Server{
		plugin: plugin,
		input:  os.Stdin,
		output: os.Stdout,
		exit: func(code int) {
			os.Exit(code)
		},
	}
}

func (s *Server) WithIO(input io.Reader, output io.Writer, exit func(code int)) *Server {
	if input != nil {
		s.input = input
	}
	if output != nil {
		s.output = output
	}
	if exit != nil {
		s.exit = exit
	}
	return s
}

func (s *Server) Listen(ctx context.Context) error {
	scanner := bufio.NewScanner(s.input)
	for scanner.Scan() {
		line := scanner.Bytes()
		if len(line) == 0 {
			continue
		}

		if err := s.handleLine(ctx, line); err != nil {
			return err
		}

		if s.shuttingDown {
			return nil
		}
	}

	return scanner.Err()
}

func (s *Server) HandleRequest(ctx context.Context, request Request) error {
	switch request.Method {
	case MethodInit:
		return s.handleInit(ctx, request)
	case MethodListTasks:
		return s.handleListTasks(ctx, request)
	case MethodExecuteTask:
		return s.handleExecuteTask(ctx, request)
	case MethodShutdown:
		return s.handleShutdown(request)
	default:
		return s.writeResponse(Response{
			JSONRPC: JSONRPCVersion,
			ID:      intPtr(request.ID),
			Error: &RPCError{
				Code:    ErrorMethodMissing,
				Message: fmt.Sprintf("Unknown method: %s", request.Method),
			},
		})
	}
}

func Run(ctx context.Context, plugin Plugin) error {
	return NewServer(plugin).Listen(ctx)
}

func (s *Server) handleLine(ctx context.Context, line []byte) error {
	var request Request
	if err := json.Unmarshal(line, &request); err != nil {
		return s.writeResponse(Response{
			JSONRPC: JSONRPCVersion,
			ID:      nil,
			Error: &RPCError{
				Code:    ErrorParseError,
				Message: "Invalid JSON-RPC payload",
			},
		})
	}

	if request.JSONRPC != JSONRPCVersion || request.Method == "" {
		return s.writeResponse(Response{
			JSONRPC: JSONRPCVersion,
			ID:      intPtr(request.ID),
			Error: &RPCError{
				Code:    ErrorInvalid,
				Message: "Invalid JSON-RPC request",
			},
		})
	}

	return s.HandleRequest(ctx, request)
}

func (s *Server) handleInit(ctx context.Context, request Request) error {
	var params InitParams
	if err := decodeParams(request.Params, &params); err != nil {
		return s.writeResponse(invalidParams(request.ID, err))
	}
	if params.ProtocolVersion != APPVersion {
		return s.writeResponse(Response{
			JSONRPC: JSONRPCVersion,
			ID:      intPtr(request.ID),
			Error: &RPCError{
				Code:    ErrorInvalid,
				Message: fmt.Sprintf("Unsupported protocol version: %s", params.ProtocolVersion),
			},
		})
	}

	result := InitResult{OK: true}
	if initPlugin, ok := s.plugin.(InitializablePlugin); ok {
		pluginResult, err := initPlugin.Init(ctx, params.Config)
		if err != nil {
			return s.writeResponse(internalError(request.ID, err))
		}
		if pluginResult.OK || pluginResult.Name != "" || pluginResult.Version != "" {
			result = pluginResult
			if !result.OK {
				result.OK = true
			}
		}
	}

	return s.writeResponse(Response{
		JSONRPC: JSONRPCVersion,
		ID:      intPtr(request.ID),
		Result:  result,
	})
}

func (s *Server) handleListTasks(ctx context.Context, request Request) error {
	tasks, err := s.plugin.ListTasks(ctx)
	if err != nil {
		return s.writeResponse(internalError(request.ID, err))
	}

	return s.writeResponse(Response{
		JSONRPC: JSONRPCVersion,
		ID:      intPtr(request.ID),
		Result:  tasks,
	})
}

func (s *Server) handleExecuteTask(ctx context.Context, request Request) error {
	var params ExecuteTaskParams
	if err := decodeParams(request.Params, &params); err != nil {
		return s.writeResponse(invalidParams(request.ID, err))
	}
	if params.ID == "" {
		return s.writeResponse(Response{
			JSONRPC: JSONRPCVersion,
			ID:      intPtr(request.ID),
			Error: &RPCError{
				Code:    ErrorInvalid,
				Message: "executeTask requires an id",
			},
		})
	}

	if err := s.writeResponse(Response{
		JSONRPC: JSONRPCVersion,
		ID:      intPtr(request.ID),
		Result:  nil,
	}); err != nil {
		return err
	}

	writer := &eventWriter{server: s}
	exitCode, err := s.plugin.ExecuteTask(ctx, params, writer)
	if err != nil {
		if writeErr := writer.Error(err.Error()); writeErr != nil {
			return writeErr
		}
		exitCode = 1
	}

	return s.writeEvent(TaskEvent{
		Type:     EventCompleted,
		ExitCode: intPtr(exitCode),
	})
}

func (s *Server) handleShutdown(request Request) error {
	s.shuttingDown = true
	if err := s.writeResponse(Response{
		JSONRPC: JSONRPCVersion,
		ID:      intPtr(request.ID),
		Result:  nil,
	}); err != nil {
		return err
	}
	s.exit(0)
	return nil
}

func (s *Server) writeResponse(response Response) error {
	return s.writeJSON(response)
}

func (s *Server) writeEvent(event TaskEvent) error {
	return s.writeJSON(event)
}

func (s *Server) writeJSON(value any) error {
	payload, err := json.Marshal(value)
	if err != nil {
		return err
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	_, err = fmt.Fprintf(s.output, "%s\n", payload)
	return err
}

type eventWriter struct {
	server *Server
}

func (w *eventWriter) Output(text string) error {
	return w.server.writeEvent(TaskEvent{Type: EventOutput, Data: text})
}

func (w *eventWriter) Error(text string) error {
	return w.server.writeEvent(TaskEvent{Type: EventError, Data: text})
}

func (w *eventWriter) Progress(value float64) error {
	return w.server.writeEvent(TaskEvent{Type: EventProgress, Progress: floatPtr(value)})
}

func decodeParams(raw any, target any) error {
	data, err := json.Marshal(raw)
	if err != nil {
		return err
	}
	return json.Unmarshal(data, target)
}

func invalidParams(id int, err error) Response {
	return Response{
		JSONRPC: JSONRPCVersion,
		ID:      intPtr(id),
		Error: &RPCError{
			Code:    ErrorInvalid,
			Message: err.Error(),
		},
	}
}

func internalError(id int, err error) Response {
	return Response{
		JSONRPC: JSONRPCVersion,
		ID:      intPtr(id),
		Error: &RPCError{
			Code:    ErrorInternal,
			Message: err.Error(),
		},
	}
}

func intPtr(value int) *int {
	return &value
}

func floatPtr(value float64) *float64 {
	return &value
}
