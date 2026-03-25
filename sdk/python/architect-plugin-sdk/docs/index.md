# Python Plugin SDK

Python SDK for authoring Architect Plugin Protocol v1 process plugins.

## Overview

The `architect-plugin-sdk` package provides a lightweight framework for building
Architect plugins in Python. Plugins communicate with the Architect Engine over
**stdin/stdout** using the **JSON-RPC 2.0** wire format defined by the Plugin
Protocol.

### Key Capabilities

- **JSON-RPC 2.0 transport** — handles framing, serialisation, and
  request/response correlation automatically.
- **Typed handler registration** — register handlers for `initialize`,
  `tasks/list`, `tasks/execute`, and other protocol methods with dataclass-based
  type hints.
- **Lifecycle management** — graceful startup, shutdown, and error propagation.

## Quick Start

```bash
pip install architect-plugin-sdk
```

```python
from architect_plugin_sdk import PluginServer, Task

server = PluginServer(name="my-plugin", version="0.1.0")

@server.on_tasks_list
async def list_tasks():
    return [Task(name="hello", description="Say hello")]

@server.on_task_execute("hello")
async def execute_hello(context):
    print("Hello from my plugin!", file=sys.stderr)

server.start()
```

## API Reference

See the source under `src/architect_plugin_sdk/` for the public API surface.

## Protocol

Full protocol specification:
[Plugin Protocol v1](https://github.com/architect-platform/architect/blob/main/docs/architecture/plugin-protocol.md)

## Configuration

Python plugins are declared in the project's `architect.yml` as process
plugins. The engine launches the plugin as a child process and communicates
over stdin/stdout:

```yaml
plugins:
  - name: my-plugin
    type: process
    command: "python -m my_plugin"
```

## Development

```bash
cd sdk/python/architect-plugin-sdk
pip install -e .          # install in editable mode
pip install -e ".[dev]"   # install with dev dependencies
pytest                    # run test suite
```

## Links

- [Plugin Protocol v1](https://github.com/architect-platform/architect/blob/main/docs/architecture/plugin-protocol.md)
- [STATUS.md](../../../../STATUS.md)
- [Architect Platform Docs](https://architect-platform.github.io/architect/)
