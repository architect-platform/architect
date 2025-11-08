package io.github.architectplatform.cli

import io.github.architectplatform.cli.client.EngineCommandClient
import io.github.architectplatform.cli.dto.RegisterProjectRequest
import io.github.architectplatform.cli.dto.SetGitHubTokenRequest
import io.micronaut.context.ApplicationContext
import jakarta.inject.Singleton
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

/**
 * Main entry point for the Architect CLI application.
 *
 * This class provides a command-line interface for interacting with the Architect Engine,
 * enabling users to:
 * - Register projects with the engine
 * - Execute tasks within projects
 * - Manage the Architect Engine lifecycle (install, start, stop, clean)
 * - View available tasks for a project
 *
 * The launcher uses PicoCLI for command-line parsing and Micronaut for dependency injection.
 *
 * @property engineCommandClient HTTP client for communicating with the Architect Engine API
 */
@Singleton
@Command(
    name = "architect",
    description = ["Architect CLI"],
)
class ArchitectLauncher(private val engineCommandClient: EngineCommandClient) : Runnable {

  /**
   * The command to execute (e.g., "build", "test", "engine").
   * If null, lists available tasks for the current project.
   */
  @Parameters(
      description = ["Command to execute"],
      arity = "0..*",
      paramLabel = "<command>",
  )
  var command: String? = null

  /**
   * Additional arguments to pass to the command.
   */
  @Parameters(
      description = ["Arguments for the command"],
      arity = "0..*",
      paramLabel = "<args>",
  )
  var args: List<String> = emptyList()

  /**
   * Enable plain output mode for CI environments.
   * When true, disables rich terminal UI and outputs simple text.
   */
  @CommandLine.Option(
      names = ["-p", "--plain"],
      description = ["Enable plain output (CI Environments)"],
      defaultValue = "false",
  )
  var plain: Boolean = false

  /**
   * Main execution logic for the CLI.
   *
   * Flow:
   * 1. Check if command is "engine" and delegate to [handleEngineCommand]
   * 2. Register the current project with the engine
   * 3. If no command specified, list available tasks
   * 4. Otherwise, execute the specified task and display results
   *
   * @throws Exception if task execution fails
   */
  override fun run() {
    if (command == "engine") {
      handleEngineCommand()
      return
    }
    
    if (command == "login") {
      handleLoginCommand()
      return
    }

    val projectPath = System.getProperty("user.dir")
    val projectName = extractProjectName(projectPath)

    println("📦 Registering project: $projectName")
    val request = RegisterProjectRequest(name = projectName, path = projectPath)
    engineCommandClient.registerProject(request)

    if (command == null) {
      val commands = engineCommandClient.getAllTasks(projectName)
      println("🧭 Available tasks:")
      commands.forEach { println(" - ${it.id}") }
      return
    }

    // Drop first arg as it's the command itself (included by PicoCLI)
    val taskArgs = if (args.isNotEmpty()) args.drop(1) else emptyList()
    executeTask(projectName, command!!, taskArgs)
  }

  /**
   * Extracts project name from the project path.
   *
   * @param projectPath The full path to the project
   * @return The project name
   */
  private fun extractProjectName(projectPath: String): String {
    return projectPath.substringAfterLast("/").substringBeforeLast(".")
  }

  /**
   * Executes a task and displays progress.
   *
   * @param projectName The name of the project
   * @param taskName The name of the task to execute
   * @param taskArgs Arguments to pass to the task
   */
  private fun executeTask(projectName: String, taskName: String, taskArgs: List<String>) {
    val ui = ConsoleUI(taskName, plain)

    println()
    println("━".repeat(80))
    println("▶️  Executing task: $taskName")
    println("📦 Project: $projectName")
    println("━".repeat(80))
    println()
    
    runBlocking {
      val startTime = System.currentTimeMillis()
      try {
        val executionId = engineCommandClient.execute(projectName, taskName, taskArgs)
        val flow = engineCommandClient.getExecutionFlow(executionId)
        flow.collect { ui.process(it) }

        val duration = (System.currentTimeMillis() - startTime) / 1000.0
        if (ui.hasFailed) {
          ui.completeWithError("Task failed (duration: ${"%.1f".format(duration)}s)")
          exitProcess(1)
        } else {
          ui.complete("Task completed successfully (duration: ${"%.1f".format(duration)}s)")
          exitProcess(0)
        }
      } catch (e: Exception) {
        val duration = (System.currentTimeMillis() - startTime) / 1000.0
        println()
        println("❌ Task execution aborted")
        println("Error: ${e.message}")
        println()
        println("Stack Trace:")
        println(e.stackTraceToString())
        println()
        println("Duration: ${"%.1f".format(duration)}s")
        exitProcess(1)
      }
    }
  }

