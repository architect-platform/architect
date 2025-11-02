package io.github.architectplatform.engine.core.tasks.interfaces

import io.github.architectplatform.engine.core.tasks.application.ExecutionTracker
import io.github.architectplatform.engine.core.tasks.application.TaskService
import io.github.architectplatform.engine.core.tasks.interfaces.dto.ExecutionStatusDTO
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.github.architectplatform.engine.domain.events.ExecutionEvent
import io.github.architectplatform.engine.domain.events.ExecutionEventType
import io.github.architectplatform.engine.domain.events.ExecutionId
import io.github.architectplatform.engine.domain.events.ExecutionTaskEvent
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Controller("/api/executions")
@ExecuteOn(TaskExecutors.IO)
class ExecutionApiController(
    private val taskService: TaskService,
    private val executionTracker: ExecutionTracker
) {

  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

  @Get("/{executionId}")
  fun getExecutionFlow(
      @PathVariable executionId: ExecutionId
  ): Flow<ArchitectEvent<ExecutionEvent>> {
    val sharedFlow = taskService.getExecutionFlow(executionId)

    return flow {
      try {
        sharedFlow.collect { eventWrapper ->
          if (eventWrapper.event !is ExecutionEvent) {
            return@collect
          }
          emit(eventWrapper)
          val executionEvent = eventWrapper.event as ExecutionEvent
          logger.info("Collected event for execution $executionId: $executionEvent")
          if (executionEvent.parentProject == null) {
            when (executionEvent.executionEventType) {
              ExecutionEventType.COMPLETED,
              ExecutionEventType.FAILED -> {
                error("Execution completed with event: $eventWrapper")
              }
              else -> {}
            }
          }

        }
      } catch (_: Exception) {}
    }
  }

  /**
   * Get the current status and statistics of an execution.
   *
   * @param executionId The unique identifier of the execution
   * @return The execution status including progress and statistics
   */
  @Get("/{executionId}/status")
  fun getExecutionStatus(
      @PathVariable executionId: ExecutionId
  ): ExecutionStatusDTO? {
    logger.info("Fetching status for execution: $executionId")
    return executionTracker.getExecutionStatus(executionId)
  }

  /**
   * Get all tracked executions.
   *
   * @return List of all execution statuses
   */
  @Get
  fun getAllExecutions(): List<ExecutionStatusDTO> {
    logger.info("Fetching all executions")
    return executionTracker.getAllExecutions()
  }
}
