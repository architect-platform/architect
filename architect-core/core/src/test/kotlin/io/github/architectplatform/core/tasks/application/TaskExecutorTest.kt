package io.github.architectplatform.core.tasks.application

import io.github.architectplatform.api.core.project.Config
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.CompositeTask
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.FailureStrategy
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskPermission
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.builtin.SimpleTask
import io.github.architectplatform.core.execution.TaskPermissionScope
import io.github.architectplatform.core.events.EmbeddedEventBus
import io.github.architectplatform.core.project.app.ApplicationEnvironment
import io.github.architectplatform.core.project.domain.Project
import io.github.architectplatform.core.tasks.infrastructure.InMemoryTaskRegistry
import io.github.architectplatform.core.domain.events.ArchitectEvent
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/** Creates an anonymous Task that overrides [onFailure] with [FailureStrategy.RETRY]. */
private fun retryTask(
  id: String,
  maxAttempts: Int,
  backoffMs: Long = 0L,
  exponential: Boolean = false,
  jitter: Boolean = false,
  block: (Environment, ProjectContext) -> TaskResult,
): Task = object : Task {
  override val id = id
  override fun description() = id
  override fun execute(environment: Environment, projectContext: ProjectContext, args: List<String>) = block(environment, projectContext)
  override fun onFailure() = FailureStrategy.RETRY(maxAttempts, backoffMs, exponential, jitter)
}

/**
 * Tests for [TaskExecutor] — parallel batch execution, sequential fallback,
 * failure propagation, and child task execution.
 */
class TaskExecutorTest {

  @Test
  fun `executes single task successfully`(@TempDir tmpDir: Path) {
    val (executor, events) = buildExecutor(parallel = true)
    val registry = InMemoryTaskRegistry()
    val task = SimpleTask("build", "Compile") { _, _ -> TaskResult.success("compiled") }
    registry.add(task)

    val project = project(tmpDir, registry)
    val (_, deferred) = executor.execute(project, task, project.context, emptyList())

    val result = runBlocking { deferred.await() }
    assertTrue(result.success)
    assertTrue(events.any { it.toString().contains("task.started") })
    assertTrue(events.any { it.toString().contains("task.completed") })
  }

  @Test
  fun `single skipped task preserves skipped status`(@TempDir tmpDir: Path) {
    val (executor, _) = buildExecutor(parallel = true)
    val registry = InMemoryTaskRegistry()
    val task = SimpleTask(
      id = "conditional",
      description = "Conditional task",
      runtimeCondition = { _, _ -> false },
    ) { _, _ ->
      TaskResult.success("should not execute")
    }
    registry.add(task)

    val project = project(tmpDir, registry)
    val (_, deferred) = executor.execute(project, task, project.context, emptyList())

    val result = runBlocking { deferred.await() }
    assertTrue(result.success)
    assertEquals(TaskResult.Status.SKIPPED, result.status)
  }

  @Test
  fun `executes parallel batch with independent tasks`(@TempDir tmpDir: Path) {
    val threadNames = CopyOnWriteArrayList<String>()
    val (executor, _) = buildExecutor(parallel = true)
    val registry = InMemoryTaskRegistry()

    // Two independent tasks (no dependencies) — should run in parallel batch 0
    registry.add(SimpleTask("lint", "Lint") { _, _ ->
      threadNames.add(Thread.currentThread().name)
      Thread.sleep(50)
      TaskResult.success("linted")
    })
    registry.add(SimpleTask("format", "Format") { _, _ ->
      threadNames.add(Thread.currentThread().name)
      Thread.sleep(50)
      TaskResult.success("formatted")
    })
    // A dependent task that depends on both — batch 1
    registry.add(SimpleTask("build", "Build", customDependencies = listOf("lint", "format")) { _, _ ->
      TaskResult.success("built")
    })

    val project = project(tmpDir, registry)
    val buildTask = registry.get("build")!!
    val (_, deferred) = executor.execute(project, buildTask, project.context, emptyList())

    val result = runBlocking { deferred.await() }
    assertTrue(result.success, "Expected success but got: ${result.message}")
  }

