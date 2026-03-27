package io.github.architectplatform.engine.core.tasks.interfaces

import io.github.architectplatform.engine.core.tasks.application.TaskService
import io.github.architectplatform.core.domain.events.toTypedArchitectEvent
import io.github.architectplatform.core.domain.events.ExecutionEvent
import io.github.architectplatform.core.domain.events.ExecutionEventType
import io.github.architectplatform.core.domain.events.ExecutionId
import io.github.architectplatform.core.domain.events.TypedArchitectEvent
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.transformWhile
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Controller("/api/executions")
@ExecuteOn(TaskExecutors.IO)
class ExecutionController(private val taskService: TaskService) {

  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

  @Get("/{executionId}")
  fun getExecutionFlow(
      @PathVariable executionId: ExecutionId
  ): Flow<TypedArchitectEvent> {
    val sharedFlow = taskService.getExecutionFlow(executionId)

    // Emit events downstream. Stop cleanly when the root execution reaches a terminal state
    // (COMPLETED, FAILED, or CANCELLED on the top-level project — parentProject == null).
    // Using transformWhile avoids throwing an exception as control flow.
    return sharedFlow
      .filter { it.event is ExecutionEvent }
      .transformWhile { eventWrapper ->
        emit(eventWrapper.toTypedArchitectEvent())
        val event = eventWrapper.event as ExecutionEvent
        logger.debug("SSE event for execution {}: type={}", executionId, event.executionEventType)
        !(event.parentProject == null &&
          (eventWrapper.id == "execution.completed" || eventWrapper.id == "execution.failed" || eventWrapper.id == "execution.cancelled") &&
          (event.executionEventType == ExecutionEventType.COMPLETED ||
            event.executionEventType == ExecutionEventType.FAILED ||
            event.executionEventType == ExecutionEventType.CANCELLED))
      }
  }

  @Delete("/{executionId}")
  fun cancelExecution(@PathVariable executionId: ExecutionId): Map<String, Any> {
    val cancelled = taskService.cancelExecution(executionId)
    return mapOf(
      "executionId" to executionId,
      "cancelled" to cancelled,
    )
  }
}
