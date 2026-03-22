package io.github.architectplatform.cli

import io.github.architectplatform.cli.client.EngineCommandClient
import io.github.architectplatform.cli.dto.HistoryRecordDTO
import io.github.architectplatform.cli.dto.RegisterProjectRequest
import io.github.architectplatform.cli.history.LocalHistoryReader
import io.github.architectplatform.cli.embedded.EmbeddedTaskExecutor
import io.github.architectplatform.cli.dto.TaskPlanDTO
import io.github.architectplatform.cli.dto.ValidationResultDTO
import io.github.architectplatform.cli.engine.EngineHealthChecker
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import java.io.File
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
class ArchitectLauncher(
    private val engineCommandClient: EngineCommandClient,
    private val engineHealthChecker: EngineHealthChecker,
    private val localHistoryReader: LocalHistoryReader,
  private val embeddedTaskExecutor: EmbeddedTaskExecutor,
) : Runnable {

  @Property(name = "architect.engine.startup-timeout-seconds", defaultValue = "30")
  var startupTimeoutSeconds: Int = 30

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
   * When true, skips auto-starting the engine daemon.
   * Use in CI environments where the daemon is managed externally.
   */
  @CommandLine.Option(
      names = ["--no-daemon"],
      description = ["Skip auto-starting the engine daemon (for CI environments)"],
      defaultValue = "false",
  )
  var noDaemon: Boolean = false

    @CommandLine.Option(
      names = ["--embedded"],
      description = ["Run tasks in embedded mode without using the engine daemon"],
      defaultValue = "false",
    )
    var embedded: Boolean = false

  @CommandLine.Option(
      names = ["--json"],
      description = ["Output in JSON format for scripting"],
      defaultValue = "false",
  )
  var json: Boolean = false

  @CommandLine.Option(
      names = ["--filter"],
      description = ["Filter tasks by phase (e.g., BUILD, TEST, RELEASE)"],
  )
  var filter: String? = null

  @CommandLine.Option(
      names = ["--no-color"],
      description = ["Disable colors in output"],
      defaultValue = "false",
  )
  var noColor: Boolean = false

  @CommandLine.Option(
      names = ["--version", "-v"],
      description = ["Print version information"],
      defaultValue = "false",
  )
  var version: Boolean = false

  @CommandLine.Option(
      names = ["-w", "--watch"],
      description = ["Watch for file changes and re-execute the task"],
      defaultValue = "false",
  )
  var watch: Boolean = false

  @CommandLine.Option(
      names = ["--env"],
      description = ["Environment profile to apply (e.g., staging, production, ci)"],
  )
  var envProfile: String? = null

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
    // Detect color preferences
    if (noColor || System.getenv("NO_COLOR") != null || System.getenv("CI") != null) {
      plain = true
    }

    // Resolve active profile (explicit flag > CI auto-detection > default)
    val resolvedProfile = io.github.architectplatform.engine.core.project.app.ProfileMerger.detectProfile(envProfile)
    embeddedTaskExecutor.activeProfile = resolvedProfile

    if (version) {
      printVersion()
      return
    }

    if (command == "engine") {
      handleEngineCommand()
      return
    }

    if (command == "history") {
      val project = args.getOrNull(1)
      val records = if (project != null) {
        localHistoryReader.getByProject(project).ifEmpty {
          runCatching { engineCommandClient.getProjectHistory(project) }.getOrElse { emptyList() }
        }
      } else {
        localHistoryReader.getAll().ifEmpty {
          runCatching { engineCommandClient.getHistory() }.getOrElse { emptyList() }
        }
      }
      printHistory(records)
      return
    }

    if (command == "plugin") {
      handlePluginCommand()
      return
    }

    val useEmbeddedExecution = embedded || (noDaemon && !engineHealthChecker.isRunning())
    if (useEmbeddedExecution) {
      runEmbeddedMode()
      return
    }

    ensureEngineRunning()

    val projectPath = System.getProperty("user.dir")
    val projectName = extractProjectName(projectPath)

    println("📦 Registering project: $projectName")
    val request = RegisterProjectRequest(name = projectName, path = projectPath)
    engineCommandClient.registerProject(request)

    if (command == null || command == "tasks") {
      val tasks = engineCommandClient.getAllTasks(projectName)
      printTasks(tasks)
      return
    }

    if (command == "info") {
      printInfo(projectName, projectPath, engineCommandClient.getAllTasks(projectName))
      return
    }

    if (command == "plan") {
      val taskName = args.getOrNull(1)
      if (taskName == null) {
        println("Usage: architect plan <task>")
        exitProcess(1)
      }
      printPlan(engineCommandClient.planTask(projectName, taskName))
      return
    }

    if (command == "validate") {
      val validation = engineCommandClient.validateProject(projectName)
      printValidation(projectName, validation)
      if (!validation.valid) exitProcess(1)
      return
    }

    // Drop first arg as it's the command itself (included by PicoCLI)
    val taskArgs = if (args.isNotEmpty()) args.drop(1) else emptyList()

    // Watch mode for engine-backed execution
    val watchTask = if (command == "watch") args.getOrNull(1) else if (watch) command else null
    if (watchTask != null) {
      runWatchMode(projectPath, watchTask) {
        executeTask(projectName, watchTask, taskArgs)
      }
      return
    }

    executeTask(projectName, command!!, taskArgs)
  }

  private fun runEmbeddedMode() {
    val projectPath = System.getProperty("user.dir")
    val projectName = extractProjectName(projectPath)

    if (!plain) {
      println("⚙️  Using embedded mode")
    }

    if (command == null || command == "tasks") {
      val tasks = embeddedTaskExecutor.listTasks(projectName, projectPath)
      printTasks(tasks)
      return
    }

    if (command == "info") {
      printInfo(projectName, projectPath, embeddedTaskExecutor.listTasks(projectName, projectPath))
      return
    }

    if (command == "plan") {
      val taskName = args.getOrNull(1)
      if (taskName == null) {
        println("Usage: architect plan <task>")
        exitProcess(1)
      }
      printPlan(embeddedTaskExecutor.plan(projectName, projectPath, taskName))
      return
    }

    if (command == "validate") {
      val validation = embeddedTaskExecutor.validate(projectName, projectPath)
      printValidation(projectName, validation)
      if (!validation.valid) exitProcess(1)
      return
    }

    // Drop first arg as it's the command itself (included by PicoCLI)
    val taskArgs = if (args.isNotEmpty()) args.drop(1) else emptyList()

    // Watch mode: re-run task on file changes
    val watchTask = if (command == "watch") args.getOrNull(1) else if (watch) command else null
    if (watchTask != null) {
      runWatchMode(projectPath, watchTask) {
        executeTaskEmbedded(projectName, projectPath, watchTask, taskArgs)
      }
      return
    }

    executeTaskEmbedded(projectName, projectPath, command!!, taskArgs)
  }

  /**
   * Ensures the engine daemon is running. If it is not running and `--no-daemon` is false,
   * resolves the engine binary (checking `~/.architect/bin/` first, then PATH), starts it,
   * and waits up to `architect.engine.startup-timeout-seconds` for it to become ready.
   */
  private fun ensureEngineRunning() {
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

  /**
   * Resolves the engine binary path.
   * Checks `~/.architect/bin/architect-engine` first, then falls back to `architect-engine` on PATH.
   */
  internal fun resolveEngineBinary(): String? {
    val home = System.getProperty("user.home") ?: return null
    val localBin = File("$home/.architect/bin/architect-engine")
    if (localBin.exists() && localBin.canExecute()) return localBin.absolutePath

    // Fall back to PATH lookup
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
    println("▶  Executing task: $taskName")
    println("📦 Project: $projectName")
    println("━".repeat(80))
    println()
    
    runBlocking {
      val startTime = System.currentTimeMillis()
      try {
        val executionId = engineCommandClient.execute(projectName, taskName, taskArgs)
        val flow = engineCommandClient.getExecutionFlow(executionId)
        flow.collect { ui.process(it) }

        val duration = System.currentTimeMillis() - startTime
        if (ui.hasFailed) {
          ui.completeWithError("Task failed (${ConsoleUI.formatDuration(duration)})")
          exitProcess(1)
        } else {
          ui.complete("Task completed successfully (${ConsoleUI.formatDuration(duration)})")
          exitProcess(0)
        }
      } catch (e: Exception) {
        val duration = System.currentTimeMillis() - startTime
        println()
        println("❌ Task execution aborted")
        println("Error: ${e.message}")
        println()
        println("Duration: ${ConsoleUI.formatDuration(duration)}")
        exitProcess(1)
      }
    }
  }

  private fun executeTaskEmbedded(
      projectName: String,
      projectPath: String,
      taskName: String,
      taskArgs: List<String>,
  ) {
    val ui = EmbeddedConsoleUI(taskName, plain)

    println()
    println("━".repeat(80))
    println("▶  Executing task: $taskName")
    println("📦 Project: $projectName")
    println("━".repeat(80))
    println()

    val startTime = System.currentTimeMillis()
    try {
      val result = embeddedTaskExecutor.execute(
          projectName = projectName,
          projectPath = projectPath,
          taskName = taskName,
          taskArgs = taskArgs,
      ) { event ->
        ui.process(event)
      }

      val duration = System.currentTimeMillis() - startTime
      if (!result.success || ui.hasFailed) {
        ui.completeWithError("Task failed (${ConsoleUI.formatDuration(duration)})")
        exitProcess(1)
      } else {
        ui.complete("Task completed successfully (${ConsoleUI.formatDuration(duration)})")
        exitProcess(0)
      }
    } catch (e: Exception) {
      val duration = System.currentTimeMillis() - startTime
      println()
      println("❌ Task execution aborted")
      println("Error: ${e.message}")
      println()
      println("Duration: ${ConsoleUI.formatDuration(duration)}")
      exitProcess(1)
    }
  }

  /**
   * Watch mode: monitors the project directory for file changes and re-runs the task.
   * Handles Ctrl+C for clean exit via shutdown hook.
   */
  private fun runWatchMode(projectPath: String, taskName: String, executeBlock: () -> Unit) {
    println("👀 Watch mode: $taskName")
    println("   Watching for changes in $projectPath ...")
    println("   Press Ctrl+C to stop")
    println()

    // First execution
    try { executeBlock() } catch (_: Exception) { /* allow re-run on next change */ }

    val watchService = io.github.architectplatform.engine.core.watch.FileWatchService(
        rootPath = java.nio.file.Paths.get(projectPath),
        debounceMs = 500,
    ) { changedPath ->
      println()
      println("━".repeat(80))
      println("🔄 File changed: $changedPath — re-running $taskName")
      println("━".repeat(80))
      println()
      try { executeBlock() } catch (_: Exception) { /* continue watching */ }
    }

    // Clean shutdown on Ctrl+C
    Runtime.getRuntime().addShutdownHook(Thread {
      watchService.stop()
      println()
      println("👋 Watch mode stopped")
    })

    watchService.start() // blocks until stop() is called
  }

  private fun handlePluginCommand() {
    val subCommand = args.getOrNull(1)
    when (subCommand) {
      "search" -> {
        val query = args.getOrNull(2)
        if (query == null) {
          println("Usage: architect plugin search <query>")
          exitProcess(1)
        }
        val registryUrl = DEFAULT_REGISTRY_URL
        try {
          val fetcher = io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher()
          val mapper = com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
          val registryJson = fetcher.fetchText(registryUrl)
          val registry = mapper.readValue(registryJson, io.github.architectplatform.engine.core.plugin.infra.PluginRegistry::class.java)
          val lowerQuery = query.lowercase()
          val results = registry.plugins.filter {
            it.id.lowercase().contains(lowerQuery) ||
              (it.description?.lowercase()?.contains(lowerQuery) == true)
          }
          if (results.isEmpty()) {
            println("No plugins found matching '$query'")
            return
          }
          if (json) {
            println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results))
          } else {
            println()
            println("━".repeat(60))
            println("🔍 Plugin Search: $query")
            println("━".repeat(60))
            val fmt = "  %-30s  %-10s  %s"
            println(fmt.format("ID", "VERSION", "DESCRIPTION"))
            println("  ${"─".repeat(56)}")
            results.forEach { p ->
              println(fmt.format(p.id.take(30), p.version.take(10), (p.description ?: "").take(20)))
            }
            println()
          }
        } catch (e: Exception) {
          println("Failed to search registry: ${e.message}")
          exitProcess(1)
        }
      }
      "install" -> {
        val pluginId = args.getOrNull(2)
        if (pluginId == null) {
          println("Usage: architect plugin install <plugin-id>")
          exitProcess(1)
        }
        val projectPath = System.getProperty("user.dir")
        val configFile = java.io.File(projectPath, "architect.yml")
        if (!configFile.exists()) {
          println("No architect.yml found in $projectPath")
          exitProcess(1)
        }
        val content = configFile.readText()
        val pluginEntry = "\n  - name: $pluginId"
        if (content.contains("plugins:")) {
          configFile.writeText(content.replaceFirst("plugins:", "plugins:$pluginEntry"))
        } else {
          configFile.appendText("\nplugins:$pluginEntry\n")
        }
        println("✅ Added plugin '$pluginId' to architect.yml")
      }
      else -> {
        println("Usage: architect plugin <search|install> [args]")
        println()
        println("Commands:")
        println("  search <query>       Search the plugin registry")
        println("  install <plugin-id>  Add a plugin to architect.yml")
        exitProcess(1)
      }
    }
  }

  private fun printPlan(plan: TaskPlanDTO) {
    println()
    println("━".repeat(80))
    println("📋 Execution Plan: ${plan.task}")
    println("📦 Project: ${plan.project}")
    println("━".repeat(80))
    println()
    println("  ${plan.totalSteps} task(s) across ${plan.parallelBatches} parallel batch(es)")
    println()

    val byBatch = plan.steps.groupBy { it.batch }.toSortedMap()
    byBatch.forEach { (batchIdx, tasks) ->
      val batchLabel = if (tasks.size > 1)
        "Batch $batchIdx — running ${tasks.size} tasks in parallel"
      else
        "Batch $batchIdx"
      println("  ┌─ $batchLabel")
      tasks.forEachIndexed { i, step ->
        val connector = if (i == tasks.size - 1) "└──" else "├──"
        val phase = if (step.phase != null) " [${step.phase}]" else ""
        val deps = if (step.depends.isNotEmpty()) " ← ${step.depends.joinToString(", ")}" else ""
        println("  │  $connector ${step.id}$phase  ${step.description}$deps")
      }
      println("  │")
    }
    println()
  }

  private fun printValidation(projectName: String, result: ValidationResultDTO) {
    println()
    println("━".repeat(80))
    val status = if (result.valid) "✅ VALID" else "❌ INVALID"
    println("$status — Project: $projectName")
    println("━".repeat(80))
    if (result.errors.isEmpty() && result.warnings.isEmpty()) {
      println("  No issues found.")
    }
    result.errors.forEach { println("  ❌ ERROR:   $it") }
    result.warnings.forEach { println("  ⚠️  WARNING: $it") }
    println()
  }

  private fun printHistory(records: List<HistoryRecordDTO>) {
    if (records.isEmpty()) {
      println("No execution history found.")
      return
    }
    println()
    println("━".repeat(80))
    println("📜 Execution History")
    println("━".repeat(80))
    val fmt = "%-8s  %-18s  %-14s  %-7s  %s"
    println(fmt.format("STATUS", "WHEN", "PROJECT", "DURATION", "TASK"))
    println("─".repeat(80))
    records.forEach { r ->
      val status = if (r.success) "✅" else "❌"
      val when_ = java.time.Instant.ofEpochMilli(r.timestamp)
          .atZone(java.time.ZoneId.systemDefault())
          .format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))
      val duration = "${r.durationMs / 1000}.${(r.durationMs % 1000) / 100}s"
      println(fmt.format(status, when_, r.project, duration, r.task))
    }
    println()
  }

  private fun printTasks(tasks: List<io.github.architectplatform.cli.dto.TaskDTO>) {
    var filtered = tasks
    if (filter != null) {
      val f = filter!!.uppercase()
      filtered = tasks.filter { it.phase?.uppercase() == f }
    }

    if (json) {
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
          .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
      println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(filtered))
      return
    }

    println()
    println("━".repeat(80))
    println("🧭 Available Tasks" + if (filter != null) " (phase: ${filter!!.uppercase()})" else "")
    println("━".repeat(80))
    val fmt = "  %-30s  %-12s  %s"
    println(fmt.format("TASK", "PHASE", "DESCRIPTION"))
    println("  ${"─".repeat(76)}")
    filtered.forEach { t ->
      println(fmt.format(t.id.take(30), (t.phase ?: "—").take(12), t.description.take(34)))
    }
    println()
    println("  ${filtered.size} task(s) available")
    println()
  }

  private fun printInfo(projectName: String, projectPath: String, tasks: List<io.github.architectplatform.cli.dto.TaskDTO>) {
    if (json) {
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
          .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
      val info = mapOf(
          "project" to projectName,
          "path" to projectPath,
          "taskCount" to tasks.size,
          "tasks" to tasks,
          "phases" to tasks.mapNotNull { it.phase }.distinct().sorted(),
      )
      println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(info))
      return
    }

    println()
    println("━".repeat(80))
    println("ℹ️  Project Info")
    println("━".repeat(80))
    println("  Name:   $projectName")
    println("  Path:   $projectPath")
    println("  Tasks:  ${tasks.size}")
    val phases = tasks.mapNotNull { it.phase }.distinct().sorted()
    if (phases.isNotEmpty()) {
      println("  Phases: ${phases.joinToString(", ")}")
    }
    println()
  }

  private fun printVersion() {
    val cliVersion = javaClass.`package`?.implementationVersion ?: "dev"
    if (json) {
      println("""{"cli":"$cliVersion"}""")
      return
    }
    println("architect $cliVersion")
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
        println("Installing Architect Engine (optional for embedded mode)...")
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
   * Executes a shell command using ProcessBuilder.
   *
   * @param command The shell command to execute
   * @param wait If true, waits for the command to complete before returning
   */
  private fun execute(command: String, wait: Boolean = true) {
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

  companion object {
    private const val DEFAULT_REGISTRY_URL = "https://registry.architect.dev/registry.json"

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
