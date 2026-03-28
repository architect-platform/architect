package io.github.architectplatform.cli

import io.github.architectplatform.cli.client.EngineCommandClient
import io.github.architectplatform.cli.command.CacheCommandHandler
import io.github.architectplatform.cli.command.CheckCommandHandler
import io.github.architectplatform.cli.command.CliInfrastructureHandler
import io.github.architectplatform.cli.command.ConfigCommandHandler
import io.github.architectplatform.cli.command.ConventionsCommandHandler
import io.github.architectplatform.cli.command.DoctorCommandHandler
import io.github.architectplatform.cli.command.EngineCommandHandler
import io.github.architectplatform.cli.command.HelpCommandHandler
import io.github.architectplatform.cli.command.InitCommandHandler
import io.github.architectplatform.cli.command.OutputFormatter
import io.github.architectplatform.cli.command.PluginCommandHandler
import io.github.architectplatform.cli.command.SchemaCommandHandler
import io.github.architectplatform.cli.command.SecretCommandHandler
import io.github.architectplatform.cli.dto.TaskDTO
import io.github.architectplatform.cli.dto.RegisterProjectRequest
import io.github.architectplatform.cli.history.LocalHistoryReader
import io.github.architectplatform.cli.embedded.EmbeddedTaskExecutor
import io.github.architectplatform.cli.embedded.MultiProjectOrchestrator
import io.github.architectplatform.cli.engine.EngineHealthChecker
import io.github.architectplatform.core.execution.EmbeddedExecutionContext
import io.github.architectplatform.core.project.app.AffectedProjectResolver
import io.github.architectplatform.core.project.app.VersionConstraint
import io.github.architectplatform.core.tasks.application.LocalOutputCache
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

