package io.github.architectplatform.core.domain.events

import io.github.architectplatform.core.tasks.domain.events.ExecutionEvents
import io.github.architectplatform.core.tasks.domain.events.TaskEvents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypedArchitectEventTest {

  @Test
  fun `maps task started event to TaskStartedEvent`() {
    val source = TaskEvents.taskStartedEvent(
      project = "demo",
      executionId = "exec-1",
      taskId = "build",
      message = "starting",
    )

    @Suppress("UNCHECKED_CAST")
    val typed = (source as ArchitectEvent<ExecutionEvent>).toTypedArchitectEvent()

    assertEquals("task.started", typed.id)
    assertEquals("TASK_STARTED", typed.type)
    assertTrue(typed.event is TaskStartedEvent)
    assertEquals("build", (typed.event as TaskStartedEvent).taskId)
  }

  @Test
  fun `maps execution completed event to ExecutionCompletedEvent`() {
    val source = ExecutionEvents.executionCompletedEvent(
      project = "demo",
      executionId = "exec-2",
      message = "done",
    )

    val typed = source.toTypedArchitectEvent()

    assertEquals("execution.completed", typed.id)
    assertEquals("EXECUTION_COMPLETED", typed.type)
    assertTrue(typed.event is ExecutionCompletedEvent)
    assertEquals("done", (typed.event as ExecutionCompletedEvent).message)
  }
}
