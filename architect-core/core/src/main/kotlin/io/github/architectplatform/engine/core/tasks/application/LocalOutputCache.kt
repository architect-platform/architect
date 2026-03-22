package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.api.core.tasks.TaskResult
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * File-system backed task output cache.
 *
 * Stores serialized task results in `~/.architect/cache/{cacheKey}/`.
 * Each entry contains:
 * - `result.json` — success flag, message, exit code
 * - `stdout.txt` — captured standard output (if applicable)
 */
class LocalOutputCache(
  private val cacheDir: Path = Path.of(System.getProperty("user.home"), ".architect", "cache"),
) {

  init {
    Files.createDirectories(cacheDir)
  }

  fun get(cacheKey: String): CachedResult? {
    val dir = cacheDir.resolve(cacheKey)
    val resultFile = dir.resolve("result.json").toFile()
    if (!resultFile.exists()) return null
    return try {
      val lines = resultFile.readLines()
      val success = lines.getOrNull(0)?.toBooleanStrictOrNull() ?: return null
      val message = lines.drop(1).joinToString("\n").ifBlank { null }
      val stdout = dir.resolve("stdout.txt").toFile().let { if (it.exists()) it.readText() else null }
      CachedResult(success = success, message = message, stdout = stdout)
    } catch (_: Exception) {
      null
    }
  }

  fun store(cacheKey: String, result: TaskResult, stdout: String? = null) {
    val dir = cacheDir.resolve(cacheKey)
    Files.createDirectories(dir)
    val resultFile = dir.resolve("result.json").toFile()
    resultFile.writeText("${result.success}\n${result.message.orEmpty()}")
    if (stdout != null) {
      dir.resolve("stdout.txt").toFile().writeText(stdout)
    }
  }

  fun contains(cacheKey: String): Boolean =
    cacheDir.resolve(cacheKey).resolve("result.json").toFile().exists()

  fun clear() {
    if (Files.exists(cacheDir)) {
      cacheDir.toFile().deleteRecursively()
      Files.createDirectories(cacheDir)
    }
  }

  fun sizeBytes(): Long {
    if (!Files.exists(cacheDir)) return 0
    var total = 0L
    cacheDir.toFile().walkTopDown().filter { it.isFile }.forEach { total += it.length() }
    return total
  }

  fun entryCount(): Int {
    if (!Files.exists(cacheDir)) return 0
    return cacheDir.toFile().listFiles()?.count { it.isDirectory } ?: 0
  }

  data class CachedResult(
    val success: Boolean,
    val message: String?,
    val stdout: String?,
  ) {
    fun toTaskResult(): TaskResult =
      if (success) TaskResult.success(message) else TaskResult.failure(message)
  }
}
