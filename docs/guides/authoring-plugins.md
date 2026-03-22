# Plugin Authoring Guide

This guide walks through the fastest supported path for creating, testing, documenting, validating, and publishing Architect plugins.

## Choose a Plugin Model

Architect supports two plugin models:

- JVM plugins: packaged as JARs, loaded through Java SPI, ideal when you want direct access to the Architect API.
- Process plugins: run as separate processes over the Architect Plugin Protocol v1 (APP v1), ideal for TypeScript, Go, Python, or any other language.

Use a JVM plugin when you want native access to `ArchitectPlugin`, `TaskRegistry`, and the in-process task API. Use a process plugin when the team already has an established language runtime or toolchain outside the JVM.

## Fastest Start

The CLI scaffolds Kotlin, TypeScript, and Go plugins directly:

```bash
architect plugin create my-plugin
architect plugin create my-process-plugin typescript
architect plugin create my-go-plugin go
```

Each scaffold includes:

- `plugin.yml` metadata
- a starter implementation
- a test harness
- a README with build commands

## Plugin Metadata

Every scaffold starts with a `plugin.yml` file:

```yaml
name: my-plugin
template: kotlin
version: 0.1.0
entrypoint: io.github.architectplatform.plugins.myplugin.MyPlugin
description: Scaffolded kotlin plugin for Architect.
```

This metadata now drives the built-in documentation workflow:

```bash
architect plugin docs path/to/plugin
```

The command generates `PLUGIN_REFERENCE.md` from `plugin.yml` and, for scaffolded templates, includes detected task metadata.

## Kotlin JVM Plugins

Kotlin plugins implement `ArchitectPlugin<C>` and are loaded from a JAR via Java SPI.

```kotlin
class ExamplePlugin : ArchitectPlugin<ExampleContext> {
  override val id = "example-plugin"
  override val contextKey = "example"
  override val ctxClass = ExampleContext::class.java
  override var context: ExampleContext = ExampleContext()

  override fun register(registry: TaskRegistry) {
    registry.add(ExampleTask(context))
  }
}
```

Requirements for JVM plugins:

- implement `ArchitectPlugin`
- expose a valid `ctxClass`
- package a SPI file at `META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin`
- build a JAR that contains the plugin implementation and its context classes

Typical JVM workflow:

```bash
cd my-plugin/app
./gradlew build
./gradlew test
```

## TypeScript Process Plugins

TypeScript process plugins use the published npm SDK:

```bash
npm install @architect-platform/plugin-sdk
```

Minimal structure:

```ts
import { runPlugin, type ArchitectProcessPlugin } from "@architect-platform/plugin-sdk";

const plugin: ArchitectProcessPlugin = {
  async listTasks() {
    return [{ id: "hello", description: "Print a greeting", phase: "BUILD" }];
  },

  async executeTask(request, writer) {
    await writer.output(`running ${request.id}`);
    return 0;
  },
};

void runPlugin(plugin);
```

Build and test:

```bash
npm install
npm run build
npm test
```

See the SDK example at `sdk/typescript/plugin-sdk/examples/full-featured-plugin.ts` for the full APP v1 lifecycle.

## Go Process Plugins

Go process plugins use the Go SDK:

```bash
go get github.com/architect-platform/plugin-sdk-go
```

Minimal structure:

```go
package main

import (
  "context"

  architectplugin "github.com/architect-platform/plugin-sdk-go"
)

type examplePlugin struct{}

func (examplePlugin) ListTasks(context.Context) ([]architectplugin.TaskDescriptor, error) {
  return []architectplugin.TaskDescriptor{{ID: "hello", Description: "Print a greeting", Phase: "BUILD"}}, nil
}

func (examplePlugin) ExecuteTask(_ context.Context, request architectplugin.ExecuteTaskParams, writer architectplugin.TaskEventWriter) (int, error) {
  _ = writer.Output("running " + request.ID)
  return 0, nil
}

func main() {
  _ = architectplugin.Run(context.Background(), examplePlugin{})
}
```

Build and test:

```bash
go test ./...
go build ./...
```

See the SDK example at `sdk/go/plugin-sdk-go/examples/full-featured/main.go` for a more complete implementation.

## Python Process Plugins

Python plugins are not scaffolded by the CLI yet, but the SDK is ready to use:

```bash
pip install architect-plugin-sdk
```

Minimal structure:

```python
from architect_plugin_sdk import PluginServer, TaskDescriptor


class ExamplePlugin:
    def list_tasks(self):
        return [TaskDescriptor(id="hello", description="Print a greeting")]

    def execute_task(self, request, writer):
        writer.output(f"running {request.id}")
        return 0


if __name__ == "__main__":
    PluginServer(ExamplePlugin()).serve()
```

Development workflow:

```bash
PYTHONPATH=src python -m unittest discover -s tests
PYTHONPATH=src python examples/full_featured_plugin.py
```

See `sdk/python/architect-plugin-sdk/examples/full_featured_plugin.py` for a complete APP v1 example.

## Testing Plugins

JVM plugins should use `ArchitectPluginTestKit` for fast, in-memory tests without a running engine:

```kotlin
val kit = ArchitectPluginTestKit(MyPlugin())
kit.configure(mapOf("setting" to "value"))
val result = kit.executeTask("my-task")
```

This is the fastest way to verify:

- config mapping into your context class
- task registration
- task execution behavior
- service injection and emitted events

Process plugins should keep protocol-level tests in their own language and add at least one end-to-end invocation of the plugin process.

## Local Development Loop

When developing a local JVM plugin, use a `type: local` plugin declaration in the target project's `architect.yml` and reload it without restarting the engine:

```yaml
plugins:
  - name: my-plugin
    type: local
    path: ./build/libs/my-plugin.jar
```

Then rebuild the plugin and reload it:

```bash
architect engine reload-plugins
```

For process plugins, point the target project at the plugin command you want Architect to run.

## Documentation and Validation

Architect now ships authoring helpers for plugin packaging:

```bash
architect plugin docs path/to/plugin
architect plugin validate path/to/plugin.jar
```

`architect plugin validate` checks:

- the SPI file exists for a JVM plugin JAR
- at least one `ArchitectPlugin` implementation can be discovered
- the plugin context can initialize from config using `ArchitectPluginTestKit`

Use it before publishing any JVM plugin artifact.

## Publishing Checklist

Before publishing a plugin, confirm all of the following:

1. The plugin has a stable `id`, `contextKey`, and context model.
2. The plugin exposes at least one meaningful task with descriptions.
3. Tests pass locally.
4. `architect plugin docs` generates an accurate `PLUGIN_REFERENCE.md`.
5. `architect plugin validate` passes for the packaged JAR.
6. The plugin README explains configuration, tasks, and local development.
7. Process plugins document their runtime requirements and entrypoint command.

## Related References

- `docs/plugin-protocol.md` for APP v1 details
- `architect-api/api/src/main/kotlin/io/github/architectplatform/api/core/plugins/ArchitectPlugin.kt` for the JVM plugin contract
- `architect-api/api/src/main/kotlin/io/github/architectplatform/api/testing/ArchitectPluginTestKit.kt` for the in-memory test harness
- `sdk/typescript/plugin-sdk/README.md` for the TypeScript SDK
- `sdk/go/plugin-sdk-go/README.md` for the Go SDK
- `sdk/python/architect-plugin-sdk/README.md` for the Python SDK