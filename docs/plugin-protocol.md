# Architect Plugin Protocol v1

Architect Plugin Protocol v1 (APP v1) defines how Architect runs plugins as subprocesses instead of JVM-loaded JARs. The transport is JSON-RPC 2.0 over stdin/stdout, with newline-delimited JSON task events streamed on stdout during execution.

## Version

- Protocol version: `1.0.0`
- JSON-RPC version: `2.0`

## Lifecycle

1. Architect launches the plugin process.
2. Architect sends `init` with plugin config and protocol version.
3. Architect sends `listTasks` to discover task descriptors.
4. Architect sends `executeTask` when a task should run.
5. The plugin acknowledges `executeTask`, then streams task events as newline-delimited JSON.
6. Architect sends `shutdown` before terminating the process.

## Declaring a Process Plugin

Use `type: process` with a required `command` field in `architect.yml`:

```yaml
plugins:
  - name: my-go-plugin
    type: process
    command: "./my-go-plugin"
```

## Requests

### `init`

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "init",
  "params": {
    "config": {
      "example": true
    },
    "protocol_version": "1.0.0"
  }
}
```

Successful response:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "ok": true,
    "name": "my-plugin",
    "version": "1.2.3"
  }
}
```

### `listTasks`

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "listTasks",
  "params": {}
}
```

Successful response:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": [
    {
      "id": "build",
      "description": "Build the project",
      "phase": "BUILD",
      "dependencies": ["init"],
      "requires_confirmation": false
    }
  ]
}
```

### `executeTask`

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "executeTask",
  "params": {
    "id": "build",
    "args": ["--scan"],
    "env": {
      "ARCHITECT_PROJECT_DIR": "/workspace/project",
      "ARCHITECT_PROFILE": "default"
    }
  }
}
```

Immediate acknowledgement:

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": null
}
```

### `shutdown`

```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "shutdown",
  "params": {}
}
```

## Streaming Task Events

After `executeTask` is acknowledged, the plugin emits one JSON object per line on stdout.

### Output event

```json
{"type":"output","data":"Compiling sources"}
```

### Progress event

```json
{"type":"progress","progress":0.5}
```

### Error event

```json
{"type":"error","data":"Compilation failed"}
```

### Completion event

```json
{"type":"completed","exitCode":0}
```

## Error Responses

Plugins report request failures using standard JSON-RPC error objects.

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "error": {
    "code": -32600,
    "message": "Invalid request"
  }
}
```

## Task Descriptor Contract

- `id`: unique task identifier
- `description`: human-readable summary
- `phase`: optional Architect workflow phase name
- `dependencies`: optional task IDs that must run first
- `requires_confirmation`: optional boolean for destructive tasks

## Execution Contract

- Plugins must keep stdout machine-readable while APP v1 is active.
- Each `executeTask` stream must terminate with a `completed` event.
- `exitCode = 0` means success.
- Any non-zero `exitCode` or emitted `error` event is treated as task failure.