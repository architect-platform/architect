from __future__ import annotations

import io
import json
import unittest

from architect_plugin_sdk import APP_VERSION, PluginServer, TaskDescriptor


class ExamplePlugin:
    def init(self, config: dict[str, object]):
        if config.get("plugin") is True:
            from architect_plugin_sdk import InitResult

            return InitResult(ok=True, name="example", version="1.0.0")
        return None

    def list_tasks(self):
        return [TaskDescriptor(id="build", description="Build")]

    def execute_task(self, request, writer):
        writer.output("starting")
        writer.progress(0.5)
        return 0


class PluginServerTest(unittest.TestCase):
    def test_init_and_list_tasks(self) -> None:
        output = io.StringIO()
        server = PluginServer(ExamplePlugin(), input_stream=io.StringIO(), output_stream=output)

        server.handle_request(
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "init",
                "params": {"config": {"plugin": True}, "protocol_version": APP_VERSION},
            }
        )
        server.handle_request({"jsonrpc": "2.0", "id": 2, "method": "listTasks"})

        lines = [json.loads(line) for line in output.getvalue().strip().splitlines()]
        self.assertEqual(lines[0]["result"]["name"], "example")
        self.assertEqual(lines[1]["result"][0]["id"], "build")

    def test_execute_task_ack_and_events(self) -> None:
        output = io.StringIO()
        server = PluginServer(ExamplePlugin(), input_stream=io.StringIO(), output_stream=output)

        server.handle_request(
            {
                "jsonrpc": "2.0",
                "id": 3,
                "method": "executeTask",
                "params": {
                    "id": "build",
                    "args": ["--scan"],
                    "env": {"ARCHITECT_PROFILE": "default"},
                },
            }
        )

        lines = [json.loads(line) for line in output.getvalue().strip().splitlines()]
        self.assertEqual(lines[0]["id"], 3)
        self.assertIsNone(lines[0]["result"])
        self.assertEqual(lines[1]["type"], "output")
        self.assertEqual(lines[1]["data"], "starting")
        self.assertEqual(lines[2]["type"], "progress")
        self.assertEqual(lines[2]["progress"], 0.5)
        self.assertEqual(lines[3]["type"], "completed")
        self.assertEqual(lines[3]["exitCode"], 0)


if __name__ == "__main__":
    unittest.main()