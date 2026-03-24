package io.github.architectplatform.cli.command

import io.github.architectplatform.cli.dto.RegisterProjectRequest
import io.github.architectplatform.cli.client.EngineCommandClient
import io.github.architectplatform.cli.engine.EngineHealthChecker
import java.io.File
import kotlin.system.exitProcess

/**
 * Handles engine lifecycle commands: install, start, stop, clean, reload-plugins.
 */
class EngineCommandHandler(
  private val engineCommandClient: EngineCommandClient,
  private val engineHealthChecker: EngineHealthChecker,
  private val extractProjectName: (String) -> String,
) {

  var startupTimeoutSeconds: Int = 30
  var plain: Boolean = false
  var noDaemon: Boolean = false

  fun handle(args: List<String>) {
    val arg = args.getOrNull(1)
    if (arg == null) {
      println("No command provided for 'engine'. Available commands: register, list, execute")
      return
    }

    when (arg) {
      "install" -> {
        println("Installing Architect Engine (optional for embedded mode)...")
        executeShell(
          "curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-cli/.installers/bash | bash"
        )
      }
      "install-ci" -> {
        println("Installing Architect Engine for CI...")
        executeShell(
          "curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-engine/.installers/bash-ci | bash"
        )
      }
      "start" -> {
        println("Running Architect Engine...")
        executeShell("architect-engine", wait = false)
      }
      "stop" -> {
        println("Stopping Architect Engine...")
        executeShell("pkill -f architect-engine")
      }
      "clean" -> {
        println("Cleaning Architect Engine...")
        executeShell("rm -rf ~/.architect-engine")
      }
      "reload-plugins" -> {
        ensureEngineRunning()
        val projectPath = System.getProperty("user.dir")
        val projectName = extractProjectName(projectPath)
        engineCommandClient.registerProject(RegisterProjectRequest(name = projectName, path = projectPath))
        engineCommandClient.reloadProjectPlugins(projectName)
        println("✅ Reloaded plugins for $projectName")
      }
      else -> {
        println("Unknown command for 'engine': $arg")
        println("Available commands: install, start, stop, clean, reload-plugins")
      }
    }
  }

  fun ensureEngineRunning() {
    if (noDaemon) return
    if (engineHealthChecker.isRunning()) return

    val engineBinary = resolveEngineBinary()
    if (engineBinary == null) {
      println(
        "❌ Architect Engine not found.\n" +
          "   Install it with: architect engine install\n" +
          "   Or start manually and retry with: architect --no-daemon <task>"
      )
      exitProcess(1)
    }

    if (!plain) println("⚙️  Starting Architect Engine...")
    ProcessBuilder(engineBinary)
      .inheritIO()
      .start()

    val timeoutMs = startupTimeoutSeconds * 1_000L
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      Thread.sleep(500)
      if (engineHealthChecker.isRunning()) {
        if (!plain) println("✅ Engine ready")
        return
      }
    }

    println("❌ Engine failed to start within ${startupTimeoutSeconds}s. Run 'architect engine start' manually.")
    exitProcess(1)
  }

  internal fun resolveEngineBinary(): String? {
    val home = System.getProperty("user.home") ?: return null
    val localBin = File("$home/.architect/bin/architect-engine")
    if (localBin.exists() && localBin.canExecute()) return localBin.absolutePath

    return try {
      val which = ProcessBuilder("which", "architect-engine")
        .redirectErrorStream(true)
        .start()
      val output = which.inputStream.bufferedReader().readLine()?.trim()
      which.waitFor()
      if (!output.isNullOrBlank()) output else null
    } catch (_: Exception) {
      null
    }
  }

  private fun executeShell(command: String, wait: Boolean = true) {
    try {
      val process = ProcessBuilder("sh", "-c", command)
        .inheritIO()
        .start()
      if (wait) {
        val exitCode = process.waitFor()
        if (exitCode == 0) {
          println("Command: $command executed successfully.")
        } else {
          println("Command: $command exited with code $exitCode.")
        }
      } else {
        println("Command: $command is running in the background.")
      }
    } catch (e: Exception) {
      println("Failed to execute command: $command - ${e.message}")
    }
  }
}
