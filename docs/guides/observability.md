# Observability Standards

This document defines observability expectations for Architect Platform server and cloud surfaces.

## Scope

These standards apply to:
- **architect-engine** — the task execution server
- **architect-cloud** — the cloud backend (project registry, execution history, real-time events)

Core libraries and plugins are out of scope (they contribute via structured events, not direct observability).

## Three Pillars

### 1. Logging

Engine and cloud surfaces must produce structured, queryable logs.

| Concern              | Requirement                                                  |
|----------------------|--------------------------------------------------------------|
| Framework            | SLF4J with Logback (Micronaut default)                       |
| Format               | Structured JSON in production; human-readable in development |
| Request correlation  | Include `executionId` in all task-execution log entries       |
| Sensitive data       | Never log secrets, tokens, or full file contents             |
| Retention            | Application logs: at least 7 days in production              |

#### Mandatory Log Points

**Engine:**
- Server startup complete (info)
- Project registered/unregistered (info)
- Task execution started with executionId (info)
- Task execution completed with result status (info)
- Task execution failed with error details (error)
- SSE client connected/disconnected (debug)
- Plugin loaded/failed to load (info/error)

**Cloud:**
- API request received with route and method (debug)
- Authentication success/failure (info/warn)
- Database query failures (error)
- WebSocket connection lifecycle (debug)

### 2. Metrics

Metrics should be exposed for operational monitoring.

| Metric                        | Type      | Labels                    |
|-------------------------------|-----------|---------------------------|
| `task_executions_total`       | Counter   | project, task, status     |
| `task_execution_duration_ms`  | Histogram | project, task             |
| `active_sse_connections`      | Gauge     | —                         |
| `registered_projects`         | Gauge     | —                         |
| `plugin_load_failures_total`  | Counter   | plugin_id, error_type     |
| `http_request_duration_ms`    | Histogram | method, route, status     |

**Current status**: Metrics are not yet instrumented. When added, use Micrometer (Micronaut's built-in metrics support).

### 3. Health Checks

Both engine and cloud must expose health endpoints.

| Endpoint         | Purpose                              | Response       |
|------------------|--------------------------------------|----------------|
| `/health`        | Liveness — process is running        | 200 OK         |
| `/health/ready`  | Readiness — can accept requests      | 200 or 503     |

**Engine readiness** requires:
- At least one project registered, OR
- Server fully initialized (plugin loading complete)

**Cloud readiness** requires:
- Database connection active
- Required external services reachable

## Event-Driven Observability

The engine uses Server-Sent Events (SSE) for real-time execution streaming. This is the primary observability channel for task execution.

### Event Types

| Event Type         | Payload                                    | When Emitted              |
|--------------------|--------------------------------------------|---------------------------|
| `EXECUTION_START`  | executionId, taskId, projectName, timestamp | Task execution begins     |
| `TASK_OUTPUT`      | executionId, taskId, output line           | Task produces stdout      |
| `TASK_ERROR`       | executionId, taskId, error line            | Task produces stderr      |
| `TASK_COMPLETED`   | executionId, taskId, status, duration      | Individual task finishes  |
| `EXECUTION_END`    | executionId, status, totalDuration         | Full execution completes  |

### SSE Contract

- Endpoint: `GET /api/executions/{executionId}/events`
- Content-Type: `text/event-stream`
- Events are newline-delimited, prefixed with `data: `
- Connection closes after `EXECUTION_END` event
- Clients should handle reconnection for long-running executions

## Error Visibility

Errors must be visible through at least two channels:
1. **Logs** — full stack trace at `error` level
2. **Events** — structured error event via SSE (for CLI/UI consumers)

For cloud surfaces, errors must also be visible through:
3. **HTTP responses** — appropriate status codes with error body

### Error Response Format

```json
{
  "error": "TaskNotFoundException",
  "message": "Task 'deploy' not found in project 'my-app'",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Current Gaps

| Gap                           | Priority | Notes                                    |
|-------------------------------|----------|------------------------------------------|
| No Micrometer metrics         | Medium   | Add when scaling becomes a concern       |
| No structured JSON logging    | Medium   | Configure Logback for production profile |
| No request correlation IDs    | Low      | Add MDC-based correlation for debugging  |
| No distributed tracing        | Low      | Not needed until multi-service deployment|
