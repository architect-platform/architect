package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.engine.core.config.EngineConfiguration
import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.github.architectplatform.engine.domain.events.ExecutionEvent
import io.github.architectplatform.engine.domain.events.ExecutionId
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

  @EventListener
  fun onExecutionEvent(eventWrapper: ArchitectEvent<*>) {
    val event = eventWrapper.event
    if (event is ExecutionEvent) {
      val flow = flows.getOrPut(event.executionId) { newFlow() }
      @Suppress("UNCHECKED_CAST")
      val typedWrapper = eventWrapper as ArchitectEvent<ExecutionEvent>
      val emitted = flow.tryEmit(typedWrapper)
      if (!emitted) {
        logger.warn("Could not emit event for ${event.executionId}")
      }
    }
  }
}