  /**
   * Handles login commands for authentication with external services.
   *
   * Supported commands:
   * - github: Authenticate with GitHub
   * - status: Check authentication status
   * - logout: Clear stored authentication
   */
  private fun handleLoginCommand() {
    val arg = args.getOrNull(1)
    if (arg == null) {
      println("No command provided for 'login'. Available commands: github, status, logout")
      return
    }

    when (arg) {
      "github" -> {
        println()
        println("━".repeat(80))
        println("🔐 GitHub Authentication")
        println("━".repeat(80))
        println()
        println("To authenticate with GitHub, you need a Personal Access Token (PAT).")
        println("This token will be used to avoid rate limiting when fetching plugins.")
        println()
        println("To create a token:")
        println("1. Go to https://github.com/settings/tokens")
        println("2. Click 'Generate new token' → 'Generate new token (classic)'")
        println("3. Give it a name (e.g., 'Architect CLI')")
        println("4. Select scopes: 'repo' (for private repos) or 'public_repo' (for public only)")
        println("5. Click 'Generate token' and copy the token")
        println()
        print("Enter your GitHub token (input will be hidden): ")
        
        // Read token securely (without echoing to console)
        val token = System.console()?.readPassword()?.let { String(it) }
            ?: readLine()?.trim() // Fallback for non-console environments
        
        if (token.isNullOrBlank()) {
          println()
          println("❌ No token provided. Authentication cancelled.")
          exitProcess(1)
        }
        
        println()
        println("Storing token securely...")
        
        try {
          val request = SetGitHubTokenRequest(token = token)
          val response = engineCommandClient.setGitHubToken(request)
          
          if (response.success) {
            println()
            println("✅ ${response.message}")
            println()
            println("You can now use Architect without GitHub rate limits!")
            println("Your token is stored securely in ~/.architect-engine/config.yml")
            exitProcess(0)
          } else {
            println()
            println("❌ Failed to store token: ${response.message}")
            exitProcess(1)
          }
        } catch (e: Exception) {
          println()
          println("❌ Failed to communicate with Architect Engine")
          println("Error: ${e.message}")
          println()
          println("Make sure the engine is running: architect engine start")
          exitProcess(1)
        }
      }
      "status" -> {
        try {
          val status = engineCommandClient.getGitHubStatus()
          println()
          if (status.authenticated) {
            println("✅ Authenticated with GitHub")
          } else {
            println("❌ Not authenticated with GitHub")
            println()
            println("Run 'architect login github' to authenticate")
          }
          println()
        } catch (e: Exception) {
          println()
          println("❌ Failed to check authentication status")
          println("Error: ${e.message}")
          println()
          println("Make sure the engine is running: architect engine start")
          exitProcess(1)
        }
      }
      "logout" -> {
        try {
          val response = engineCommandClient.clearGitHubToken()
          println()
          if (response.success) {
            println("✅ ${response.message}")
          } else {
            println("❌ ${response.message}")
          }
          println()
        } catch (e: Exception) {
          println()
          println("❌ Failed to logout")
          println("Error: ${e.message}")
          println()
          println("Make sure the engine is running: architect engine start")
          exitProcess(1)
        }
      }
      else -> {
        println("Unknown command for 'login': $arg")
        println("Available commands: github, status, logout")
      }
    }
  }

  /**
   * Handles engine-specific commands like install, start, stop, and clean.
   *
   * Supported commands:
   * - install: Downloads and installs the Architect Engine
   * - install-ci: Installs the engine optimized for CI environments
   * - start: Starts the Architect Engine as a background process
   * - stop: Stops any running Architect Engine processes
   * - clean: Removes all Architect Engine data
   */
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

  /**
   * Executes a shell command using the system's runtime.
   *
   * @param command The shell command to execute
   * @param wait If true, waits for the command to complete before returning
   */
  private fun execute(command: String, wait: Boolean = true) {
    try {
      val process = Runtime.getRuntime().exec(command)
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

  companion object {
    /**
     * Application entry point.
     *
     * Initializes the Micronaut application context, retrieves the launcher bean,
     * executes the command-line arguments, and terminates with the appropriate exit code.
     *
     * @param args Command-line arguments passed to the application
     */
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
