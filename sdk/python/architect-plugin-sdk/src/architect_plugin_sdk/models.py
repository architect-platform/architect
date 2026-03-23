from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(slots=True)
class InitResult:
    ok: bool = True
    name: str | None = None
    version: str | None = None


@dataclass(slots=True)
class TaskDescriptor:
    id: str
    description: str | None = None
    phase: str | None = None
    dependencies: list[str] | None = None
    permissions: list[str] | None = None
    requires_confirmation: bool = False


@dataclass(slots=True)
class ExecuteTaskParams:
    id: str
    args: list[str] | None = None
    env: dict[str, str] | None = None


@dataclass(slots=True)
class TaskEvent:
    type: str
    data: str | None = None
    progress: float | None = None
    exitCode: int | None = None


@dataclass(slots=True)
class JsonRpcError:
    code: int
    message: str
    data: Any = None