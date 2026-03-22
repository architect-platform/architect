package io.github.architectplatform.engine.core.tasks.interfaces

import io.github.architectplatform.engine.core.tasks.application.TaskService
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.github.architectplatform.engine.domain.events.ExecutionEvent
import io.github.architectplatform.engine.domain.events.ExecutionEventType
import io.github.architectplatform.engine.domain.events.ExecutionId
import io.micronaut.http.annotation.Controller
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
class ExecutionApiController(private val taskService: TaskService) {

  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

  @Get("/{executionId}")
  fun getExecutionFlow(
      @PathVariable executionId: ExecutionId
  ): Flow<ArchitectEvent<ExecutionEvent>> {
    val sharedFlow = taskService.getExecutionFlow(executionId)

    // Emit events downstream. Stop cleanly when the root execution reaches a terminal state
    // (COMPLETED or FAILED on the top-level project — parentProject == null).
    // Using transformWhile avoids throwing an exception as control flow.
    return sharedFlow
      .filter { it.event is ExecutionEvent }
      .transformWhile { eventWrapper ->
        emit(eventWrapper)
        val event = eventWrapper.event as ExecutionEvent
        logger.debug("SSE event for execution {}: type={}", executionId, event.executionEventType)
        // Continue while the event is NOT a terminal root-level execution event.
        // Task-level failures should still flow through so clients receive the final
        // execution.failed event that summarizes the overall run.
        !(event.parentProject == null &&
          (eventWrapper.id == "execution.completed" || eventWrapper.id == "execution.failed") &&
          (event.executionEventType == ExecutionEventType.COMPLETED ||
            event.executionEventType == ExecutionEventType.FAILED))
      }
  }
}
