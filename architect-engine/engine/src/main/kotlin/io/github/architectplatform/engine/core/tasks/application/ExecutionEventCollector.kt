package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.core.config.EngineConfiguration
import io.github.architectplatform.core.domain.events.ArchitectEvent
import io.github.architectplatform.core.domain.events.ExecutionEvent
import io.github.architectplatform.core.domain.events.ExecutionId
import io.github.architectplatform.engine.core.metrics.MetricsService
import io.micronaut.context.annotation.Property
import io.micronaut.runtime.event.annotation.EventListener
import jakarta.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.slf4j.LoggerFactory

@Singleton
class ExecutionEventCollector(
    @Property(name = EngineConfiguration.EventCollector.REPLAY_SIZE, defaultValue = "${EngineConfiguration.EventCollector.DEFAULT_REPLAY_SIZE}")
    private val replaySize: Int = EngineConfiguration.EventCollector.DEFAULT_REPLAY_SIZE,
    
    @Property(name = EngineConfiguration.EventCollector.BUFFER_CAPACITY, defaultValue = "${EngineConfiguration.EventCollector.DEFAULT_BUFFER_CAPACITY}")
    private val bufferCapacity: Int = EngineConfiguration.EventCollector.DEFAULT_BUFFER_CAPACITY,

    @Property(name = EngineConfiguration.EventCollector.OVERFLOW_STRATEGY, defaultValue = EngineConfiguration.EventCollector.DEFAULT_OVERFLOW_STRATEGY)
    private val overflowStrategy: String = EngineConfiguration.EventCollector.DEFAULT_OVERFLOW_STRATEGY,

    private val metricsService: MetricsService = MetricsService(),
) {

  private val logger = LoggerFactory.getLogger(this::class.java)
  private val flows = mutableMapOf<ExecutionId, MutableSharedFlow<ArchitectEvent<ExecutionEvent>>>()
  private val eventLogs = mutableMapOf<ExecutionId, MutableList<ArchitectEvent<ExecutionEvent>>>()
  private val configuredOverflowStrategy = OverflowStrategy.from(overflowStrategy)

  private fun newFlow(): MutableSharedFlow<ArchitectEvent<ExecutionEvent>> =
      MutableSharedFlow(
          replay = replaySize,
          extraBufferCapacity = effectiveBufferCapacity(),
          onBufferOverflow = effectiveBufferOverflow())

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
      when (configuredOverflowStrategy) {
        OverflowStrategy.BLOCK -> {
          // BLOCK means no drop accounting; emit suspends until consumers catch up.
          kotlinx.coroutines.runBlocking {
            flow.emit(typedWrapper)
          }
        }
        OverflowStrategy.DROP_OLDEST,
        OverflowStrategy.EXPAND -> {
          val emitted = flow.tryEmit(typedWrapper)
          if (!emitted) {
            metricsService.incrementCounter("architect.events.dropped")
            logger.warn("Could not emit event for ${event.executionId}; event dropped due to overflow")
          } else {
            logBufferPressure(flow, event.executionId)
          }
        }
      }
    }
  }

  private fun logBufferPressure(flow: SharedFlow<ArchitectEvent<ExecutionEvent>>, executionId: ExecutionId) {
    val threshold = ((effectiveBufferCapacity() + replaySize) * 0.8).toInt()
    if (threshold > 0 && flow.replayCache.size >= threshold) {
      logger.warn(
        "Execution event buffer is above 80% capacity for {} (current={}, threshold={})",
        executionId,
        flow.replayCache.size,
        threshold,
      )
    }
  }

  private fun effectiveBufferCapacity(): Int =
    when (configuredOverflowStrategy) {
      OverflowStrategy.EXPAND -> (bufferCapacity * 2).coerceAtLeast(bufferCapacity + 1)
      else -> bufferCapacity
    }

  private fun effectiveBufferOverflow(): BufferOverflow =
    when (configuredOverflowStrategy) {
      OverflowStrategy.BLOCK -> BufferOverflow.SUSPEND
      OverflowStrategy.DROP_OLDEST,
      OverflowStrategy.EXPAND -> BufferOverflow.DROP_OLDEST
    }

  private enum class OverflowStrategy {
    BLOCK,
    DROP_OLDEST,
    EXPAND,
    ;

    companion object {
      fun from(raw: String): OverflowStrategy =
        runCatching { valueOf(raw.trim().uppercase()) }.getOrDefault(DROP_OLDEST)
    }
  }
}
