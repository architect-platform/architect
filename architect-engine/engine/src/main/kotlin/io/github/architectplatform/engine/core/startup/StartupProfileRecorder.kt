package io.github.architectplatform.engine.core.startup

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

data class StartupTiming(
  val label: String,
  val durationNanos: Long,
) {
  fun durationMillis(): Double = durationNanos / 1_000_000.0
}

@Singleton
class StartupProfileRecorder(
  private val nanoTime: () -> Long = System::nanoTime,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)
  private val timings = linkedMapOf<String, Long>()
  private var lastCheckpointNanos = nanoTime()

  @Synchronized
  fun checkpoint(label: String): StartupTiming {
    val now = nanoTime()
    val timing = StartupTiming(label, now - lastCheckpointNanos)
    timings[label] = timing.durationNanos
    lastCheckpointNanos = now
    return timing
  }

  fun <T> measure(label: String, block: () -> T): T {
    val start = nanoTime()
    return try {
      block()
    } finally {
      recordDuration(label, nanoTime() - start)
    }
  }

  @Synchronized
  fun recordDuration(label: String, durationNanos: Long) {
    timings[label] = durationNanos
  }

  @Synchronized
  fun topBottlenecks(limit: Int = 3): List<StartupTiming> =
    timings.entries
      .sortedByDescending { it.value }
      .take(limit)
      .map { StartupTiming(it.key, it.value) }

  fun logTopBottlenecks(limit: Int = 3) {
    val top = topBottlenecks(limit)
    if (top.isEmpty()) {
      logger.info("Startup profiling captured no timings")
      return
    }

    top.forEachIndexed { index, timing ->
      logger.info(
        "Startup bottleneck #{}: {} took {} ms",
        index + 1,
        timing.label,
        String.format("%.3f", timing.durationMillis()),
      )
    }
  }
}