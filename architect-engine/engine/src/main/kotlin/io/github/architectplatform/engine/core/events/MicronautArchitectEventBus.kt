package io.github.architectplatform.engine.core.events

import io.github.architectplatform.engine.domain.events.ArchitectEvent
import io.micronaut.context.event.ApplicationEventPublisher
import jakarta.inject.Singleton

@Singleton
class MicronautArchitectEventBus(
  private val eventPublisher: ApplicationEventPublisher<ArchitectEvent<*>>,
) : EventBus<ArchitectEvent<*>> {
  override fun invoke(event: ArchitectEvent<*>) {
    eventPublisher.publishEvent(event)
  }
}