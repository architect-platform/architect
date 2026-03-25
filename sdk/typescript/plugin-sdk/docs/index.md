# TypeScript Plugin SDK

TypeScript SDK for authoring Architect Plugin Protocol v1 process plugins.

## Overview

The `@architect-platform/plugin-sdk` package provides a lightweight framework
for building Architect plugins in TypeScript (or JavaScript). Plugins
communicate with the Architect Engine over **stdin/stdout** using the
**JSON-RPC 2.0** wire format defined by the Plugin Protocol.

### Key Capabilities

- **JSON-RPC 2.0 transport** — handles framing, serialisation, and
  request/response correlation automatically.
- **Typed handler registration** — register handlers for `initialize`,
  `tasks/list`, `tasks/execute`, and other protocol methods with full
  TypeScript types.
- **Lifecycle management** — graceful startup, shutdown, and error propagation.

## Quick Start

```bash
npm install @architect-platform/plugin-sdk
```

```typescript
import { PluginServer } from "@architect-platform/plugin-sdk";

const server = new PluginServer({ name: "my-plugin", version: "0.1.0" });

server.onTasksList(async () => [
  { name: "hello", description: "Say hello" },
]);

server.onTaskExecute("hello", async () => {
  console.error("Hello from my plugin!");
});

server.start();
```

## API Reference

See the generated TypeDoc output in `dist/` after running `npm run build`, or
browse the source under `src/`.

## Protocol

Full protocol specification:
[Plugin Protocol v1](https://github.com/architect-platform/architect/blob/main/docs/architecture/plugin-protocol.md)

## Configuration

TypeScript plugins are declared in the project's `architect.yml` as process
plugins. The engine launches the plugin as a child process and communicates
over stdin/stdout:

```yaml
plugins:
  - name: my-plugin
    type: process
    command: "node dist/index.js"
```

## Development

```bash
cd sdk/typescript/plugin-sdk
npm install
npm run build        # compile TypeScript
npm run test         # run test suite
```

## Links

- [Plugin Protocol v1](https://github.com/architect-platform/architect/blob/main/docs/architecture/plugin-protocol.md)
- [STATUS.md](../../../../STATUS.md)
- [Architect Platform Docs](https://architect-platform.github.io/architect/)
