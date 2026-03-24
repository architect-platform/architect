package io.github.architectplatform.cli.command

import kotlin.system.exitProcess

/**
 * Handles cache subcommands: clear, info.
 */
class CacheCommandHandler {

  var json: Boolean = false

  fun handle(args: List<String>) {
    val cache = io.github.architectplatform.core.tasks.application.LocalOutputCache()
    val subCommand = args.getOrNull(1)
    when (subCommand) {
      "clear" -> {
        cache.clear()
        println("✅ Cache cleared")
      }
      "info" -> {
        val entries = cache.entryCount()
        val sizeBytes = cache.sizeBytes()
        val sizeDisplay = when {
          sizeBytes < 1024 -> "${sizeBytes}B"
          sizeBytes < 1024 * 1024 -> "${"%.1f".format(sizeBytes / 1024.0)}KB"
          else -> "${"%.1f".format(sizeBytes / (1024.0 * 1024.0))}MB"
        }
        if (json) {
          println("""{"entries":$entries,"sizeBytes":$sizeBytes}""")
        } else {
          println()
          println("━".repeat(40))
          println("📦 Task Output Cache")
          println("━".repeat(40))
          println("  Entries:  $entries")
          println("  Size:     $sizeDisplay")
          println()
        }
      }
      else -> {
        println("Usage: architect cache <clear|info>")
        println()
        println("Commands:")
        println("  clear   Wipe the local task output cache")
        println("  info    Show cache size and entry count")
        exitProcess(1)
      }
    }
  }
}
