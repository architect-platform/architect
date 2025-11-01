package io.github.architectplatform.cli

import ConsoleUI
import io.github.architectplatform.cli.client.EngineCommandClient
import io.github.architectplatform.cli.dto.RegisterProjectRequest
import io.micronaut.context.ApplicationContext
import jakarta.inject.Singleton
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Singleton
@Command(
    name = "architect",
    description = ["Architect CLI"],
)
class ArchitectLauncher(private val engineCommandClient: EngineCommandClient) : Runnable {

  @Parameters(
      description = ["Command to execute"],
      arity = "0..*",
      paramLabel = "<command>",
  )
  var command: String? = null

  @Parameters(
      description = ["Arguments for the command"],
      arity = "0..*",
      paramLabel = "<args>",
  )
  var args: List<String> = emptyList()

  @CommandLine.Option(
      names = ["-p", "--plain"],
      description = ["Enable plain output (CI Environments)"],
      defaultValue = "false",
  )
  var plain: Boolean = false

  override fun run() {
    if (command == "engine") {
      handleEngineCommand()
      return
    }

    val projectPath = System.getProperty("user.dir")
    val projectName = projectPath.substringAfterLast("/").substringBeforeLast(".")

    println("📦 Registering project: $projectName")
    val request = RegisterProjectRequest(name = projectName, path = projectPath)
    engineCommandClient.registerProject(request)

    args = args.drop(1)

    if (command == null) {
      val commands = engineCommandClient.getAllTasks(projectName)
      println("🧭 Available tasks:")
      commands.forEach { println(" - ${it.id}") }
      return
    }

    val ui = ConsoleUI(command!!, plain)

    println("🛠️Executing task: $command")
    runBlocking {
      val startTime = System.currentTimeMillis()
      try {
        val executionId = engineCommandClient.execute(projectName, command!!, args)
        val flow = engineCommandClient.getExecutionFlow(executionId)
        flow.collect {
            ui.process(it)
        }
        val duration = (System.currentTimeMillis() - startTime) / 1000.0
        if (ui.hasFailed) {
          ui.completeWithError("Task failed")
          exitProcess(1)
        } else {
          ui.complete("Task completed in ${"%.1f".format(duration)}s")
          exitProcess(0)
        }
      } catch (e: Exception) {
        ui.completeWithError("Task aborted: ${e.message}")
        exitProcess(1)
      }
    }
  }

  private fun handleEngineCommand() {
    val arg = args.getOrNull(1)
    if (arg == null) {
      println("No command provided for 'engine'. Available commands: register, list, execute")
      return
    }

    when (arg) {
      "install" -> {
        println("Installing Architect Engine...")
        val command =
            "curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-cli/.installers/bash | bash"
        execute(command)
      }
      "install-ci" -> {
        println("Installing Architect Engine for CI...")
        val command =
            "curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-engine/.installers/bash-ci | bash"
        execute(command)
      }
      "start" -> {
        println("Running Architect Engine...")
        val command = "architect-engine"
        execute(command, false)
      }
      "stop" -> {
        println("Stopping Architect Engine...")
        val command = "pkill -f architect-engine"
        execute(command)
      }
      "clean" -> {
        println("Cleaning Architect Engine...")
        val command = "rm -rf ~/.architect-engine"
        execute(command)
      }
      else -> {
        println("Unknown command for 'engine': $arg")
        println("Available commands: install, start, stop, clean")
      }
    }
  }

  private fun execute(command: String, wait: Boolean = true) {
    try {
      val process = Runtime.getRuntime().exec(command)
      if (wait) {
        process.waitFor()
        println("Command: $command executed successfully.")
      } else {
        println("Command: $command is running in the background.")
      }
    } catch (e: Exception) {
      println("Failed to execute command: $command - ${e.message}")
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val context = ApplicationContext.run()
      val launcher = context.getBean(ArchitectLauncher::class.java)
      val exitCode = CommandLine(launcher).execute(*args)
      context.close()
      exitProcess(exitCode)
    }
  }
}
