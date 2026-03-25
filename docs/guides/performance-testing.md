# Performance Testing Standards

This document defines when and how performance testing should be triggered, and who owns it.

## Scope

Performance testing applies to components that handle concurrent load or have latency-sensitive paths:

| Component        | Performance-Sensitive Paths                          |
|------------------|------------------------------------------------------|
| architect-engine | Task execution, SSE streaming, plugin loading        |
| architect-cloud  | API response times, WebSocket throughput, DB queries |
| architect-core   | Dependency resolution, cache key computation         |

CLI, API library, and plugins are **not** performance-tested (single-user, single-execution).

## Testing Triggers

### Mandatory (Must Run Before Merge)

| Trigger                                           | Test Type       |
|---------------------------------------------------|-----------------|
| Changes to `TaskExecutor` or `TaskDependencyResolver` | Execution benchmark |
| Changes to SSE streaming or event publishing      | Throughput test   |
| Changes to plugin loading or classloader isolation | Startup benchmark |
| Changes to cache key computation                  | Hash throughput   |
| Database schema changes (cloud)                   | Query benchmark   |

### Recommended (Run Periodically)

| Trigger              | Frequency  | Test Type                  |
|----------------------|------------|----------------------------|
| Pre-release          | Per release| Full benchmark suite       |
| Weekly CI            | Weekly     | Regression detection       |
| Dependency upgrades  | Per upgrade| Before/after comparison    |

## Benchmark Categories

### 1. Task Execution Throughput

Measure time to execute N independent tasks with varying parallelism.

**Baseline targets** (on reference hardware):
- 100 no-op tasks, sequential: < 2 seconds
- 100 no-op tasks, parallel (4 threads): < 1 second
- Dependency resolution for 100-task DAG: < 100ms

### 2. SSE Streaming Latency

Measure time from task event emission to SSE client receipt.

**Baseline targets**:
- Event-to-client latency: < 50ms (p99)
- Concurrent SSE connections: support 50+ without degradation
- Event throughput: 1000 events/second sustained

### 3. Startup Time

Measure time from process start to ready state.

**Baseline targets**:
- Engine cold start (no plugins): < 3 seconds
- Engine with 10 plugins: < 8 seconds
- Cloud backend startup: < 5 seconds

### 4. Cache Performance

Measure cache key computation and lookup times.

**Baseline targets**:
- Cache key computation (10 files, 100KB total): < 50ms
- Cache lookup (in-memory): < 1ms
- Cache lookup (local disk): < 10ms

## Ownership

| Area                      | Owner                  | Responsibility                    |
|---------------------------|------------------------|-----------------------------------|
| Engine benchmarks         | Engine team            | Maintain and run execution/SSE tests |
| Cloud benchmarks          | Cloud team             | Maintain and run API/DB tests     |
| Core benchmarks           | Core team              | Maintain dependency/cache tests   |
| Benchmark infrastructure  | Platform team          | CI integration, result storage    |
| Regression detection      | PR author + reviewer   | Compare before/after on triggers  |

## Implementation Notes

### Current Status

Performance benchmarks are **not yet implemented**. When added:

1. Use JMH (Java Microbenchmark Harness) for Kotlin microbenchmarks
2. Place benchmarks in `src/jmh/kotlin/` directories (Gradle JMH plugin)
3. Store baseline results in `benchmarks/` directory as JSON
4. CI compares current run against stored baselines; flag >10% regressions

### Anti-Patterns

| ❌ Don't                                         | ✅ Do Instead                                      |
|--------------------------------------------------|----------------------------------------------------|
| Benchmark in CI without warm-up                  | Use JMH with proper warm-up iterations             |
| Compare benchmarks across different hardware     | Run before/after on same CI runner                 |
| Ignore variance in results                       | Require >10% sustained regression to flag          |
| Performance test everything                      | Focus on the critical paths listed above           |
