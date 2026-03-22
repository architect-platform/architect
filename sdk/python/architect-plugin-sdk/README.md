# architect-plugin-sdk

Python SDK for Architect Plugin Protocol v1 (APP v1). It lets you build Architect process plugins that communicate with the engine over JSON-RPC 2.0 on stdin/stdout.

## Install

```bash
pip install architect-plugin-sdk
```

## Usage

```python
from architect_plugin_sdk import PluginServer, TaskDescriptor


class ExamplePlugin:
    def list_tasks(self):
        return [TaskDescriptor(id="hello", description="Print a greeting")]

    def execute_task(self, request, writer):
        writer.output(f"running {request.id}")
        writer.progress(1.0)
        return 0


if __name__ == "__main__":
    PluginServer(ExamplePlugin()).serve()
```

## API

- `PluginServer`: APP v1 JSON-RPC server for stdin/stdout plugins
- `PluginEventWriter`: helper for `output`, `error`, and `progress` events
- `TaskDescriptor`, `ExecuteTaskParams`, `TaskEvent`, `InitResult`: typed protocol models
- `run_plugin(plugin)`: convenience helper that starts the server immediately

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
PYTHONPATH=src python -m unittest discover -s tests
```