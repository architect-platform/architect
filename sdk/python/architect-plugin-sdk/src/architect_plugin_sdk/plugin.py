from __future__ import annotations

from typing import Any, Protocol, runtime_checkable

from .models import ExecuteTaskParams, InitResult, TaskDescriptor


@runtime_checkable
class PluginEventWriter(Protocol):
    def output(self, text: str) -> None: ...

    def error(self, text: str) -> None: ...

    def progress(self, value: float) -> None: ...


@runtime_checkable
class ArchitectPlugin(Protocol):
    def list_tasks(self) -> list[TaskDescriptor]: ...

    def execute_task(self, request: ExecuteTaskParams, writer: PluginEventWriter) -> int | None: ...


@runtime_checkable
class InitializablePlugin(Protocol):
    def init(self, config: dict[str, Any]) -> InitResult | None: ...