package io.github.architectplatform.engine.core.tasks.interfaces

import io.github.architectplatform.engine.core.tasks.application.TaskService
import io.github.architectplatform.core.tasks.domain.events.ExecutionEvents.executionCompletedEvent
import io.github.architectplatform.core.tasks.domain.events.ExecutionEvents.executionFailedEvent
import io.github.architectplatform.core.tasks.domain.events.ExecutionEvents.executionStartedEvent
import io.github.architectplatform.core.tasks.domain.events.TaskEvents.taskCompletedEvent
import io.github.architectplatform.core.tasks.domain.events.TaskEvents.taskFailedEvent
import io.github.architectplatform.core.domain.events.ArchitectEvent
import io.github.architectplatform.core.domain.events.ExecutionEvent
import io.github.architectplatform.core.domain.events.ExecutionEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ExecutionControllerTest {

  private val taskService = mock<TaskService>()
  private val controller = ExecutionController(taskService)

  @Test
  fun `should stream execution events to clients`() = runBlocking {
    val executionId = "exec-123"
    whenever(taskService.getExecutionFlow(executionId)).thenReturn(
      eventFlow(
        executionStartedEvent("demo", executionId, message = "started"),
        taskCompletedEvent("demo", executionId, "build", message = "build done"),
        executionCompletedEvent("demo", executionId, message = "all done"),
      )
    )

    val events = controller.getExecutionFlow(executionId).toList()

    assertEquals(3, events.size)
    assertEquals(ExecutionEventType.STARTED, events[0].event?.executionEventType)
    assertEquals(ExecutionEventType.TASK_COMPLETED, events[1].event?.executionEventType)
    assertEquals(ExecutionEventType.COMPLETED, events[2].event?.executionEventType)
  }

  @Test
  fun `should terminate stream after completed root event`() = runBlocking {
    val executionId = "exec-completed"
    whenever(taskService.getExecutionFlow(executionId)).thenReturn(
      eventFlow(
        executionStartedEvent("demo", executionId, message = "started"),
        executionCompletedEvent("demo", executionId, message = "done"),
        taskCompletedEvent("demo", executionId, "after-terminal", message = "should not stream"),
      )
    )

    val events = controller.getExecutionFlow(executionId).toList()

    assertEquals(2, events.size)
    assertEquals(listOf("execution.started", "execution.completed"), events.map { it.id })
  }

  @Test
  fun `should terminate stream after failed root event`() = runBlocking {
    val executionId = "exec-failed"
    whenever(taskService.getExecutionFlow(executionId)).thenReturn(
      eventFlow(
        executionStartedEvent("demo", executionId, message = "started"),
        executionFailedEvent("demo", executionId, message = "boom", errorDetails = "stack"),
        taskCompletedEvent("demo", executionId, "after-terminal", message = "should not stream"),
      )
    )

    val events = controller.getExecutionFlow(executionId).toList()

    assertEquals(2, events.size)
    assertEquals(listOf("execution.started", "execution.failed"), events.map { it.id })
    assertEquals(ExecutionEventType.FAILED, events.last().event?.executionEventType)
  }

  @Test
  fun `should continue streaming after task failure until execution failed event`() = runBlocking {
    val executionId = "exec-task-failed"
    whenever(taskService.getExecutionFlow(executionId)).thenReturn(
      eventFlow(
        executionStartedEvent("demo", executionId, message = "started"),
        taskCompletedEvent("demo", executionId, "prepare", message = "prepare done"),
        taskFailedEvent(
          "demo",
          executionId,
          "build",
          message = "build failed",
          errorDetails = "boom",
        ),
        executionFailedEvent("demo", executionId, message = "execution failed", errorDetails = "boom"),
      )
    )

    val events = controller.getExecutionFlow(executionId).toList()

    assertEquals(
      listOf("execution.started", "task.completed", "task.failed", "execution.failed"),
      events.map { it.id },
    )
    assertEquals(ExecutionEventType.FAILED, events.last().event?.executionEventType)
  }

  private fun eventFlow(vararg events: ArchitectEvent<out ExecutionEvent>): Flow<ArchitectEvent<ExecutionEvent>> {
    @Suppress("UNCHECKED_CAST")
    return flowOf(*events) as Flow<ArchitectEvent<ExecutionEvent>>
  }
}