from __future__ import annotations

import json
import sys
from dataclasses import asdict, is_dataclass
from typing import Any, TextIO

from .models import ExecuteTaskParams, InitResult, JsonRpcError, TaskDescriptor, TaskEvent
from .plugin import ArchitectPlugin, InitializablePlugin
from .protocol import (
    APP_VERSION,
    ERROR_INTERNAL,
    ERROR_INVALID,
    ERROR_METHOD_NOT_FOUND,
    ERROR_PARSE,
    EVENT_COMPLETED,
    EVENT_ERROR,
    EVENT_OUTPUT,
    EVENT_PROGRESS,
    JSON_RPC_VERSION,
    METHOD_EXECUTE_TASK,
    METHOD_INIT,
    METHOD_LIST_TASKS,
    METHOD_SHUTDOWN,
)


def _jsonable(value: Any) -> Any:
    if is_dataclass(value):
        return {key: _jsonable(item) for key, item in asdict(value).items() if item is not None}
    if isinstance(value, list):
        return [_jsonable(item) for item in value]
    if isinstance(value, dict):
        return {key: _jsonable(item) for key, item in value.items() if item is not None}
    return value


class _EventWriter:
    def __init__(self, server: "PluginServer") -> None:
        self._server = server

    def output(self, text: str) -> None:
        self._server.write_event(TaskEvent(type=EVENT_OUTPUT, data=text))

    def error(self, text: str) -> None:
        self._server.write_event(TaskEvent(type=EVENT_ERROR, data=text))

    def progress(self, value: float) -> None:
        self._server.write_event(TaskEvent(type=EVENT_PROGRESS, progress=value))


class PluginServer:
    def __init__(
        self,
        plugin: ArchitectPlugin,
        input_stream: TextIO | None = None,
        output_stream: TextIO | None = None,
        exit_handler: callable | None = None,
    ) -> None:
        self.plugin = plugin
        self.input_stream = input_stream or sys.stdin
        self.output_stream = output_stream or sys.stdout
        self.exit_handler = exit_handler or (lambda code=0: None)
        self.shutting_down = False

    def serve(self) -> None:
        for line in self.input_stream:
            line = line.strip()
            if not line:
                continue
            self.handle_line(line)
            if self.shutting_down:
                return

    def handle_line(self, line: str) -> None:
        try:
            request = json.loads(line)
        except json.JSONDecodeError:
            self.write_response(error=JsonRpcError(ERROR_PARSE, "Invalid JSON-RPC payload"), request_id=None)
            return

        self.handle_request(request)

    def handle_request(self, request: dict[str, Any]) -> None:
        method = request.get("method")
        request_id = request.get("id")

        if request.get("jsonrpc") != JSON_RPC_VERSION or not isinstance(method, str):
            self.write_response(error=JsonRpcError(ERROR_INVALID, "Invalid JSON-RPC request"), request_id=request_id)
            return

        if method == METHOD_INIT:
            self._handle_init(request_id, request.get("params") or {})
        elif method == METHOD_LIST_TASKS:
            self._handle_list_tasks(request_id)
        elif method == METHOD_EXECUTE_TASK:
            self._handle_execute_task(request_id, request.get("params") or {})
        elif method == METHOD_SHUTDOWN:
            self._handle_shutdown(request_id)
        else:
            self.write_response(
                error=JsonRpcError(ERROR_METHOD_NOT_FOUND, f"Unknown method: {method}"),
                request_id=request_id,
            )

    def _handle_init(self, request_id: int | None, params: dict[str, Any]) -> None:
        if params.get("protocol_version") != APP_VERSION:
            self.write_response(
                error=JsonRpcError(ERROR_INVALID, f"Unsupported protocol version: {params.get('protocol_version')!s}"),
                request_id=request_id,
            )
            return

        result = InitResult(ok=True)
        if isinstance(self.plugin, InitializablePlugin):
            plugin_result = self.plugin.init(params.get("config") or {})
            if plugin_result is not None:
                result = plugin_result
                result.ok = True

        self.write_response(result=result, request_id=request_id)

    def _handle_list_tasks(self, request_id: int | None) -> None:
        try:
            tasks = self.plugin.list_tasks()
            self.write_response(result=tasks, request_id=request_id)
        except Exception as error:  # pragma: no cover - mirrored in execute handler test path
            self.write_response(error=JsonRpcError(ERROR_INTERNAL, str(error)), request_id=request_id)

    def _handle_execute_task(self, request_id: int | None, params: dict[str, Any]) -> None:
        request = ExecuteTaskParams(
            id=params.get("id", ""),
            args=params.get("args"),
            env=params.get("env"),
        )
        if not request.id:
            self.write_response(error=JsonRpcError(ERROR_INVALID, "executeTask requires an id"), request_id=request_id)
            return

        self.write_response(result=None, request_id=request_id)
        writer = _EventWriter(self)

        try:
            exit_code = self.plugin.execute_task(request, writer)
            self.write_event(TaskEvent(type=EVENT_COMPLETED, exitCode=0 if exit_code is None else exit_code))
        except Exception as error:
            writer.error(str(error))
            self.write_event(TaskEvent(type=EVENT_COMPLETED, exitCode=1))

    def _handle_shutdown(self, request_id: int | None) -> None:
        self.shutting_down = True
        self.write_response(result=None, request_id=request_id)
        self.exit_handler(0)

    def write_response(self, *, result: Any = None, error: JsonRpcError | None = None, request_id: int | None) -> None:
        payload = {
            "jsonrpc": JSON_RPC_VERSION,
            "id": request_id,
        }
        if error is not None:
            payload["error"] = _jsonable(error)
        else:
            payload["result"] = _jsonable(result)
        self._write_line(payload)

    def write_event(self, event: TaskEvent) -> None:
        self._write_line(_jsonable(event))

    def _write_line(self, payload: dict[str, Any]) -> None:
        self.output_stream.write(json.dumps(payload) + "\n")
        self.output_stream.flush()


def run_plugin(plugin: ArchitectPlugin) -> None:
    PluginServer(plugin).serve()