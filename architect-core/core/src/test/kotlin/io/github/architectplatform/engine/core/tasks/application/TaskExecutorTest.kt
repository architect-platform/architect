package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.api.core.project.Config
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.CompositeTask
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.impl.SimpleTask
import io.github.architectplatform.engine.core.events.EmbeddedEventBus
import io.github.architectplatform.engine.core.project.app.ApplicationEnvironment
import io.github.architectplatform.engine.core.project.domain.Project
import io.github.architectplatform.engine.core.tasks.infrastructure.InMemoryTaskRegistry
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList

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
