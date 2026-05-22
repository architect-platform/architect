package io.github.architectplatform.engine.core.startup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StartupProfileRecorderTest {

  @Test
  fun `should rank the top startup bottlenecks by duration`() {
    val timestamps = ArrayDeque(listOf(0L, 10_000_000L, 35_000_000L, 55_000_000L, 90_000_000L))
    val recorder = StartupProfileRecorder { timestamps.removeFirst() }

    recorder.checkpoint("micronaut-bootstrap")
    recorder.checkpoint("server-startup")
    recorder.checkpoint("service-ready")
    recorder.recordDuration("cloud-engine-registration", 22_000_000L)

    val top = recorder.topBottlenecks()

    assertEquals(listOf("server-startup", "cloud-engine-registration", "service-ready"), top.map { it.label })
    assertEquals(listOf(25_000_000L, 22_000_000L, 20_000_000L), top.map { it.durationNanos })
  }
}