  @Test
  fun `sequential fallback executes tasks one at a time`(@TempDir tmpDir: Path) {
    val executionOrder = CopyOnWriteArrayList<String>()
    val (executor, _) = buildExecutor(parallel = false)
    val registry = InMemoryTaskRegistry()

    registry.add(SimpleTask("a", "Task A") { _, _ ->
      executionOrder.add("a")
      TaskResult.success("a done")
    })
    registry.add(SimpleTask("b", "Task B") { _, _ ->
      executionOrder.add("b")
      TaskResult.success("b done")
    })
    registry.add(SimpleTask("c", "Task C", customDependencies = listOf("a", "b")) { _, _ ->
      executionOrder.add("c")
      TaskResult.success("c done")
    })

    val project = project(tmpDir, registry)
    val (_, deferred) = executor.execute(project, registry.get("c")!!, project.context, emptyList())

    val result = runBlocking { deferred.await() }
    assertTrue(result.success)
    // c must come after a and b
    assertTrue(executionOrder.indexOf("c") > executionOrder.indexOf("a"))
    assertTrue(executionOrder.indexOf("c") > executionOrder.indexOf("b"))
  }

  @Test
  fun `failure in batch stops execution of remaining batches`(@TempDir tmpDir: Path) {
    val (executor, _) = buildExecutor(parallel = true)
    val registry = InMemoryTaskRegistry()

    registry.add(SimpleTask("fail-early", "Fails") { _, _ ->
      TaskResult.failure("boom")
    })
    registry.add(SimpleTask("after-fail", "Should not run", customDependencies = listOf("fail-early")) { _, _ ->
      TaskResult.success("should not reach here")
    })

    val project = project(tmpDir, registry)
    val (_, deferred) = executor.execute(project, registry.get("after-fail")!!, project.context, emptyList())

    val result = runBlocking { deferred.await() }
    assertFalse(result.success)
    assertTrue(result.message!!.contains("failed"))
  }

  @Test
  fun `child tasks are executed as part of composite task`(@TempDir tmpDir: Path) {
    val executionOrder = CopyOnWriteArrayList<String>()
    val (executor, _) = buildExecutor(parallel = true)
    val registry = InMemoryTaskRegistry()

    registry.add(SimpleTask("child-a", "Child A") { _, _ ->
      executionOrder.add("child-a")
      TaskResult.success("child-a done")
    })
    registry.add(SimpleTask("child-b", "Child B") { _, _ ->
      executionOrder.add("child-b")
      TaskResult.success("child-b done")
    })
    val composite = CompositeTask(
      id = "parent",
      description = "Parent task",
      children = listOf("child-a", "child-b"),
    )
    registry.add(composite)

    val project = project(tmpDir, registry)
    val (_, deferred) = executor.execute(project, composite, project.context, emptyList())

    val result = runBlocking { deferred.await() }
    assertTrue(result.success, "Expected success but got: ${result.message}")
    assertTrue(executionOrder.contains("child-a"))
    assertTrue(executionOrder.contains("child-b"))
  }

  @Test
  fun `task exception is caught and returns failure`(@TempDir tmpDir: Path) {
    val (executor, _) = buildExecutor(parallel = true)
    val registry = InMemoryTaskRegistry()

    registry.add(SimpleTask("explode", "Throws") { _, _ ->
      throw RuntimeException("kaboom")
    })

    val project = project(tmpDir, registry)
    val (_, deferred) = executor.execute(project, registry.get("explode")!!, project.context, emptyList())

    val result = runBlocking { deferred.await() }
    assertFalse(result.success)
    assertTrue(result.message!!.contains("kaboom"))
  }

  @Test
  fun `task without process exec permission fails when launching command`(@TempDir tmpDir: Path) {
    val commandExecutor = object : io.github.architectplatform.api.components.execution.CommandExecutor {
      override fun execute(command: String, workingDir: String?) {
        TaskPermissionScope.current()?.let { context ->
          if (TaskPermission.PROCESS_EXEC !in context.permissions) {
            throw IllegalStateException("Task '${context.taskId}' requires permission 'process:exec' to launch subprocesses")
          }
        }
      }
    }
    val environment = ApplicationEnvironment(
      services = mapOf(io.github.architectplatform.api.components.execution.CommandExecutor::class.java to commandExecutor),
    )
    val eventBus = EmbeddedEventBus<ArchitectEvent<*>>()
    val executor = TaskExecutor(
      environment = environment,
      taskCache = TaskCache(cacheEnabled = false),
      eventBus = eventBus::invoke,
      parallelExecutionEnabled = true,
    )
    val registry = InMemoryTaskRegistry()
    val task = SimpleTask(
      id = "exec",
      description = "Execute command",
      permissions = setOf(TaskPermission.FILE_SYSTEM_READ),
    ) { env, _ ->
      env.service(io.github.architectplatform.api.components.execution.CommandExecutor::class.java).execute("echo hello")
      TaskResult.success()
    }
    registry.add(task)

    val project = project(tmpDir, registry)
    val (_, deferred) = executor.execute(project, task, project.context, emptyList())

    val result = runBlocking { deferred.await() }
    assertFalse(result.success)
    assertTrue(result.message!!.contains("process:exec"))
  }

