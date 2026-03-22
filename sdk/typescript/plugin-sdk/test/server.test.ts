import test from "node:test";
import assert from "node:assert/strict";
import { PassThrough, Writable } from "node:stream";
import { ArchitectProcessPlugin, PluginServer } from "../src";
import { APP_VERSION, JSON_RPC_VERSION, METHODS } from "../src/protocol";

class CaptureWritable extends Writable {
  private readonly chunks: string[] = [];

  _write(chunk: Buffer | string, _encoding: BufferEncoding, callback: (error?: Error | null) => void): void {
    this.chunks.push(chunk.toString());
    callback();
  }

  lines(): string[] {
    return this.chunks.join("").trim().split("\n").filter(Boolean);
  }
}

test("PluginServer handles init and listTasks", async () => {
  const output = new CaptureWritable();
  const plugin: ArchitectProcessPlugin = {
    async init(config) {
      assert.equal(config.plugin, true);
      return { ok: true, name: "example", version: "1.0.0" };
    },
    async listTasks() {
      return [{ id: "build", description: "Build" }];
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
    params: { config: { plugin: true }, protocol_version: APP_VERSION },
  });
  await server.handleRequest({
    jsonrpc: JSON_RPC_VERSION,
    id: 2,
    method: METHODS.listTasks,
  });

  const lines = output.lines().map((line) => JSON.parse(line));
  assert.equal(lines[0].result.ok, true);
  assert.equal(lines[0].result.name, "example");
  assert.deepEqual(lines[1].result, [{ id: "build", description: "Build" }]);
});

test("PluginServer acknowledges executeTask and streams events", async () => {
  const output = new CaptureWritable();
  const plugin: ArchitectProcessPlugin = {
    listTasks() {
      return [];
    },
    async executeTask(_request, writer) {
      await writer.output("starting");
      await writer.progress(0.5);
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
    id: 3,
    method: METHODS.executeTask,
    params: {
      id: "build",
      args: ["--scan"],
      env: { ARCHITECT_PROFILE: "default" },
    },
  });

  const lines = output.lines().map((line) => JSON.parse(line));
  assert.equal(lines[0].id, 3);
  assert.equal(lines[0].result, null);
  assert.equal(lines[1].type, "output");
  assert.equal(lines[1].data, "starting");
  assert.equal(lines[2].type, "progress");
  assert.equal(lines[2].progress, 0.5);
  assert.equal(lines[3].type, "completed");
  assert.equal(lines[3].exitCode, 0);
});