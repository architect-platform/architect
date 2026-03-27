package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.core.tasks.domain.events.ExecutionEvents
import io.github.architectplatform.core.tasks.domain.events.TaskEvents
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExecutionEventCollectorTest {

  @Test
  fun `stores replayable event log in arrival order`() {
    val collector = ExecutionEventCollector()
    val executionId = "exec-replay-1"

    val started = ExecutionEvents.executionStartedEvent("demo", executionId, message = "start")
    val task = TaskEvents.taskCompletedEvent("demo", executionId, "build", message = "done")
    val completed = ExecutionEvents.executionCompletedEvent("demo", executionId, message = "end")

    collector.onExecutionEvent(started)
    collector.onExecutionEvent(task)
    collector.onExecutionEvent(completed)

    val replay = collector.getReplayEvents(executionId)
    assertEquals(listOf("execution.started", "task.completed", "execution.completed"), replay.map { it.id })
  }

  @Test
  fun `supports replay from offset`() {
    val collector = ExecutionEventCollector()
    val executionId = "exec-replay-2"

    collector.onExecutionEvent(ExecutionEvents.executionStartedEvent("demo", executionId, message = "start"))
    collector.onExecutionEvent(TaskEvents.taskCompletedEvent("demo", executionId, "build", message = "done"))
    collector.onExecutionEvent(ExecutionEvents.executionCompletedEvent("demo", executionId, message = "end"))

    val replay = collector.getReplayEvents(executionId, from = 1)
    assertEquals(listOf("task.completed", "execution.completed"), replay.map { it.id })
  }
}