/**
 * Main entry point for the Architect CLI application.
 *
 * Dispatches commands to focused handler classes:
 * - [EngineCommandHandler] for engine lifecycle
 * - [PluginCommandHandler] for plugin management
 * - [CacheCommandHandler] for cache management
 * - [CheckCommandHandler] for precondition checks
 * - [DoctorCommandHandler] for environment diagnostics
 * - [CliInfrastructureHandler] for completion/upgrade
 * - [OutputFormatter] for all display/formatting
 *
 * Task execution and watch mode remain in this class due to tight coupling with CLI state.
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

  // Command handlers
  private val engineHandler = EngineCommandHandler(engineCommandClient, engineHealthChecker, ::extractProjectName)
  private val pluginHandler = PluginCommandHandler()
  private val cacheHandler = CacheCommandHandler()
  private val checkHandler = CheckCommandHandler(embeddedTaskExecutor, ::extractProjectName)
  private val cliHandler = CliInfrastructureHandler(engineCommandClient, ::extractProjectName)
  private val helpHandler = HelpCommandHandler()
  internal var secretHandler = SecretCommandHandler()
  private val initHandler = InitCommandHandler()
  private val configHandler = ConfigCommandHandler()
  private val conventionsHandler = ConventionsCommandHandler()
  private val doctorHandler = DoctorCommandHandler(engineHealthChecker)
  private val schemaHandler = SchemaCommandHandler()
  private val output = OutputFormatter()
  private val multiProjectOrchestrator = MultiProjectOrchestrator(embeddedTaskExecutor)

  // Graceful cancellation state
  @Volatile private var cancelRequested = false
  @Volatile private var activeExecutionId: String? = null

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

  @CommandLine.Option(
      names = ["-p", "--plain"],
      description = ["Enable plain output (CI Environments)"],
      defaultValue = "false",
  )
  var plain: Boolean = false

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

  @CommandLine.Option(
      names = ["--verbose"],
      description = ["Set verbosity level: 1=tasks (default), 2=details+stdout, 3=debug"],
      defaultValue = "1",
      arity = "0..1",
      fallbackValue = "2",
  )
  var verbosity: Int = 1

  @CommandLine.Option(
      names = ["-q", "--quiet"],
      description = ["Quiet mode: only show summary and failures"],
      defaultValue = "false",
  )
  var quiet: Boolean = false

  @CommandLine.Option(
      names = ["--dry-run"],
      description = ["Show execution plan without running tasks"],
      defaultValue = "false",
  )
  var dryRun: Boolean = false

  @CommandLine.Option(
      names = ["--timing"],
      description = ["Show detailed task execution timing breakdown"],
      defaultValue = "false",
  )
  var timing: Boolean = false

  @CommandLine.Option(
      names = ["--parallel"],
      description = ["Control task parallelism (0=unlimited, 1=sequential)"],
      defaultValue = "-1",
  )
  var parallel: Int = -1

  @CommandLine.Option(
      names = ["--output", "-o"],
      description = ["Save execution output to file"],
  )
  var outputFile: String? = null

  @CommandLine.Option(
      names = ["--tee"],
      description = ["Save output to file while also displaying on terminal"],
      defaultValue = "false",
  )
  var tee: Boolean = false

  @CommandLine.Option(
      names = ["--all"],
      description = ["Execute the task across all discovered subprojects in dependency order"],
      defaultValue = "false",
  )
  var all: Boolean = false

  override fun run() {
    if (noColor || System.getenv("NO_COLOR") != null || System.getenv("CI") != null) {
      plain = true
    }
    if (quiet) verbosity = 0

    val resolvedProfile = io.github.architectplatform.core.project.app.ProfileMerger.detectProfile(envProfile)
    embeddedTaskExecutor.activeProfile = resolvedProfile
    embeddedTaskExecutor.outputCacheEnabled = !noCache

    // Sync handler options
    syncHandlerOptions()

    // Install Ctrl+C graceful cancellation handler
    Runtime.getRuntime().addShutdownHook(Thread {
      if (!cancelRequested && activeExecutionId != null) {
        cancelRequested = true
        System.err.println("\n⚠️  Cancelling... (press Ctrl+C again to force)")
        activeExecutionId?.let { execId ->
          try { engineCommandClient.cancelExecution(execId) } catch (_: Exception) {}
        }
      }
    })

    // Resolve command aliases from architect.yml
    command = resolveAlias(command)

    if (version) {
      output.printVersion()
      return
    }

    if (command != "upgrade") {
      warnIfVersionMismatch(resolvedProfile)
    }

    when (command) {
      "help" -> { helpHandler.handle(args); return }
      "init" -> { initHandler.handle(args); return }
      "engine" -> { engineHandler.handle(args); return }
      "plugin" -> { pluginHandler.handle(args); return }
      "cache" -> { cacheHandler.handle(args); return }
      "secret" -> { secretHandler.handle(args); return }
      "completion" -> { cliHandler.handleCompletion(args, this); return }
      "upgrade" -> { cliHandler.handleUpgrade(args); return }
      "check" -> { checkHandler.handle(args); return }
      "doctor" -> { doctorHandler.handle(args); return }
      "config" -> { configHandler.handle(args); return }
      "conventions" -> { conventionsHandler.handle(args); return }
      "retry" -> { handleRetry(); return }
      "history" -> { handleHistory(); return }
      "stats" -> { handleStats(); return }
      "affected" -> { handleAffectedCommand(); return }
      "status" -> { handleStatusCommand(); return }
      "schema" -> { handleSchemaCommand(); return }
    }

    if (command == "tasks" || command == null) {
      pluginHandler.maybeWarnOutdatedPlugins()
    }

    val useEmbeddedExecution = embedded || (noDaemon && !engineHealthChecker.isRunning())

    // --output / --tee: redirect stdout to file
    setupOutputRedirection()

    // --dry-run: show execution plan without running
    if (dryRun && command != null && command !in listOf("tasks", "info", "plan", "graph", "validate")) {
      val projectPath = System.getProperty("user.dir")
      val projectName = extractProjectName(projectPath)
      println()
      println("━".repeat(80))
      println("🔍 DRY RUN — showing execution plan for: $command")
      println("📦 Project: $projectName")
      println("━".repeat(80))
      println()
      val plan = if (useEmbeddedExecution) {
        embeddedTaskExecutor.plan(projectName, projectPath, command!!)
      } else {
        engineHandler.ensureEngineRunning()
        val request = RegisterProjectRequest(name = projectName, path = projectPath)
        engineCommandClient.registerProject(request)
        engineCommandClient.planTask(projectName, command!!)
      }
      output.printPlanTree(plan)
      println()
      println("ℹ️  No tasks were executed (--dry-run mode)")
      return
    }

    if (useEmbeddedExecution) {
      runEmbeddedMode()
      return
    }

    engineHandler.ensureEngineRunning()

    val projectPath = System.getProperty("user.dir")
    val projectName = extractProjectName(projectPath)

    println("📦 Registering project: $projectName")
    val request = RegisterProjectRequest(name = projectName, path = projectPath)
    engineCommandClient.registerProject(request)
    cliHandler.cacheProjectName(projectName)

    when (command) {
      "tasks" -> {
        output.printTasks(listEngineTasks(projectName, projectPath))
        return
      }
      null -> {
        val tasks = listEngineTasks(projectName, projectPath)
        val selector = InteractiveTaskSelector(plain)
        val selected = selector.select(tasks)
        if (selected == null) {
          // Non-interactive environment or user cancelled — fall back to plain task list
          output.printTasks(tasks)
          return
        }
        // User chose a task interactively — execute it
        command = selected
      }
      "info" -> {
        output.printInfo(projectName, projectPath, listEngineTasks(projectName, projectPath))
        return
      }
      "plan" -> {
        val planOptions = output.parsePlanOptions(args)
        if (planOptions.taskName == null) {
          println("Usage: architect plan <task> [--tree]")
          exitProcess(1)
        }
        val plan = engineCommandClient.planTask(projectName, planOptions.taskName)
        if (planOptions.tree) output.printPlanTree(plan) else output.printPlan(plan)
        return
      }
      "graph" -> {
        handleGraphCommand(projectName, projectPath, engine = true)
        return
      }
      "validate" -> {
        val validateArgs = if (args.isNotEmpty()) args.drop(1) else emptyList()
        if (validateArgs.contains("--structure")) {
          executeTask(projectName, "architecture-validate", listOf("--structure"))
        }
        val validation = engineCommandClient.validateProject(projectName)
        output.printValidation(projectName, validation)
        if (!validation.valid) exitProcess(1)
        return
      }
    }

    val taskArgs = if (args.isNotEmpty()) args.drop(1) else emptyList()

    val watchTask = if (command == "watch") args.getOrNull(1) else if (watch) command else null
    if (watchTask != null) {
      runWatchMode(projectPath, watchTask) {
        val watchAffectedProjects = resolveAffectedIfEnabled(projectName, projectPath)
          ?: return@runWatchMode
        executeTask(
          projectName,
          watchTask,
          augmentTaskArgsForExecution(watchTask, taskArgs, watchAffectedProjects),
        )
      }
      return
    }

    val affectedProjects = resolveAffectedIfEnabled(projectName, projectPath) ?: return
    executeTask(projectName, command!!, augmentTaskArgsForExecution(command!!, taskArgs, affectedProjects))
  }

  private fun syncHandlerOptions() {
    engineHandler.startupTimeoutSeconds = startupTimeoutSeconds
    engineHandler.plain = plain
    engineHandler.noDaemon = noDaemon
    pluginHandler.json = json
    cacheHandler.json = json
    checkHandler.json = json
    doctorHandler.plain = plain
    configHandler.json = json
    configHandler.plain = plain
    output.json = json
    output.filter = filter
    output.verbosity = verbosity
  }

  /**
   * Sets up output redirection for --output and --tee flags.
   * Returns the FileOutputStream if active, null otherwise.
   */
  private fun setupOutputRedirection(): java.io.FileOutputStream? {
    val filePath = outputFile ?: return null
    val file = java.io.File(filePath)
    file.parentFile?.mkdirs()
    val fileStream = java.io.FileOutputStream(file)
    val printStream = if (tee) {
      // Tee mode: write to both terminal and file
      val teeStream = object : java.io.OutputStream() {
        val original = System.out
        override fun write(b: Int) { original.write(b); fileStream.write(b) }
        override fun write(b: ByteArray) { original.write(b); fileStream.write(b) }
        override fun write(b: ByteArray, off: Int, len: Int) { original.write(b, off, len); fileStream.write(b, off, len) }
        override fun flush() { original.flush(); fileStream.flush() }
      }
      java.io.PrintStream(teeStream, true)
    } else {
      // Output-only: write only to file
      java.io.PrintStream(fileStream, true)
    }
    System.setOut(printStream)
    Runtime.getRuntime().addShutdownHook(Thread {
      System.out.flush()
      fileStream.close()
    })
    return fileStream
  }

  /**
   * Resolves a command alias from the `aliases` section in architect.yml.
   * Falls back to the original command if no alias matches.
   */
  @Suppress("UNCHECKED_CAST")
  private fun resolveAlias(cmd: String?): String? {
    if (cmd == null) return null
    return try {
      val configFile = java.io.File(System.getProperty("user.dir"), "architect.yml")
      if (!configFile.exists()) return cmd
      val yaml = org.yaml.snakeyaml.Yaml()
      val config = yaml.load<Map<String, Any>>(configFile.inputStream()) ?: return cmd
      val aliases = config["aliases"] as? Map<String, String> ?: return cmd
      aliases[cmd] ?: cmd
    } catch (_: Exception) {
      cmd
    }
  }

  private fun listEngineTasks(projectName: String, projectPath: String): List<TaskDTO> {
    val tasks = engineCommandClient.getAllTasks(projectName)
    return applyConfiguredGroups(tasks, projectPath)
  }

  @Suppress("UNCHECKED_CAST")
  private fun applyConfiguredGroups(tasks: List<TaskDTO>, projectPath: String): List<TaskDTO> {
    if (tasks.any { it.groupMembers != null }) return tasks
    val configFile = java.io.File(projectPath, "architect.yml")
    if (!configFile.exists()) return tasks
    val groups = try {
      val yaml = org.yaml.snakeyaml.Yaml()
      val config = yaml.load<Map<String, Any>>(configFile.inputStream()) ?: return tasks
      val rawGroups = config["groups"] as? Map<*, *> ?: return tasks
      linkedMapOf<String, List<String>>().also { resolved ->
        rawGroups.forEach { (rawGroupId, rawMembers) ->
          val groupId = rawGroupId as? String ?: return@forEach
          val memberIds =
            (rawMembers as? List<*>)?.mapNotNull { it as? String }?.distinct().orEmpty()
          if (memberIds.isNotEmpty()) {
            resolved[groupId] = memberIds
          }
        }
      }
    } catch (_: Exception) {
      return tasks
    }

    if (groups.isEmpty()) return tasks
    val tasksById = tasks.associateBy { it.id }
    val groupHeaders = mutableListOf<TaskDTO>()
    val aliasIds = mutableSetOf<String>()
    groups.forEach { (groupId, members) ->
      val groupTask = tasksById[groupId]
      groupHeaders += TaskDTO(
        id = groupId,
        description = groupTask?.description ?: "Task group '$groupId'",
        phase = groupTask?.phase,
        groupMembers = members,
      )
      members.forEach { memberId ->
        if (tasksById.containsKey(memberId)) {
          aliasIds += groupAliasId(groupId, memberId)
        }
      }
    }

    val filteredTasks = tasks.filterNot { it.id in groups.keys || it.id in aliasIds }
    return groupHeaders + filteredTasks
  }

  private fun groupAliasId(groupId: String, memberId: String): String {
    val suffix = when {
      memberId.startsWith("$groupId-") -> memberId.removePrefix("$groupId-")
      memberId.endsWith("-$groupId") -> memberId.removeSuffix("-$groupId")
      else -> memberId
    }.ifBlank { memberId }
    return "$groupId:$suffix"
  }

  private fun handleHistory() {
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
    output.printHistory(records)
  }

  private fun warnIfVersionMismatch(profile: String) {
    val projectPath = System.getProperty("user.dir")
    val constraintValue = try {
      ArchitectVersionConstraintReader.read(projectPath, profile)
    } catch (e: Exception) {
      println("⚠️  Failed to read architect.version constraint: ${e.message}")
      return
    } ?: return
    val currentVersion = CliVersion.current()
    if (currentVersion == "dev") return
    val parsed = try {
      VersionConstraint.parse(constraintValue)
    } catch (e: IllegalArgumentException) {
      println("⚠️  Invalid architect.version constraint '$constraintValue': ${e.message}")
      return
    }
    if (parsed != null && !parsed.isSatisfiedBy(currentVersion)) {
      println(
        "⚠️  Architect CLI $currentVersion does not satisfy architect.version '$constraintValue'. " +
          "Update the CLI or adjust architect.yml.",
      )
    }
  }

  private fun handleStats() {
    val project = args.getOrNull(1)
    val taskId = args.getOrNull(2)
    if (project == null) {
      println("Usage: architect stats <project> [<task>]")
      return
    }
    if (taskId != null) {
      val stats = runCatching { engineCommandClient.getTaskStats(project, taskId) }.getOrNull()
      if (stats == null) {
        println("ℹ️  No performance data found for task '$taskId' in project '$project'")
        return
      }
      output.printTaskStats(stats)
    } else {
      val statsList = runCatching { engineCommandClient.getAllTaskStats(project) }.getOrElse { emptyList() }
      output.printStats(statsList)
    }
  }

  private fun handleRetry() {
    val records = localHistoryReader.getAll().ifEmpty {
      runCatching { engineCommandClient.getHistory() }.getOrElse { emptyList() }
    }
    val lastFailed = records.firstOrNull { !it.success }
    if (lastFailed == null) {
      println("ℹ️  No failed executions found in history")
      return
    }
    val fromTask = args.indexOf("--from").let { if (it >= 0) args.getOrNull(it + 1) else null }
    val taskName = fromTask ?: lastFailed.task
    val projectPath = System.getProperty("user.dir")
    val projectName = lastFailed.project

    println("🔄 Retrying: $taskName (project: $projectName)")
    println()

    val useEmbedded = embedded || (noDaemon && !engineHealthChecker.isRunning())
    if (useEmbedded) {
      executeTaskEmbedded(projectName, projectPath, taskName, lastFailed.args)
    } else {
      engineHandler.ensureEngineRunning()
      val request = RegisterProjectRequest(name = projectName, path = projectPath)
      engineCommandClient.registerProject(request)
      executeTask(projectName, taskName, lastFailed.args)
    }
  }

  private fun handleAffectedCommand() {
    val projectPath = System.getProperty("user.dir")
    val projectName = extractProjectName(projectPath)
    val affectedProjects = resolveAffectedProjects(projectName, projectPath)
    output.printAffected(affectedProjects, baseRef)
  }

  private fun handleStatusCommand() {
    val projectPath = System.getProperty("user.dir")
    val projectName = extractProjectName(projectPath)

    val context = EmbeddedExecutionContext.create(
      remoteContentFetcher = io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher(),
      activeProfile = embeddedTaskExecutor.activeProfile,
    )
    context.projectService.registerProject(projectName, projectPath)
    val root = context.projectService.getProject(projectName)

    val allProjects = if (root != null) {
      fun flatten(p: io.github.architectplatform.core.project.domain.Project): List<io.github.architectplatform.core.project.domain.Project> =
        listOf(p) + p.subProjects.flatMap(::flatten)
      flatten(root)
    } else {
      listOf()
    }

    val cache = LocalOutputCache()

    val projectHealthList = allProjects.map { proj ->
      val validation = try {
        embeddedTaskExecutor.validate(proj.name, proj.path)
      } catch (e: Exception) {
        io.github.architectplatform.cli.dto.ValidationResultDTO(
          valid = false,
          errors = listOf("Failed to validate: ${e.message}"),
          warnings = emptyList(),
        )
      }
      val taskCount = try {
        embeddedTaskExecutor.listTasks(proj.name, proj.path).count { it.groupMembers == null }
      } catch (_: Exception) { 0 }
      val cacheKey = "project:${proj.name}"
      val cached = cache.get(cacheKey)
      val lastBuildAge: Long? = if (cached != null) {
        val cacheDir = java.nio.file.Path.of(System.getProperty("user.home"), ".architect", "cache", cacheKey)
        val resultFile = cacheDir.resolve("result.json").toFile()
        if (resultFile.exists()) {
          (System.currentTimeMillis() - resultFile.lastModified()) / 1000
        } else null
      } else null

      io.github.architectplatform.cli.dto.ProjectHealthDTO(
        name = proj.name,
        path = proj.path,
        valid = validation.valid,
        errors = validation.errors,
        warnings = validation.warnings,
        taskCount = taskCount,
        lastBuildSuccess = cached?.success,
        lastBuildAgeSeconds = lastBuildAge,
      )
    }

    val health = io.github.architectplatform.cli.dto.MonorepoHealthDTO(
      rootProject = projectName,
      projects = projectHealthList,
      healthyCount = projectHealthList.count { it.valid },
      unhealthyCount = projectHealthList.count { !it.valid },
      timestamp = java.time.Instant.now().toString(),
    )

    val jsonFlag = args.contains("--json")
    output.printHealthDashboard(health, jsonFlag)
  }

  private fun handleSchemaCommand() {
    schemaHandler.handle(args)
  }

  private fun handleGraphCommand(projectName: String, projectPath: String, engine: Boolean) {
    val graphOptions = output.parseGraphOptions(args)
    if (graphOptions.showProjects) {
      val graph = loadProjectDependencyGraph(projectName, projectPath)
      val affectedProjects = if (graphOptions.showAffected) {
        resolveAffectedProjects(projectName, projectPath)
      } else emptySet()
      if (graphOptions.open) output.openProjectGraph(projectName, graph, affectedProjects)
      else output.printProjectGraph(projectName, graph)
      return
    }
    val plans = if (engine) {
      graphOptions.taskName?.let { listOf(engineCommandClient.planTask(projectName, it)) }
        ?: engineCommandClient.getAllTasks(projectName).map { engineCommandClient.planTask(projectName, it.id) }
    } else {
      graphOptions.taskName?.let { listOf(embeddedTaskExecutor.plan(projectName, projectPath, it)) }
        ?: embeddedTaskExecutor.listTasks(projectName, projectPath).map {
          embeddedTaskExecutor.plan(projectName, projectPath, it.id)
        }
    }
    if (graphOptions.open) output.openGraph(projectName, plans)
    else output.printGraph(projectName, plans)
  }

  private fun runEmbeddedMode() {
    val projectPath = System.getProperty("user.dir")
    val projectName = extractProjectName(projectPath)

    if (!plain) {
      println("⚙️  Using embedded mode")
    }

    when (command) {
      "tasks" -> { output.printTasks(embeddedTaskExecutor.listTasks(projectName, projectPath)); return }
      null -> {
        val tasks = embeddedTaskExecutor.listTasks(projectName, projectPath)
        val selector = InteractiveTaskSelector(plain)
        val selected = selector.select(tasks)
        if (selected == null) {
          output.printTasks(tasks)
          return
        }
        command = selected
      }
      "info" -> { output.printInfo(projectName, projectPath, embeddedTaskExecutor.listTasks(projectName, projectPath)); return }
      "plan" -> {
        val planOptions = output.parsePlanOptions(args)
        if (planOptions.taskName == null) {
          println("Usage: architect plan <task> [--tree]")
          exitProcess(1)
        }
        val plan = embeddedTaskExecutor.plan(projectName, projectPath, planOptions.taskName)
        if (planOptions.tree) output.printPlanTree(plan) else output.printPlan(plan)
        return
      }
      "graph" -> {
        handleGraphCommand(projectName, projectPath, engine = false)
        return
      }
      "validate" -> {
        val validateArgs = if (args.isNotEmpty()) args.drop(1) else emptyList()
        if (validateArgs.contains("--structure")) {
          executeTaskEmbedded(projectName, projectPath, "architecture-validate", listOf("--structure"))
        }
        val validation = embeddedTaskExecutor.validate(projectName, projectPath)
        output.printValidation(projectName, validation)
        if (!validation.valid) exitProcess(1)
        return
      }
    }

    val taskArgs = if (args.isNotEmpty()) args.drop(1) else emptyList()

    // --all: execute task across all subprojects in dependency order
    if (all) {
      executeTaskAllProjects(projectName, projectPath, command!!, taskArgs)
      return
    }

    // --affected: execute task across affected subprojects in dependency order
    if (affected) {
      val affectedProjectNames = resolveAffectedProjects(projectName, projectPath)
      if (affectedProjectNames.isEmpty()) {
        println("✅ No projects affected — nothing to run.")
        return
      }
      executeTaskForProjects(projectName, projectPath, command!!, taskArgs, affectedProjectNames)
      return
    }

    val watchTask = if (command == "watch") args.getOrNull(1) else if (watch) command else null
    if (watchTask != null) {
      runWatchMode(projectPath, watchTask) {
        val watchAffectedProjects = resolveAffectedIfEnabled(projectName, projectPath)
          ?: return@runWatchMode
        executeTaskEmbedded(
          projectName,
          projectPath,
          watchTask,
          augmentTaskArgsForExecution(watchTask, taskArgs, watchAffectedProjects),
        )
      }
      return
    }

    val affectedProjects = resolveAffectedIfEnabled(projectName, projectPath) ?: return
    executeTaskEmbedded(
      projectName,
      projectPath,
      command!!,
      augmentTaskArgsForExecution(command!!, taskArgs, affectedProjects),
    )
  }

  private fun executeTaskAllProjects(projectName: String, projectPath: String, taskName: String, taskArgs: List<String>) {
    val context = io.github.architectplatform.core.execution.EmbeddedExecutionContext.create(
      remoteContentFetcher = io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher(),
      activeProfile = embeddedTaskExecutor.activeProfile,
    )
    context.projectService.registerProject(projectName, projectPath)
    val root = context.projectService.getProject(projectName) ?: run {
      println("❌ Project $projectName not found")
      exitProcess(1)
    }
    val graph = context.projectService.buildDependencyGraph(projectName)
    val allProjects = graph.projects.associateWith { name ->
      if (name == projectName) projectPath
      else root.subProjects.find { it.name == name }?.path ?: projectPath
    }
    executeProjectOrchestration(graph, allProjects, taskName, taskArgs, label = "all ${allProjects.size} project(s)")
  }

  private fun executeTaskForProjects(
    projectName: String,
    projectPath: String,
    taskName: String,
    taskArgs: List<String>,
    targetProjectNames: Set<String>,
  ) {
    val context = io.github.architectplatform.core.execution.EmbeddedExecutionContext.create(
      remoteContentFetcher = io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher(),
      activeProfile = embeddedTaskExecutor.activeProfile,
    )
    context.projectService.registerProject(projectName, projectPath)
    val root = context.projectService.getProject(projectName) ?: run {
      println("❌ Project $projectName not found")
      exitProcess(1)
    }
    val graph = context.projectService.buildDependencyGraph(projectName)
    val targetProjects = targetProjectNames.associateWith { name ->
      if (name == projectName) projectPath
      else root.subProjects.find { it.name == name }?.path ?: projectPath
    }
    if (!plain) {
      println("🎯 Affected projects: ${targetProjectNames.sorted().joinToString(", ")}")
    }
    executeProjectOrchestration(graph, targetProjects, taskName, taskArgs, label = "${targetProjects.size} affected project(s)")
  }

  private fun executeProjectOrchestration(
    graph: io.github.architectplatform.core.project.domain.ProjectDependencyGraph,
    targetProjects: Map<String, String>,
    taskName: String,
    taskArgs: List<String>,
    label: String,
  ) {
    println()
    println("━".repeat(80))
    println("🏗  Cross-project execution: $taskName ($label)")
    println("━".repeat(80))

    val result = multiProjectOrchestrator.run(
      graph = graph,
      targetProjects = targetProjects,
      taskName = taskName,
      taskArgs = taskArgs,
      stopOnFailure = true,
      onEvent = { /* events streamed to console by executor */ },
      onProgress = { projName: String, tier: Int, totalTiers: Int ->
        println()
        println("  ▶ [$tier/$totalTiers] $projName")
        println("  ${"─".repeat(60)}")
      },
    )

    println()
    println("━".repeat(80))
    println("📊 Cross-project summary: $taskName")
    println("━".repeat(80))
    result.results.forEach { pr: MultiProjectOrchestrator.ProjectResult ->
      val icon = if (pr.result.success) "✅" else "❌"
      val ms = pr.durationMs
      println("  $icon ${pr.projectName} — ${pr.result.message} (${ms}ms)")
    }
    if (result.skipped.isNotEmpty()) {
      println("  ⏭  Skipped: ${result.skipped.joinToString(", ")}")
    }
    println()
    if (!result.success) {
      exitProcess(1)
    }
  }

  /**
   * Resolves affected projects if `--affected` flag is set.
   * Returns null if affected is set but no projects are affected (caller should return early).
   * Returns empty set if affected flag is not set.
   */
  private fun resolveAffectedIfEnabled(projectName: String, projectPath: String): Set<String>? {
    if (!affected) return emptySet()
    val resolved = resolveAffectedProjects(projectName, projectPath)
    if (resolved.isEmpty()) {
      println("✅ No projects affected — nothing to run.")
      return null
    }
    if (!plain) {
      println("🎯 Affected projects: ${resolved.joinToString(", ")}")
    }
    return resolved
  }

  private fun resolveAffectedProjects(projectName: String, projectPath: String): Set<String> {
    val context = EmbeddedExecutionContext.create(
      remoteContentFetcher = io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher(),
      activeProfile = embeddedTaskExecutor.activeProfile,
    )
    context.projectService.registerProject(projectName, projectPath)
    val project = context.projectService.getProject(projectName)
      ?: return emptySet()

    val graph = context.projectService.buildDependencyGraph(projectName)
    val projectConfig = project.context.config["project"] as? Map<*, *>
    val affectedConfig = AffectedProjectResolver.parseConfig(projectConfig)
    val resolver = AffectedProjectResolver()

    if (!noCache) {
      val outputCache = LocalOutputCache()
      resolver.cacheValidator = { projects ->
        projects.filterTo(mutableSetOf()) { projName ->
          val subProject = graph.projects
            .firstOrNull { it == projName }
          if (subProject == null) true
          else !outputCache.contains("project:$projName")
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

  private fun executeTask(projectName: String, taskName: String, taskArgs: List<String>) {
    val ui = ConsoleUI(taskName, plain, verbosity, timing)

    println()
    println("━".repeat(80))
    println("▶  Executing task: $taskName")
    println("📦 Project: $projectName")
    if (verbosity >= 3) {
      println("🔧 Verbosity: $verbosity | Plain: $plain | No-cache: $noCache | Profile: ${embeddedTaskExecutor.activeProfile}")
    }
    println("━".repeat(80))
    println()
    
    runBlocking {
      val startTime = System.currentTimeMillis()
      try {
        val executionId = engineCommandClient.execute(projectName, taskName, taskArgs)
        activeExecutionId = executionId
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
    val ui = EmbeddedConsoleUI(taskName, plain, verbosity, timing)

    println()
    println("━".repeat(80))
    println("▶  Executing task: $taskName")
    println("📦 Project: $projectName")
    if (verbosity >= 3) {
      println("🔧 Verbosity: $verbosity | Plain: $plain | No-cache: $noCache | Profile: ${embeddedTaskExecutor.activeProfile}")
      println("📁 Project path: $projectPath")
    }
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

  private fun runWatchMode(projectPath: String, taskName: String, executeBlock: () -> Unit) {
    println("👀 Watch mode: $taskName")
    println("   Watching for changes in $projectPath ...")
    println("   Press Ctrl+C to stop")
    println()

    try { executeBlock() } catch (_: Exception) { /* allow re-run on next change */ }

    val watchService = io.github.architectplatform.core.watch.FileWatchService(
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

    Runtime.getRuntime().addShutdownHook(Thread {
      watchService.stop()
      println()
      println("👋 Watch mode stopped")
    })

    watchService.start()
  }

  private fun loadProjectDependencyGraph(projectName: String, projectPath: String): io.github.architectplatform.core.project.domain.ProjectDependencyGraph {
    val context = EmbeddedExecutionContext.create(
      remoteContentFetcher = io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher(),
      activeProfile = embeddedTaskExecutor.activeProfile,
    )
    context.projectService.registerProject(projectName, projectPath)
    return context.projectService.buildDependencyGraph(projectName)
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
