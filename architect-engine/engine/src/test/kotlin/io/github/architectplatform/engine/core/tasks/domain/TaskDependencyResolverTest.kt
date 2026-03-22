package io.github.architectplatform.engine.core.tasks.domain

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.engine.core.tasks.infrastructure.InMemoryTaskRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.time.Duration

/**
 * Unit tests for TaskDependencyResolver.
 */
class TaskDependencyResolverTest {

    private lateinit var resolver: TaskDependencyResolver
    private lateinit var registry: InMemoryTaskRegistry

    @BeforeEach
    fun setup() {
        resolver = TaskDependencyResolver()
        registry = InMemoryTaskRegistry()
    }

    @Test
    fun `should resolve task with no dependencies`() {
        // Given
        val task = TestTask("task-a")
        registry.add(task)

        // When
        val resolved = resolver.resolveAllDependencies(task, registry)

        // Then
        assertEquals(1, resolved.size)
        assertTrue(resolved.containsKey("task-a"))
    }

    @Test
    fun `should resolve task with single dependency`() {
        // Given
        val taskA = TestTask("task-a")
        val taskB = TestTask("task-b", listOf("task-a"))
        registry.add(taskA)
        registry.add(taskB)

        // When
        val resolved = resolver.resolveAllDependencies(taskB, registry)

        // Then
        assertEquals(2, resolved.size)
        assertTrue(resolved.containsKey("task-a"))
        assertTrue(resolved.containsKey("task-b"))
    }

    @Test
    fun `should resolve task with transitive dependencies`() {
        // Given
        val taskA = TestTask("task-a")
        val taskB = TestTask("task-b", listOf("task-a"))
        val taskC = TestTask("task-c", listOf("task-b"))
        registry.add(taskA)
        registry.add(taskB)
        registry.add(taskC)

        // When
        val resolved = resolver.resolveAllDependencies(taskC, registry)

        // Then
        assertEquals(3, resolved.size)
        assertTrue(resolved.containsKey("task-a"))
        assertTrue(resolved.containsKey("task-b"))
        assertTrue(resolved.containsKey("task-c"))
    }

    @Test
    fun `should resolve child tasks and their dependencies`() {
        // Given
        val setup = TestTask("setup")
        val child = TestTask("child", dependencies = listOf("setup"))
        val parent = TestTask("parent", children = listOf("child"))
        registry.add(setup)
        registry.add(child)
        registry.add(parent)

        // When
        val resolved = resolver.resolveAllDependencies(parent, registry)

        // Then
        assertEquals(setOf("setup", "child", "parent"), resolved.keys)
    }

