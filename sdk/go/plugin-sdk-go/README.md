# github.com/architect-platform/plugin-sdk-go

Go SDK for Architect Plugin Protocol v1 (APP v1). It lets you build Architect process plugins that communicate with the engine over JSON-RPC 2.0 on stdin/stdout.

## Install

```bash
go get github.com/architect-platform/plugin-sdk-go
```

## Usage

```go
package main

import (
	"context"

	architectplugin "github.com/architect-platform/plugin-sdk-go"
)

type examplePlugin struct{}

func (examplePlugin) ListTasks(context.Context) ([]architectplugin.TaskDescriptor, error) {
	return []architectplugin.TaskDescriptor{{
		ID:          "hello",
		Description: "Print a greeting",
	}}, nil
}

func (examplePlugin) ExecuteTask(_ context.Context, request architectplugin.ExecuteTaskParams, writer architectplugin.TaskEventWriter) (int, error) {
	if err := writer.Output("running " + request.ID); err != nil {
		return 1, err
	}
	if err := writer.Progress(1); err != nil {
		return 1, err
	}
	return 0, nil
}

func main() {
	_ = architectplugin.Run(context.Background(), examplePlugin{})
}
```

## API

- `Run(ctx, plugin)` starts the APP v1 server on stdin/stdout
- `NewServer(plugin)` creates a reusable server instance for tests or custom hosting
- `Plugin` defines `ListTasks` and `ExecuteTask`
- `InitializablePlugin` optionally adds `Init`
- `TaskEventWriter` emits `output`, `error`, and `progress` events

## Protocol Coverage

This SDK implements the full APP v1 lifecycle:

- `init`
- `listTasks`
- `executeTask`
- `shutdown`

It emits newline-delimited JSON task events for:

- `output`
- `progress`
- `error`
- `completed`

## Development

```bash
go test ./...
```