import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { PassThrough, Writable } from "node:stream";
import { ArchitectProcessPlugin, PluginServer } from "../src";
import {
  APP_VERSION,
  JSON_RPC_VERSION,
  METHODS,
  EVENT_TYPES,
  JSON_RPC_ERROR,
} from "../src/protocol";

// The test runs from dist/test/ after tsc compilation, so walk up from __dirname
// to find the repo root, then locate docs/protocol-fixtures.json.
function findFixturesPath(): string {
  let dir = resolve(__dirname);
  for (let i = 0; i < 10; i++) {
    const candidate = resolve(dir, "docs", "protocol-fixtures.json");
    try {
      readFileSync(candidate);
      return candidate;
    } catch {
      dir = resolve(dir, "..");
    }
  }
  throw new Error("Cannot locate docs/protocol-fixtures.json");
}

const fixtures = JSON.parse(readFileSync(findFixturesPath(), "utf-8"));

class CaptureWritable extends Writable {
  private readonly chunks: string[] = [];
  _write(chunk: Buffer | string, _encoding: BufferEncoding, cb: (e?: Error | null) => void): void {
    this.chunks.push(chunk.toString());
    cb();
  }
  lines(): string[] {
    return this.chunks.join("").trim().split("\n").filter(Boolean);
  }
}

test("protocol constants match shared fixtures", () => {
  assert.equal(JSON_RPC_VERSION, fixtures.json_rpc_version);
  assert.equal(APP_VERSION, fixtures.protocol_version);
  assert.equal(METHODS.init, "init");
  assert.equal(METHODS.listTasks, "listTasks");
  assert.equal(METHODS.executeTask, "executeTask");
  assert.equal(METHODS.shutdown, "shutdown");
  assert.equal(EVENT_TYPES.output, "output");
  assert.equal(EVENT_TYPES.progress, "progress");
  assert.equal(EVENT_TYPES.error, "error");
  assert.equal(EVENT_TYPES.completed, "completed");
  assert.equal(JSON_RPC_ERROR.invalidRequest, -32600);
});

test("server can process fixture init and listTasks requests", async () => {
  const output = new CaptureWritable();
  const plugin: ArchitectProcessPlugin = {
    async init(config) {
      return { ok: true, name: "test-plugin", version: "1.0.0" };
    },
    async listTasks() {
      return [
        {
          id: "build",
          description: "Build the project",
          phase: "build",
          dependencies: ["init"],
          permissions: ["process:exec"],
          requires_confirmation: false,
        },
        {
          id: "deploy",
          description: "Deploy to production",
          phase: "release",
          dependencies: ["build"],
          permissions: ["process:exec", "network:outbound"],
          requires_confirmation: true,
        },
      ];
    },
    async executeTask() {
      return 0;
    },
  };

  const server = new PluginServer(plugin, {
    input: new PassThrough(),
    output,
    exit: () => undefined,
  });

  await server.handleRequest(fixtures.requests.init);
  await server.handleRequest(fixtures.requests.listTasks);

  const lines = output.lines().map((l) => JSON.parse(l));

  // Init response matches fixture shape
  assert.equal(lines[0].jsonrpc, fixtures.responses.init_success.jsonrpc);
  assert.equal(lines[0].id, fixtures.responses.init_success.id);
  assert.equal(lines[0].result.ok, true);
  assert.equal(lines[0].result.name, "test-plugin");

  // ListTasks response matches fixture shape
  assert.equal(lines[1].jsonrpc, "2.0");
  assert.equal(lines[1].id, fixtures.responses.listTasks_success.id);
  assert.equal(lines[1].result.length, 2);
  assert.equal(lines[1].result[0].id, "build");
  assert.equal(lines[1].result[1].requires_confirmation, true);
});

test("server streams fixture-compatible events during executeTask", async () => {
  const output = new CaptureWritable();
  const plugin: ArchitectProcessPlugin = {
    listTasks() {
      return [];
    },
    async executeTask(_req, writer) {
      await writer.output("Compiling sources...");
      await writer.progress(0.5);
      return 0;
    },
  };

  const server = new PluginServer(plugin, {
    input: new PassThrough(),
    output,
    exit: () => undefined,
  });

  await server.handleRequest(fixtures.requests.executeTask);

  const lines = output.lines().map((l) => JSON.parse(l));

  // Ack matches fixture shape
  assert.equal(lines[0].id, fixtures.responses.executeTask_ack.id);
  assert.equal(lines[0].result, null);

  // Output event matches fixture event shape
  assert.equal(lines[1].type, fixtures.events.output.type);
  assert.equal(lines[1].data, fixtures.events.output.data);

  // Progress event
  assert.equal(lines[2].type, fixtures.events.progress.type);
  assert.equal(lines[2].progress, fixtures.events.progress.progress);

  // Completed event
  assert.equal(lines[3].type, fixtures.events.completed_success.type);
  assert.equal(lines[3].exitCode, fixtures.events.completed_success.exitCode);
});

test("server rejects unsupported protocol version per fixture", async () => {
  const output = new CaptureWritable();
  const plugin: ArchitectProcessPlugin = {
    listTasks() {
      return [];
    },
    async executeTask() {
      return 0;
    },
  };

  const server = new PluginServer(plugin, {
    input: new PassThrough(),
    output,
    exit: () => undefined,
  });

  await server.handleRequest({
    jsonrpc: JSON_RPC_VERSION,
    id: 1,
    method: METHODS.init,
    params: { config: {}, protocol_version: "2.0.0" },
  });

  const lines = output.lines().map((l) => JSON.parse(l));
  assert.ok(lines[0].error, "should return error for wrong protocol version");
  assert.equal(lines[0].error.code, fixtures.responses.protocol_version_error.error.code);
});