  @Test
  fun `concurrency semaphore limits simultaneous task execution`(@TempDir tmpDir: Path) {
    val maxConcurrent = 2
    val runningCount = java.util.concurrent.atomic.AtomicInteger(0)
    val peakConcurrent = java.util.concurrent.atomic.AtomicInteger(0)
    val events = CopyOnWriteArrayList<ArchitectEvent<*>>()
    val eventBus = EmbeddedEventBus<ArchitectEvent<*>>()
    eventBus.subscribe { events.add(it) }
    val executor = TaskExecutor(
      environment = ApplicationEnvironment(),
      taskCache = TaskCache(cacheEnabled = false),
      eventBus = eventBus::invoke,
      parallelExecutionEnabled = true,
      maxConcurrentTasks = maxConcurrent,
    )
    val registry = InMemoryTaskRegistry()
    // 4 independent tasks each holding a slot for 50ms — with semaphore(2) only 2 run at once
    for (i in 1..4) {
      registry.add(SimpleTask("task-$i", "Task $i") { _, _ ->
        val current = runningCount.incrementAndGet()
        peakConcurrent.updateAndGet { max -> maxOf(max, current) }
        Thread.sleep(50)
        runningCount.decrementAndGet()
        TaskResult.success("done-$i")
      })
    }
    // A root task that depends on all 4 (so they run as a batch)
    registry.add(SimpleTask("root", "Root", customDependencies = (1..4).map { "task-$it" }) { _, _ -> TaskResult.success("root") })

    val project = project(tmpDir, registry)
    val (_, deferred) = executor.execute(project, registry.get("root")!!, project.context, emptyList())
    val result = runBlocking { deferred.await() }

    assertTrue(result.success, "Execution should succeed: ${result.message}")
    assertTrue(peakConcurrent.get() <= maxConcurrent, "Peak concurrent tasks ${peakConcurrent.get()} exceeded limit $maxConcurrent")
  }

  // ─── helpers ──────────────────────────────────────────────────────────────

  private fun buildExecutor(parallel: Boolean): Pair<TaskExecutor, MutableList<ArchitectEvent<*>>> {
    val events = CopyOnWriteArrayList<ArchitectEvent<*>>()
    val eventBus = EmbeddedEventBus<ArchitectEvent<*>>()
    eventBus.subscribe { events.add(it) }
    val environment = ApplicationEnvironment()
    val executor = TaskExecutor(
      environment = environment,
      taskCache = TaskCache(cacheEnabled = false),
      eventBus = eventBus::invoke,
      parallelExecutionEnabled = parallel,
    )
    return executor to events
  }

  private fun project(dir: Path, registry: InMemoryTaskRegistry): Project {
    val config: Config = mapOf("project" to mapOf("name" to "test-project"))
    val context = ProjectContext(dir = dir, config = config)
    return Project(
      name = "test-project",
      path = dir.toString(),
      context = context,
      plugins = emptyList(),
      taskRegistry = registry,
    )
  }
}

// ─── Retry logic tests (separate class for clarity) ──────────────────────────

class TaskExecutorRetryTest {

  @Test
  fun `task with RETRY strategy retries on failure and succeeds`(@TempDir tmpDir: Path) {
    val attempts = AtomicInteger(0)
    val (executor, events) = buildExecutor()
    val registry = InMemoryTaskRegistry()

    val task = retryTask("flaky", maxAttempts = 3) { _, _ ->
      if (attempts.incrementAndGet() < 3) TaskResult.failure("not yet") else TaskResult.success("ok")
    }
    registry.add(task)

    val project = project(tmpDir, registry)
    val (_, deferred) = executor.execute(project, task, project.context, emptyList())
    val result = runBlocking { deferred.await() }

    assertTrue(result.success, "Expected success after retries but got: ${result.message}")
    assertEquals(3, attempts.get(), "Expected exactly 3 attempts")
    val retryingEvents = events.filter { it.toString().contains("task.retrying") }
    assertEquals(2, retryingEvents.size, "Expected 2 retrying events (attempts 2 and 3)")
  }

