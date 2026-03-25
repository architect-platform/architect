# ADR-004: Support non-JVM plugins via JSON-RPC subprocess protocol

| Field    | Value      |
|----------|------------|
| **Status** | Accepted |
| **Date**   | 2025-01-15 |
| **Authors** | Architect Platform contributors |

## Context

ADR-003 established Java SPI as the plugin loading mechanism for JVM plugins.
However, restricting plugins to JVM languages limits ecosystem growth. Many
developer-tooling teams work primarily in TypeScript, Python, or Go and are
unlikely to adopt Kotlin/Java solely to write an Architect plugin.

We evaluated three approaches for cross-language plugin support:

1. **gRPC** — strongly typed, high performance, but requires code generation
   and a network listener per plugin. Adds significant complexity to both the
   engine and plugin authoring experience.
2. **REST API** — simple, but requires each plugin to run an HTTP server.
   Port management, lifecycle control, and startup latency make this
   heavyweight for small plugins.
3. **JSON-RPC over stdin/stdout** — the engine spawns the plugin as a child
   process and communicates via newline-delimited JSON on standard streams.
   Lightweight, no networking, and natural process-level isolation.

## Decision

The Architect Plugin Protocol v1 uses **JSON-RPC 2.0 over stdin/stdout** with
newline-delimited JSON (NDJSON) framing.

The engine spawns the plugin binary as a subprocess, sends requests as JSON-RPC
messages on stdin, and reads responses from stdout. Stderr is forwarded to the
engine's logging subsystem.

Official SDKs are provided for TypeScript (`@architect-platform/sdk`), Python
(`architect-sdk`), and Go (`architect-sdk-go`). Each SDK handles protocol
framing, request dispatching, and lifecycle signals.

## Consequences

### Positive

- Any programming language that can read stdin and write stdout can implement
  a plugin — no JVM, no code generation, no network stack required.
- Each process plugin runs in its own OS process, providing natural memory and
  fault isolation. A crashing plugin does not bring down the engine.
- The protocol is simple enough that a minimal plugin can be implemented
  without an SDK, using only standard-library JSON parsing.

### Negative

- Inter-process communication via stdin/stdout has higher latency than
  in-process JVM calls. Plugins that require many fine-grained interactions
  with the engine may notice performance overhead.
- Process plugins cannot share in-memory state with the engine or with other
  plugins. All data must be serialized across the process boundary.
- The engine must manage subprocess lifecycles (spawn, health-check, timeout,
  kill), adding operational complexity compared to in-process loading.
