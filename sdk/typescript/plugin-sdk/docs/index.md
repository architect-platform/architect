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

## Status

**Incubating** — protocol coverage is documented; tests and examples present.
See `STATUS.md` at the repository root.