    @Test
    fun `should throw exception when dependency not found`() {
        // Given
        val taskB = TestTask("task-b", listOf("task-a"))
        registry.add(taskB)

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            resolver.resolveAllDependencies(taskB, registry)
        }
    }

    @Test
    fun `should topologically sort tasks with no dependencies`() {
        // Given
        val taskA = TestTask("task-a")
        val tasks = mapOf("task-a" to taskA)

        // When
        val sorted = resolver.topologicalSort(tasks)

        // Then
        assertEquals(1, sorted.size)
        assertEquals("task-a", sorted[0].id)
    }

    @Test
    fun `should topologically sort tasks with dependencies`() {
        // Given
        val taskA = TestTask("task-a")
        val taskB = TestTask("task-b", listOf("task-a"))
        val tasks = mapOf("task-a" to taskA, "task-b" to taskB)

        // When
        val sorted = resolver.topologicalSort(tasks)

        // Then
        assertEquals(2, sorted.size)
        assertEquals("task-a", sorted[0].id)
        assertEquals("task-b", sorted[1].id)
    }

    @Test
    fun `should topologically sort complex dependency tree`() {
        // Given
        val taskA = TestTask("task-a")
        val taskB = TestTask("task-b")
        val taskC = TestTask("task-c", listOf("task-a", "task-b"))
        val taskD = TestTask("task-d", listOf("task-c"))
        val tasks = mapOf(
            "task-a" to taskA,
            "task-b" to taskB,
            "task-c" to taskC,
            "task-d" to taskD
        )

        // When
        val sorted = resolver.topologicalSort(tasks)

        // Then
        assertEquals(4, sorted.size)
        val taskAIndex = sorted.indexOfFirst { it.id == "task-a" }
        val taskBIndex = sorted.indexOfFirst { it.id == "task-b" }
        val taskCIndex = sorted.indexOfFirst { it.id == "task-c" }
        val taskDIndex = sorted.indexOfFirst { it.id == "task-d" }

        // Verify order: A and B before C, C before D
        assertTrue(taskAIndex < taskCIndex)
        assertTrue(taskBIndex < taskCIndex)
        assertTrue(taskCIndex < taskDIndex)
    }

    @Test
    fun `should detect circular dependency`() {
        // Given
        val taskA = TestTask("task-a", listOf("task-b"))
        val taskB = TestTask("task-b", listOf("task-a"))
        val tasks = mapOf("task-a" to taskA, "task-b" to taskB)

        // When & Then
        val exception = assertThrows(IllegalStateException::class.java) {
            resolver.topologicalSort(tasks)
        }
        assertTrue(exception.message!!.contains("Circular dependency"))
    }

    @Test
    fun `should handle diamond dependency pattern`() {
        // Given
        //     A
        //    / \
        //   B   C
        //    \ /
        //     D
        val taskA = TestTask("task-a")
        val taskB = TestTask("task-b", listOf("task-a"))
        val taskC = TestTask("task-c", listOf("task-a"))
        val taskD = TestTask("task-d", listOf("task-b", "task-c"))
        val tasks = mapOf(
            "task-a" to taskA,
            "task-b" to taskB,
            "task-c" to taskC,
            "task-d" to taskD
        )

        // When
        val sorted = resolver.topologicalSort(tasks)

        // Then
        assertEquals(4, sorted.size)
        val taskAIndex = sorted.indexOfFirst { it.id == "task-a" }
        val taskBIndex = sorted.indexOfFirst { it.id == "task-b" }
        val taskCIndex = sorted.indexOfFirst { it.id == "task-c" }
        val taskDIndex = sorted.indexOfFirst { it.id == "task-d" }

        // Verify A comes before B and C, both B and C come before D
        assertTrue(taskAIndex < taskBIndex)
        assertTrue(taskAIndex < taskCIndex)
        assertTrue(taskBIndex < taskDIndex)
        assertTrue(taskCIndex < taskDIndex)
    }

    @Test
    fun `should assign parallel batches for diamond dependency pattern`() {
        // Given
        val taskA = TestTask("task-a")
        val taskB = TestTask("task-b", dependencies = listOf("task-a"))
        val taskC = TestTask("task-c", dependencies = listOf("task-a"))
        val taskD = TestTask("task-d", dependencies = listOf("task-b", "task-c"))
        val orderedTasks = resolver.topologicalSort(
            mapOf(
                "task-a" to taskA,
                "task-b" to taskB,
                "task-c" to taskC,
                "task-d" to taskD,
            )
        )

        // When
        val batches = resolver.toBatches(orderedTasks)

        // Then
        assertEquals(0, batches["task-a"])
        assertEquals(1, batches["task-b"])
        assertEquals(1, batches["task-c"])
        assertEquals(2, batches["task-d"])
    }

    @Test
    fun `should resolve composite children in dependency order`() {
        // Given
        val prepare = TestTask("prepare")
        val compile = TestTask("compile", dependencies = listOf("prepare"))
        val packageTask = TestTask("package", dependencies = listOf("compile"))
        val composite = TestTask("build", children = listOf("package", "compile"))
        registry.add(prepare)
        registry.add(compile)
        registry.add(packageTask)
        registry.add(composite)

        // When
        val children = resolver.resolveChildren(composite, registry)

        // Then
        assertEquals(listOf("prepare", "compile", "package"), children.map { it.id })
    }

    @Test
    fun `should topologically sort large dependency graph within bounded time`() {
        val tasks = buildLargeTaskGraph(size = 1_000)

        val sorted = assertTimeoutPreemptively(Duration.ofSeconds(1)) {
            resolver.topologicalSort(tasks)
        }

        assertEquals(1_000, sorted.size)
        assertEquals("task-0", sorted.first().id)
        assertEquals("task-999", sorted.last().id)
    }

    /**
     * Test implementation of Task.
     */
    class TestTask(
        override val id: String,
        private val dependencies: List<String> = emptyList(),
        private val children: List<String> = emptyList(),
    ) : Task {
        override fun description(): String = "Test task $id"
        
        override fun depends(): List<String> = dependencies

        override fun children(): List<String> = children
        
        override fun execute(
            environment: Environment,
            projectContext: ProjectContext,
            args: List<String>
        ): TaskResult {
            return TaskResult.success()
        }
    }

    private fun buildLargeTaskGraph(size: Int): Map<String, Task> =
        (0 until size).associate { index ->
            val id = "task-$index"
            val dependencies = if (index == 0) emptyList() else listOf("task-${index - 1}")
            id to TestTask(id, dependencies = dependencies)
        }
}
