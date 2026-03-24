from __future__ import annotations

import io
import json
import os
import unittest

from architect_plugin_sdk import (
    APP_VERSION,
    JSON_RPC_VERSION,
    PluginServer,
    TaskDescriptor,
    InitResult,
)
from architect_plugin_sdk.protocol import (
    METHOD_INIT,
    METHOD_LIST_TASKS,
    METHOD_EXECUTE_TASK,
    METHOD_SHUTDOWN,
    EVENT_OUTPUT,
    EVENT_PROGRESS,
    EVENT_ERROR,
    EVENT_COMPLETED,
    ERROR_INVALID,
)

FIXTURES_PATH = os.path.join(
    os.path.dirname(__file__),
    "..",
    "..",
    "..",
    "..",
    "docs",
    "protocol-fixtures.json",
)


def _load_fixtures() -> dict:
    with open(FIXTURES_PATH) as f:
        return json.load(f)


class FixturePlugin:
    """Plugin that returns responses matching the shared fixture data."""

    def init(self, config: dict[str, object]):
        return InitResult(ok=True, name="test-plugin", version="1.0.0")

    def list_tasks(self):
        return [
            TaskDescriptor(
                id="build",
                description="Build the project",
                phase="build",
                dependencies=["init"],
                permissions=["process:exec"],
                requires_confirmation=False,
            ),
            TaskDescriptor(
                id="deploy",
                description="Deploy to production",
                phase="release",
                dependencies=["build"],
                permissions=["process:exec", "network:outbound"],
                requires_confirmation=True,
            ),
        ]

    def execute_task(self, request, writer):
        writer.output("Compiling sources...")
        writer.progress(0.5)
        return 0


class ProtocolCompatibilityTest(unittest.TestCase):
    def setUp(self) -> None:
        self.fixtures = _load_fixtures()

    def test_protocol_constants_match_fixtures(self) -> None:
        self.assertEqual(JSON_RPC_VERSION, self.fixtures["json_rpc_version"])
        self.assertEqual(APP_VERSION, self.fixtures["protocol_version"])
        self.assertEqual(METHOD_INIT, "init")
        self.assertEqual(METHOD_LIST_TASKS, "listTasks")
        self.assertEqual(METHOD_EXECUTE_TASK, "executeTask")
        self.assertEqual(METHOD_SHUTDOWN, "shutdown")
        self.assertEqual(EVENT_OUTPUT, "output")
        self.assertEqual(EVENT_PROGRESS, "progress")
        self.assertEqual(EVENT_ERROR, "error")
        self.assertEqual(EVENT_COMPLETED, "completed")
        self.assertEqual(ERROR_INVALID, -32600)

    def test_server_processes_fixture_init_and_list_tasks(self) -> None:
        output = io.StringIO()
        server = PluginServer(FixturePlugin(), input_stream=io.StringIO(), output_stream=output)

        server.handle_request(self.fixtures["requests"]["init"])
        server.handle_request(self.fixtures["requests"]["listTasks"])

        lines = [json.loads(line) for line in output.getvalue().strip().splitlines()]

        # Init response
        self.assertEqual(lines[0]["jsonrpc"], "2.0")
        self.assertEqual(lines[0]["id"], self.fixtures["responses"]["init_success"]["id"])
        self.assertEqual(lines[0]["result"]["ok"], True)
        self.assertEqual(lines[0]["result"]["name"], "test-plugin")

        # ListTasks response
        self.assertEqual(lines[1]["id"], self.fixtures["responses"]["listTasks_success"]["id"])
        self.assertEqual(len(lines[1]["result"]), 2)
        self.assertEqual(lines[1]["result"][0]["id"], "build")
        self.assertEqual(lines[1]["result"][1]["requires_confirmation"], True)

    def test_server_streams_fixture_compatible_events(self) -> None:
        output = io.StringIO()
        server = PluginServer(FixturePlugin(), input_stream=io.StringIO(), output_stream=output)

        server.handle_request(self.fixtures["requests"]["executeTask"])

        lines = [json.loads(line) for line in output.getvalue().strip().splitlines()]

        # Ack
        self.assertEqual(lines[0]["id"], self.fixtures["responses"]["executeTask_ack"]["id"])
        self.assertIsNone(lines[0]["result"])

        # Output event
        self.assertEqual(lines[1]["type"], self.fixtures["events"]["output"]["type"])
        self.assertEqual(lines[1]["data"], self.fixtures["events"]["output"]["data"])

        # Progress event
        self.assertEqual(lines[2]["type"], self.fixtures["events"]["progress"]["type"])
        self.assertEqual(lines[2]["progress"], self.fixtures["events"]["progress"]["progress"])

        # Completed event
        self.assertEqual(lines[3]["type"], self.fixtures["events"]["completed_success"]["type"])
        self.assertEqual(lines[3]["exitCode"], self.fixtures["events"]["completed_success"]["exitCode"])

    def test_server_rejects_unsupported_protocol_version(self) -> None:
        output = io.StringIO()
        server = PluginServer(FixturePlugin(), input_stream=io.StringIO(), output_stream=output)

        server.handle_request(
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "init",
                "params": {"config": {}, "protocol_version": "2.0.0"},
            }
        )

        lines = [json.loads(line) for line in output.getvalue().strip().splitlines()]
        self.assertIn("error", lines[0])
        self.assertEqual(
            lines[0]["error"]["code"],
            self.fixtures["responses"]["protocol_version_error"]["error"]["code"],
        )


if __name__ == "__main__":
    unittest.main()
