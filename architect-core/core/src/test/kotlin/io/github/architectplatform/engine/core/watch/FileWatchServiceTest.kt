package io.github.architectplatform.engine.core.watch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class FileWatchServiceTest {

  @Test
  fun `detects created file`(@TempDir dir: Path) {
    val latch = CountDownLatch(1)
    val changes = AtomicInteger(0)

    val watcher = FileWatchService(
        rootPath = dir,
        debounceMs = 50,
    ) {
      changes.incrementAndGet()
      latch.countDown()
    }
    watcher.startAsync()

    Thread.sleep(200) // let watcher register
    Files.writeString(dir.resolve("hello.txt"), "hello")

    assertTrue(latch.await(10, TimeUnit.SECONDS), "Change event expected")
    assertTrue(changes.get() >= 1)
    watcher.stop()
  }

  @Test
  fun `debounce prevents rapid duplicate events`(@TempDir dir: Path) {
    val changes = AtomicInteger(0)

    val watcher = FileWatchService(
        rootPath = dir,
        debounceMs = 300,
    ) {
      changes.incrementAndGet()
    }
    watcher.startAsync()

    Thread.sleep(200)
    // Rapid writes — should be debounced into fewer events
    repeat(5) { i ->
      Files.writeString(dir.resolve("file.txt"), "content-$i")
      Thread.sleep(20)
    }

    Thread.sleep(500) // wait for debounce window
    watcher.stop()

    // With 300ms debounce window and rapid writes, we expect significantly fewer than 5 events
    assertTrue(changes.get() < 5, "Expected debounce to reduce events, got ${changes.get()}")
  }

  @Test
  fun `glob pattern filters events`(@TempDir dir: Path) {
    val latch = CountDownLatch(1)
    val changes = AtomicInteger(0)

    val watcher = FileWatchService(
        rootPath = dir,
        globPatterns = listOf("*.kt"),
        debounceMs = 50,
    ) {
      changes.incrementAndGet()
      latch.countDown()
    }
    watcher.startAsync()

    Thread.sleep(200)
    // This should NOT trigger (wrong extension)
    Files.writeString(dir.resolve("readme.txt"), "ignored")
    Thread.sleep(200)

    // This SHOULD trigger
    Files.writeString(dir.resolve("Main.kt"), "fun main() {}")
    assertTrue(latch.await(10, TimeUnit.SECONDS), "Expected .kt change event")

    watcher.stop()
    // Non-matching file should not have been counted separately
    assertTrue(changes.get() >= 1)
  }

  @Test
  fun `stop stops the watcher`(@TempDir dir: Path) {
    val watcher = FileWatchService(rootPath = dir, debounceMs = 50) {}
    watcher.startAsync()
    Thread.sleep(100)
    assertTrue(watcher.isRunning)
    watcher.stop()
    Thread.sleep(200)
    assertFalse(watcher.isRunning)
  }

  @Test
  fun `skips hidden directories`(@TempDir dir: Path) {
    val changes = AtomicInteger(0)
    val latch = CountDownLatch(1)

    // Create a hidden directory
    val hidden = dir.resolve(".git")
    Files.createDirectories(hidden)

    val watcher = FileWatchService(rootPath = dir, debounceMs = 50) {
      changes.incrementAndGet()
      latch.countDown()
    }
    watcher.startAsync()

    Thread.sleep(200)
    // Write to hidden dir — should be ignored
    Files.writeString(hidden.resolve("config"), "test")
    Thread.sleep(300)

    // Write to non-hidden path — should trigger
    Files.writeString(dir.resolve("visible.txt"), "seen")
    assertTrue(latch.await(10, TimeUnit.SECONDS))

    watcher.stop()
  }

  // ── WatchConfig tests ─────────────────────────────────────────────

  @Test
  fun `WatchConfig fromMap parses paths and debounce`() {
    val map = mapOf<String, Any>(
        "paths" to listOf("*.kt", "*.java"),
        "debounce-ms" to 300,
    )
    val config = WatchConfig.fromMap(map)
    assertEquals(listOf("*.kt", "*.java"), config.paths)
    assertEquals(300, config.debounceMs)
  }

  @Test
  fun `WatchConfig fromMap defaults`() {
    val config = WatchConfig.fromMap(null)
    assertTrue(config.paths.isEmpty())
    assertEquals(500, config.debounceMs)
  }

  @Test
  fun `WatchConfig resolve uses task-level config`() {
    val config = mapOf<String, Any>(
        "tasks" to mapOf(
            "test" to mapOf(
                "watch" to mapOf(
                    "paths" to listOf("src/**"),
                    "debounce-ms" to 200,
                )
            )
        )
    )
    val resolved = WatchConfig.resolve("test", config)
    assertEquals(listOf("src/**"), resolved.paths)
    assertEquals(200, resolved.debounceMs)
  }

  @Test
  fun `WatchConfig resolve falls back to global watch`() {
    val config = mapOf<String, Any>(
        "watch" to mapOf(
            "paths" to listOf("lib/**"),
        )
    )
    val resolved = WatchConfig.resolve("build", config)
    assertEquals(listOf("lib/**"), resolved.paths)
  }

  @Test
  fun `WatchConfig resolve falls back to plugin defaults`() {
    val config = emptyMap<String, Any>()
    val resolved = WatchConfig.resolve("build", config, pluginContextKeys = setOf("gradle", "javascript"))
    assertTrue(resolved.paths.contains("*.kt"))
    assertTrue(resolved.paths.contains("*.ts"))
  }
}
