package io.github.architectplatform.engine.core.tasks.domain

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.engine.core.tasks.infrastructure.InMemoryTaskRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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

    /**
     * Test implementation of Task.
     */
    class TestTask(
        override val id: String,
        private val dependencies: List<String> = emptyList()
    ) : Task {
        override fun description(): String = "Test task $id"
        
        override fun depends(): List<String> = dependencies
        
        override fun execute(
            environment: Environment,
            projectContext: ProjectContext,
            args: List<String>
        ): TaskResult {
            return TaskResult.success()
        }
    }
}
