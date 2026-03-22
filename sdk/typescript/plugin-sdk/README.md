# @architect-platform/plugin-sdk

TypeScript SDK for Architect Plugin Protocol v1 (APP v1). It lets you build Architect process plugins that communicate with the engine over JSON-RPC 2.0 on stdin/stdout.

## Install

```bash
npm install @architect-platform/plugin-sdk
```

## Usage

```ts
import { runPlugin, type ArchitectProcessPlugin } from "@architect-platform/plugin-sdk";

const plugin: ArchitectProcessPlugin = {
  async init(config) {
    return {
      ok: true,
      name: "example-plugin",
      version: "1.0.0",
    };
  },

  async listTasks() {
    return [
      {
        id: "hello",
        description: "Print a greeting",
      },
    ];
  },

  async executeTask(request, writer) {
    await writer.output(`Running ${request.id}`);
    await writer.progress(1);
    return 0;
  },
};

void runPlugin(plugin);
```

## Full Example Plugin

A full-featured APP v1 example is available at `examples/full-featured-plugin.ts`.

It demonstrates:

- `init(config)` returning plugin metadata
- `listTasks()` with `phase`, `dependencies`, and `requires_confirmation`
- `executeTask(id, args, env)` using `output`, `progress`, and `error` events
- Success (`exitCode: 0`) and failure (`exitCode: 1`) task paths

Use this example as a template in your plugin package where `@architect-platform/plugin-sdk` is installed.

## API

- `runPlugin(plugin)`: starts the APP v1 JSON-RPC server on stdin/stdout
- `PluginServer`: lower-level server class for custom hosting and testing
- `ArchitectProcessPlugin`: TypeScript interface for implementing process plugins
- `TaskEventWriter`: helper passed to `executeTask` for `output`, `error`, and `progress` events
- `protocol` exports: typed APP v1 constants and message contracts

## Protocol Coverage

This SDK implements the full APP v1 request lifecycle:

- `init`
- `listTasks`
- `executeTask`
- `shutdown`

During `executeTask`, the SDK emits newline-delimited JSON events for:

- `output`
- `progress`
- `error`
- `completed`

## Development

```bash
npm install
npm test
```