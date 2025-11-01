package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.engine.core.project.app.ProjectService
import io.github.architectplatform.engine.core.project.domain.Project
import io.github.architectplatform.engine.core.tasks.infrastructure.InMemoryTaskRegistry
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.io.path.Path

/**
 * Unit tests for TaskService.
 */
@MicronautTest
class TaskServiceTest {

    private lateinit var projectService: ProjectService
    private lateinit var taskExecutor: TaskExecutor
    private lateinit var eventCollector: ExecutionEventCollector
    private lateinit var eventPublisher: ApplicationEventPublisher<ArchitectEvent<*>>
    private lateinit var taskService: TaskService

    @BeforeEach
    fun setup() {
        projectService = mock()
        taskExecutor = mock()
        eventCollector = mock()
        eventPublisher = mock()
        taskService = TaskService(projectService, taskExecutor, eventCollector, eventPublisher)
    }

    @Test
    fun `should get all tasks for a project`() {
        // Given
        val projectName = "test-project"
        val taskRegistry = InMemoryTaskRegistry()
        val testTask = TestTask("test-task")
        taskRegistry.add(testTask)

        val project = createTestProject(projectName, taskRegistry)
        whenever(projectService.getProject(projectName)).thenReturn(project)

        // When
        val tasks = taskService.getAllTasks(projectName)

        // Then
        assertEquals(1, tasks.size)
        assertEquals("test-task", tasks[0].id)
    }

    @Test
    fun `should get task by id`() {
        // Given
        val projectName = "test-project"
        val taskId = "test-task"
        val taskRegistry = InMemoryTaskRegistry()
        val testTask = TestTask(taskId)
        taskRegistry.add(testTask)

        val project = createTestProject(projectName, taskRegistry)
        whenever(projectService.getProject(projectName)).thenReturn(project)

        // When
        val task = taskService.getTaskById(projectName, taskId)

        // Then
        assertNotNull(task)
        assertEquals(taskId, task.id)
    }

    @Test
    fun `should throw exception when project not found`() {
        // Given
        val projectName = "non-existent"
        whenever(projectService.getProject(projectName)).thenReturn(null)

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            taskService.getAllTasks(projectName)
        }
    }

    @Test
    fun `should throw exception when task not found`() {
        // Given
        val projectName = "test-project"
        val taskRegistry = InMemoryTaskRegistry()
        val project = createTestProject(projectName, taskRegistry)
        whenever(projectService.getProject(projectName)).thenReturn(project)

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            taskService.getTaskById(projectName, "non-existent-task")
        }
    }

    @Test
    fun `should return sorted tasks by id`() {
        // Given
        val projectName = "test-project"
        val taskRegistry = InMemoryTaskRegistry()
        taskRegistry.add(TestTask("z-task"))
        taskRegistry.add(TestTask("a-task"))
        taskRegistry.add(TestTask("m-task"))

        val project = createTestProject(projectName, taskRegistry)
        whenever(projectService.getProject(projectName)).thenReturn(project)

        // When
        val tasks = taskService.getAllTasks(projectName)

        // Then
        assertEquals(3, tasks.size)
        assertEquals("a-task", tasks[0].id)
        assertEquals("m-task", tasks[1].id)
        assertEquals("z-task", tasks[2].id)
    }

    private fun createTestProject(
        name: String,
        taskRegistry: InMemoryTaskRegistry
    ): Project {
        val config = mapOf("project" to mapOf("name" to name))
        val context = ProjectContext(Path("/test/path"), config)
        return Project(
            name = name,
            path = "/test/path",
            context = context,
            plugins = emptyList(),
            subProjects = emptyList(),
            taskRegistry = taskRegistry
        )
    }

    /**
     * Test implementation of Task.
     */
    class TestTask(override val id: String) : Task {
        override fun description(): String = "Test task $id"
        
        override fun execute(
            environment: Environment,
            projectContext: ProjectContext,
            args: List<String>
        ): TaskResult {
            return TaskResult.success()
        }
    }
}
