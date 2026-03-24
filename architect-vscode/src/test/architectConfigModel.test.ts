import test from "node:test";
import assert from "node:assert/strict";
import { parseArchitectConfig } from "../architectConfigModel";

test("parseArchitectConfig extracts inline tasks with metadata", () => {
  const config = `
tasks:
  build-app:
    description: Build the application
    phase: BUILD
  smoke-test:
    description: Run smoke tests
`;

  const model = parseArchitectConfig(config);

  assert.deepEqual(model.tasks, [
    {
      id: "build-app",
      description: "Build the application",
      phase: "BUILD",
      source: "tasks",
    },
    {
      id: "smoke-test",
      description: "Run smoke tests",
      phase: undefined,
      source: "tasks",
    },
  ]);
});

test("parseArchitectConfig extracts scripts plugin tasks through the shared model", () => {
  const config = `
scripts:
  enabled: true
  scripts:
    hello:
      command: echo hello
      description: Prints a greeting
    publish-site:
      command: echo publish
      description: Publishes docs
      phase: PUBLISH
`;

  const model = parseArchitectConfig(config);

  assert.deepEqual(model.tasks, [
    {
      id: "hello",
      description: "Prints a greeting",
      phase: undefined,
      source: "scripts",
    },
    {
      id: "publish-site",
      description: "Publishes docs",
      phase: "PUBLISH",
      source: "scripts",
    },
  ]);
});

test("parseArchitectConfig rejects malformed yaml", () => {
  assert.throws(() => parseArchitectConfig("tasks:\n  broken: ["), /Flow sequence/);
});
