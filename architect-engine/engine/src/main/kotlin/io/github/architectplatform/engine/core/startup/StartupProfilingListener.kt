package io.github.architectplatform.engine.core.startup

import io.micronaut.discovery.event.ServiceReadyEvent
import io.micronaut.runtime.event.ApplicationStartupEvent
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import jakarta.inject.Singleton

@Singleton
class StartupProfilingListener(
  private val startupProfileRecorder: StartupProfileRecorder,
) {
  @EventListener
  fun onApplicationStartup(@Suppress("UNUSED_PARAMETER") event: ApplicationStartupEvent) {
    startupProfileRecorder.checkpoint("micronaut-bootstrap")
  }

  @EventListener
  fun onServerStartup(@Suppress("UNUSED_PARAMETER") event: ServerStartupEvent) {
    startupProfileRecorder.checkpoint("server-startup")
  }

  @EventListener
  fun onServiceReady(@Suppress("UNUSED_PARAMETER") event: ServiceReadyEvent) {
    startupProfileRecorder.checkpoint("service-ready")
    startupProfileRecorder.logTopBottlenecks()
  }
}
