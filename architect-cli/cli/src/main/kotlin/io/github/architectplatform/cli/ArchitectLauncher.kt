package io.github.architectplatform.cli

import io.github.architectplatform.cli.client.EngineCommandClient
import io.github.architectplatform.cli.dto.HistoryRecordDTO
import io.github.architectplatform.cli.dto.RegisterProjectRequest
import io.github.architectplatform.cli.history.LocalHistoryReader
import io.github.architectplatform.cli.embedded.EmbeddedTaskExecutor
import io.github.architectplatform.cli.plugin.PluginDocumentationGenerator
import io.github.architectplatform.cli.plugin.PluginJarValidator
import io.github.architectplatform.cli.plugin.PluginScaffolder
import io.github.architectplatform.cli.plugin.PluginTemplate
import io.github.architectplatform.cli.dto.TaskPlanDTO
import io.github.architectplatform.cli.dto.ValidationResultDTO
import io.github.architectplatform.cli.engine.EngineHealthChecker
import io.github.architectplatform.cli.graph.TaskGraphDotRenderer
import io.github.architectplatform.cli.graph.TaskGraphHtmlRenderer
import io.github.architectplatform.engine.core.execution.EmbeddedExecutionContext
import io.github.architectplatform.engine.core.project.app.AffectedProjectResolver
import io.github.architectplatform.engine.core.tasks.application.LocalOutputCache
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import java.awt.Desktop
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
  private val pluginScaffolder = PluginScaffolder()
  private val pluginDocumentationGenerator = PluginDocumentationGenerator()
  private val pluginJarValidator = PluginJarValidator()
  private val taskGraphDotRenderer = TaskGraphDotRenderer()
  private val taskGraphHtmlRenderer = TaskGraphHtmlRenderer()

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

  @CommandLine.Option(
      names = ["--affected"],
      description = ["Execute tasks only for projects affected by changes since base ref"],
      defaultValue = "false",
  )
  var affected: Boolean = false

  @CommandLine.Option(
      names = ["--base"],
      description = ["Base git ref for affected detection (default: HEAD~1)"],
  )
  var baseRef: String? = null

  @CommandLine.Option(
      names = ["--no-cache"],
      description = ["Bypass task output cache for this run"],
      defaultValue = "false",
  )
  var noCache: Boolean = false

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
    embeddedTaskExecutor.outputCacheEnabled = !noCache

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

    if (command == "cache") {
      handleCacheCommand()
      return
    }

    if (command == "affected") {
      val projectPath = System.getProperty("user.dir")
      val projectName = extractProjectName(projectPath)
      val affectedProjects = resolveAffectedProjects(projectName, projectPath)
      printAffected(affectedProjects)
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

    if (command == "graph") {
      val graphOptions = parseGraphOptions(args)
      val tasks = engineCommandClient.getAllTasks(projectName)
      val plans = tasks.map { engineCommandClient.planTask(projectName, it.id) }
      if (graphOptions.open) {
        openGraph(projectName, plans)
      } else {
        printGraph(projectName, plans)
      }
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
        val watchAffectedProjects =
          if (affected) {
            val resolved = resolveAffectedProjects(projectName, projectPath)
            if (resolved.isEmpty()) {
              println("✅ No projects affected — nothing to run.")
              return@runWatchMode
            }
            if (!plain) {
              println("🎯 Affected projects: ${resolved.joinToString(", ")}")
            }
            resolved
          } else {
            emptySet()
          }
        executeTask(
          projectName,
          watchTask,
          augmentTaskArgsForExecution(watchTask, taskArgs, watchAffectedProjects),
        )
      }
      return
    }

    val affectedProjects = if (affected) {
      val resolved = resolveAffectedProjects(projectName, projectPath)
      if (resolved.isEmpty()) {
        println("✅ No projects affected — nothing to run.")
        return
      }
      if (!plain) {
        println("🎯 Affected projects: ${resolved.joinToString(", ")}")
      }
      resolved
    } else {
      emptySet()
    }

    executeTask(projectName, command!!, augmentTaskArgsForExecution(command!!, taskArgs, affectedProjects))
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

    if (command == "graph") {
      val graphOptions = parseGraphOptions(args)
      val tasks = embeddedTaskExecutor.listTasks(projectName, projectPath)
      val plans = tasks.map { embeddedTaskExecutor.plan(projectName, projectPath, it.id) }
      if (graphOptions.open) {
        openGraph(projectName, plans)
      } else {
        printGraph(projectName, plans)
      }
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
        val watchAffectedProjects =
          if (affected) {
            val resolved = resolveAffectedProjects(projectName, projectPath)
            if (resolved.isEmpty()) {
              println("✅ No projects affected — nothing to run.")
              return@runWatchMode
            }
            if (!plain) {
              println("🎯 Affected projects: ${resolved.joinToString(", ")}")
            }
            resolved
          } else {
            emptySet()
          }
        executeTaskEmbedded(
          projectName,
          projectPath,
          watchTask,
          augmentTaskArgsForExecution(watchTask, taskArgs, watchAffectedProjects),
        )
      }
      return
    }

    val affectedProjects = if (affected) {
      val resolved = resolveAffectedProjects(projectName, projectPath)
      if (resolved.isEmpty()) {
        println("✅ No projects affected — nothing to run.")
        return
      }
      if (!plain) {
        println("🎯 Affected projects: ${resolved.joinToString(", ")}")
      }
      resolved
    } else {
      emptySet()
    }

    executeTaskEmbedded(
      projectName,
      projectPath,
      command!!,
      augmentTaskArgsForExecution(command!!, taskArgs, affectedProjects),
    )
  }

  /**
   * Resolves which projects are affected by changes since the configured base ref.
   */
  private fun resolveAffectedProjects(projectName: String, projectPath: String): Set<String> {
    val context = EmbeddedExecutionContext.create(
      remoteContentFetcher = io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher(),
      activeProfile = embeddedTaskExecutor.activeProfile,
    )
    context.projectService.registerProject(projectName, projectPath)
    val project = context.projectService.getProject(projectName)
      ?: return emptySet()

    val graph = context.projectService.buildDependencyGraph(projectName)
    val affectedConfig = AffectedProjectResolver.parseConfig(
      project.context.config["project"] as? Map<String, Any>
    )
    val resolver = AffectedProjectResolver()

    // Phase 17: when output caching is enabled, filter out projects whose cached outputs
    // are still valid. Uses a project-level content hash (SHA-256 of all source files)
    // stored in the local output cache to skip projects that would produce no-op re-runs.
    if (!noCache) {
      val outputCache = LocalOutputCache()
      resolver.cacheValidator = { projects ->
        projects.filterTo(mutableSetOf()) { projectName ->
          val subProject = graph.projects
            .firstOrNull { it == projectName }
          if (subProject == null) true  // unknown project – keep it
          else !outputCache.contains("project:$projectName")
        }
      }
    }

    return resolver.resolve(
      root = project,
      graph = graph,
      baseRef = baseRef ?: "HEAD~1",
      config = affectedConfig,
    )
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

  internal fun augmentTaskArgsForExecution(
      taskName: String,
      taskArgs: List<String>,
      affectedProjects: Set<String> = emptySet(),
  ): List<String> {
    if (!affected || !taskName.startsWith("nx-")) {
      return taskArgs
    }

    val augmentedArgs = taskArgs.toMutableList()
    if (augmentedArgs.none { it == "--architect-affected" }) {
      augmentedArgs += "--architect-affected"
    }
    if (affectedProjects.isNotEmpty() && augmentedArgs.none { it.startsWith("--architect-projects=") }) {
      augmentedArgs += "--architect-projects=${affectedProjects.sorted().joinToString(",")}"
    }
    return augmentedArgs
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
      "validate" -> {
        val pluginPath = args.getOrNull(2)
        if (pluginPath == null) {
          println("Usage: architect plugin validate <path>")
          exitProcess(1)
        }

        try {
          val validation = pluginJarValidator.validate(java.nio.file.Path.of(pluginPath))
          if (json) {
            val mapper = com.fasterxml.jackson.databind.ObjectMapper()
              .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
            println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(validation))
          } else {
            val statusIcon = if (validation.valid) "✅" else "❌"
            println("$statusIcon Plugin validation ${if (validation.valid) "passed" else "failed"} for ${validation.jarPath}")
            println("SPI providers: ${validation.spiImplementations.size}")
            validation.plugins.forEach { plugin ->
              println("- ${plugin.pluginId} (${plugin.className})")
              println("  contextKey=${plugin.contextKey}, ctxClass=${plugin.contextClass}, config=${if (plugin.configDeserializationValid) "ok" else "failed"}")
            }
            if (validation.warnings.isNotEmpty()) {
              println("Warnings:")
              validation.warnings.forEach { println("- $it") }
            }
            if (validation.errors.isNotEmpty()) {
              println("Errors:")
              validation.errors.forEach { println("- $it") }
            }
          }
          if (!validation.valid) {
            exitProcess(1)
          }
        } catch (e: Exception) {
          println("Failed to validate plugin JAR: ${e.message}")
          exitProcess(1)
        }
      }
      "docs" -> {
        val pluginPath = args.getOrNull(2)
        if (pluginPath == null) {
          println("Usage: architect plugin docs <path>")
          exitProcess(1)
        }

        try {
          val generatedDocs = pluginDocumentationGenerator.generate(java.nio.file.Path.of(pluginPath))
          if (json) {
            val mapper = com.fasterxml.jackson.databind.ObjectMapper()
              .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
            println(
              mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                mapOf(
                  "plugin" to generatedDocs.manifest.name,
                  "template" to generatedDocs.manifest.template,
                  "outputPath" to generatedDocs.outputPath.toString(),
                  "tasks" to generatedDocs.tasks.map { it.id },
                ),
              ),
            )
          } else {
            println("✅ Generated plugin reference doc at ${generatedDocs.outputPath}")
          }
        } catch (e: Exception) {
          println("Failed to generate plugin docs: ${e.message}")
          exitProcess(1)
        }
      }
      "create" -> {
        val pluginName = args.getOrNull(2)
        if (pluginName == null) {
          println("Usage: architect plugin create <name> [template|--template <template>]")
          exitProcess(1)
        }

        val template = resolvePluginTemplate(args)
        if (template == null) {
          println("Unknown template. Supported templates: kotlin, typescript, go")
          exitProcess(1)
        }

        try {
          val scaffoldPath = pluginScaffolder.scaffold(
            name = pluginName,
            template = template,
            targetRoot = java.nio.file.Path.of(System.getProperty("user.dir")),
          )
          println("✅ Created ${template.id} plugin scaffold at $scaffoldPath")
        } catch (e: Exception) {
          println("Failed to create plugin scaffold: ${e.message}")
          exitProcess(1)
        }
      }
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
        println("Usage: architect plugin <create|docs|validate|search|install> [args]")
        println()
        println("Commands:")
        println("  docs <path>              Generate PLUGIN_REFERENCE.md from plugin metadata")
        println("  validate <path>          Validate a plugin JAR and its SPI/config wiring")
        println("  create <name> [template]  Scaffold a new plugin (kotlin, typescript, go)")
        println("  search <query>       Search the plugin registry")
        println("  install <plugin-id>  Add a plugin to architect.yml")
        exitProcess(1)
      }
    }
  }

  private fun resolvePluginTemplate(arguments: List<String>): PluginTemplate? {
    var positionalTemplate: String? = null
    var index = 3
    while (index < arguments.size) {
      val argument = arguments[index]
      when {
        argument == "--template" -> {
          return arguments.getOrNull(index + 1)?.let(PluginTemplate::from)
        }
        argument.startsWith("--template=") -> {
          return PluginTemplate.from(argument.substringAfter('='))
        }
        !argument.startsWith("--") && positionalTemplate == null -> {
          positionalTemplate = argument
        }
      }
      index += 1
    }

    return PluginTemplate.from(positionalTemplate ?: "kotlin")
  }

  private fun handleCacheCommand() {
    val cache = io.github.architectplatform.engine.core.tasks.application.LocalOutputCache()
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

  private fun printGraph(projectName: String, plans: List<TaskPlanDTO>) {
    println(taskGraphDotRenderer.render(projectName, plans))
  }

  private fun openGraph(projectName: String, plans: List<TaskPlanDTO>) {
    val outputPath = taskGraphHtmlRenderer.writeTempFile(projectName, plans)
    println("📈 Graph page: $outputPath")
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
      Desktop.getDesktop().browse(outputPath.toUri())
    } else {
      println("Browser opening is not supported in this environment. Open the HTML file manually.")
    }
  }

  private fun parseGraphOptions(arguments: List<String>): GraphOptions {
    val options = arguments.drop(1)
    val invalid = options.filter { it != "--open" }
    if (invalid.isNotEmpty()) {
      println("Usage: architect graph [--open]")
      exitProcess(1)
    }
    return GraphOptions(open = options.contains("--open"))
  }

  private data class GraphOptions(
    val open: Boolean,
  )

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

  private fun printAffected(affectedProjects: Set<String>) {
    if (json) {
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
          .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
      println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(affectedProjects))
      return
    }
    if (affectedProjects.isEmpty()) {
      println("✅ No projects affected.")
      return
    }
    println()
    println("━".repeat(80))
    println("🎯 Affected Projects (base: ${baseRef ?: "HEAD~1"})")
    println("━".repeat(80))
    affectedProjects.sorted().forEach { println("  • $it") }
    println()
    println("  ${affectedProjects.size} project(s) affected")
    println()
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