  @Test
  fun `task with RETRY strategy fails after exhausting all attempts`(@TempDir tmpDir: Path) {
    val attempts = AtomicInteger(0)
    val (executor, events) = buildExecutor()
    val registry = InMemoryTaskRegistry()

    val task = retryTask("always-fail", maxAttempts = 2) { _, _ ->
      attempts.incrementAndGet()
      TaskResult.failure("always bad")
    }
    registry.add(task)

    val project = project(tmpDir, registry)
    val (_, deferred) = executor.execute(project, task, project.context, emptyList())
    val result = runBlocking { deferred.await() }

    assertFalse(result.success)
    assertEquals(3, attempts.get(), "Expected initial + 2 retry attempts = 3 total")
    val failEvents = events.filter { it.toString().contains("task.failed") }
    assertTrue(failEvents.isNotEmpty(), "Expected at least one task.failed event")
  }

  @Test
  fun `RETRY with no-delay completes quickly`(@TempDir tmpDir: Path) {
    val (executor, _) = buildExecutor()
    val registry = InMemoryTaskRegistry()
    val task = retryTask("quick-retry", maxAttempts = 2, backoffMs = 0L) { _, _ -> TaskResult.failure("x") }
    registry.add(task)
    val project = project(tmpDir, registry)
    val (_, deferred) = executor.execute(project, task, project.context, emptyList())
    val start = System.currentTimeMillis()
    val result = runBlocking { deferred.await() }
    val elapsed = System.currentTimeMillis() - start
    assertFalse(result.success)
    assertTrue(elapsed < 500, "No-delay retry should complete quickly, took ${elapsed}ms")
  }

  @Test
  fun `FailureStrategy RETRY computeDelayMs returns 0 when backoffMs is 0`() {
    val strategy = FailureStrategy.RETRY(maxAttempts = 3, backoffMs = 0L)
    assertEquals(0L, strategy.computeDelayMs(1))
    assertEquals(0L, strategy.computeDelayMs(2))
  }

  @Test
  fun `FailureStrategy RETRY computeDelayMs returns flat delay without exponential`() {
    val strategy = FailureStrategy.RETRY(maxAttempts = 3, backoffMs = 100L, exponential = false, jitter = false)
    assertEquals(100L, strategy.computeDelayMs(1))
    assertEquals(100L, strategy.computeDelayMs(2))
    assertEquals(100L, strategy.computeDelayMs(3))
  }

  @Test
  fun `FailureStrategy RETRY computeDelayMs doubles delay with exponential backoff`() {
    val strategy = FailureStrategy.RETRY(maxAttempts = 4, backoffMs = 100L, exponential = true, jitter = false)
    assertEquals(100L, strategy.computeDelayMs(1))  // 100 * 2^0 = 100
    assertEquals(200L, strategy.computeDelayMs(2))  // 100 * 2^1 = 200
    assertEquals(400L, strategy.computeDelayMs(3))  // 100 * 2^2 = 400
    assertEquals(800L, strategy.computeDelayMs(4))  // 100 * 2^3 = 800
  }

  @Test
  fun `FailureStrategy RETRY computeDelayMs adds jitter within 25 percent of base`() {
    val strategy = FailureStrategy.RETRY(maxAttempts = 1, backoffMs = 1000L, exponential = false, jitter = true)
    repeat(20) {
      val computed = strategy.computeDelayMs(1)
      assertTrue(computed in 750L..1250L, "Jittered delay $computed not in [750, 1250]")
    }
  }

  // ─── helpers ───────────────────────────────────────────────────────────────

  private fun buildExecutor(): Pair<TaskExecutor, MutableList<ArchitectEvent<*>>> {
    val events = CopyOnWriteArrayList<ArchitectEvent<*>>()
    val eventBus = EmbeddedEventBus<ArchitectEvent<*>>()
    eventBus.subscribe { events.add(it) }
    val executor = TaskExecutor(
      environment = ApplicationEnvironment(),
      taskCache = TaskCache(cacheEnabled = false),
      eventBus = eventBus::invoke,
      parallelExecutionEnabled = false,
    )
    return executor to events
  }

  private fun project(dir: Path, registry: InMemoryTaskRegistry): Project {
    val config: Config = mapOf("project" to mapOf("name" to "test-project"))
    return Project(
      name = "test-project",
      path = dir.toString(),
      context = ProjectContext(dir = dir, config = config),
      plugins = emptyList(),
      taskRegistry = registry,
    )
  }
}
