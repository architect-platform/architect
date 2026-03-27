package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.core.config.EngineConfiguration
import io.github.architectplatform.core.domain.events.ArchitectEvent
import io.github.architectplatform.core.domain.events.ExecutionEvent
import io.github.architectplatform.core.domain.events.ExecutionId
import io.micronaut.context.annotation.Property
import io.micronaut.runtime.event.annotation.EventListener
import jakarta.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.slf4j.LoggerFactory

@Singleton
class ExecutionEventCollector(
    @Property(name = EngineConfiguration.EventCollector.REPLAY_SIZE, defaultValue = "${EngineConfiguration.EventCollector.DEFAULT_REPLAY_SIZE}")
    private val replaySize: Int = EngineConfiguration.EventCollector.DEFAULT_REPLAY_SIZE,
    
    @Property(name = EngineConfiguration.EventCollector.BUFFER_CAPACITY, defaultValue = "${EngineConfiguration.EventCollector.DEFAULT_BUFFER_CAPACITY}")
    private val bufferCapacity: Int = EngineConfiguration.EventCollector.DEFAULT_BUFFER_CAPACITY
) {

  private val logger = LoggerFactory.getLogger(this::class.java)
  private val flows = mutableMapOf<ExecutionId, MutableSharedFlow<ArchitectEvent<ExecutionEvent>>>()
  private val eventLogs = mutableMapOf<ExecutionId, MutableList<ArchitectEvent<ExecutionEvent>>>()

  private fun newFlow(): MutableSharedFlow<ArchitectEvent<ExecutionEvent>> =
      MutableSharedFlow(
          replay = replaySize,
          extraBufferCapacity = bufferCapacity,
          onBufferOverflow = BufferOverflow.DROP_OLDEST)

  fun getFlow(executionId: ExecutionId): Flow<ArchitectEvent<ExecutionEvent>> =
      synchronized(flows) {
        // Subscribers share this flow
        flows.getOrPut(executionId) { newFlow() }
      }

  fun getReplayEvents(executionId: ExecutionId, from: Int = 0): List<ArchitectEvent<ExecutionEvent>> =
      synchronized(eventLogs) {
        val events = eventLogs[executionId]?.toList().orEmpty()
        val start = from.coerceAtLeast(0).coerceAtMost(events.size)
        events.drop(start)
      }

  @EventListener
  fun onExecutionEvent(eventWrapper: ArchitectEvent<*>) {
    val event = eventWrapper.event
    if (event is ExecutionEvent) {
      val flow = flows.getOrPut(event.executionId) { newFlow() }
      @Suppress("UNCHECKED_CAST")
      val typedWrapper = eventWrapper as ArchitectEvent<ExecutionEvent>
      synchronized(eventLogs) {
        eventLogs.getOrPut(event.executionId) { mutableListOf() }.add(typedWrapper)
      }
      val emitted = flow.tryEmit(typedWrapper)
      if (!emitted) {
        logger.warn("Could not emit event for ${event.executionId}")
      }
    }
  }
}
