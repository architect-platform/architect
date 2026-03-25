# Go Plugin SDK

Go SDK for authoring Architect Plugin Protocol v1 process plugins.

## Overview

The `plugin-sdk-go` module provides a lightweight framework for building
Architect plugins in Go. Plugins communicate with the Architect Engine over
**stdin/stdout** using the **JSON-RPC 2.0** wire format defined by the Plugin
Protocol.

### Key Capabilities

- **JSON-RPC 2.0 transport** — handles framing, serialisation, and
  request/response correlation automatically.
- **Typed handler registration** — register handlers for `initialize`,
  `tasks/list`, `tasks/execute`, and other protocol methods with Go structs.
- **Lifecycle management** — graceful startup, shutdown, and error propagation.
- **Single-binary deployment** — plugins compile to a self-contained executable.

## Quick Start

```bash
go get github.com/architect-platform/plugin-sdk-go
```

```go
package main

import "github.com/architect-platform/plugin-sdk-go/sdk"

func main() {
    s := sdk.NewServer("my-plugin", "0.1.0")

    s.OnTasksList(func() []sdk.Task {
        return []sdk.Task{{Name: "hello", Description: "Say hello"}}
    })

    s.OnTaskExecute("hello", func(ctx sdk.ExecContext) error {
        ctx.Log("Hello from my plugin!")
        return nil
    })

    s.Start()
}
```

## API Reference

See the GoDoc comments in `sdk/` for the public API surface, or run:

```bash
go doc ./sdk/...
```

## Protocol

Full protocol specification:
[Plugin Protocol v1](https://github.com/architect-platform/architect/blob/main/docs/architecture/plugin-protocol.md)

## Configuration

Go plugins are declared in the project's `architect.yml` as process plugins.
The engine launches the compiled binary as a child process and communicates
over stdin/stdout:

```yaml
plugins:
  - name: my-plugin
    type: process
    command: "./my-plugin"
```

## Development

```bash
cd sdk/go/plugin-sdk-go
go build ./...    # build all packages
go test ./...     # run test suite
go vet ./...      # static analysis
```

## Links

- [Plugin Protocol v1](https://github.com/architect-platform/architect/blob/main/docs/architecture/plugin-protocol.md)
- [STATUS.md](../../../../STATUS.md)
- [Architect Platform Docs](https://architect-platform.github.io/architect/)
