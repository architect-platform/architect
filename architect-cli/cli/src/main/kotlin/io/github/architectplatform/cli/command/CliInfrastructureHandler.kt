package io.github.architectplatform.cli.command

import io.github.architectplatform.cli.client.EngineCommandClient
import kotlin.system.exitProcess

/**
 * Handles the `architect completion` and `architect upgrade` commands.
 *
 * @param engineCommandClient Optional HTTP client to query the engine for dynamic completions.
 * @param extractProjectName Function to resolve the current project name from a directory path.
 */
class CliInfrastructureHandler(
  private val engineCommandClient: EngineCommandClient? = null,
  private val extractProjectName: ((String) -> String)? = null,
) {

  // ── Phase names used for --filter completions ─────────────────────────────

  private val knownPhases = listOf(
    "INIT", "LINT", "VERIFY", "BUILD", "TEST", "RUN", "RELEASE", "PUBLISH"
  )

  // ── Dynamic completion query ──────────────────────────────────────────────

  /**
   * Handles `architect completion query <type>`.
   *
   * Outputs one completion candidate per line for shell completion scripts.
   * Types:
   *  - `tasks`   — available task IDs for the current project
   *  - `phases`  — known phase names for --filter completions
   *  - `projects`— registered project names
   */
  fun handleCompletionQuery(type: String) {
    when (type.lowercase()) {
      "tasks" -> {
        val tasks = fetchTasksFromCache()
        tasks.forEach { println(it) }
      }
      "phases" -> knownPhases.forEach { println(it) }
      "projects" -> {
        val projects = runCatching {
          engineCommandClient?.getAllProjects()?.map { it.name } ?: emptyList()
        }.getOrElse { emptyList() }
        // Also include any locally cached project names
        val cached = readProjectCache()
        (projects + cached).distinct().sorted().forEach { println(it) }
      }
      else -> {
        System.err.println("Unknown completion type: $type  (supported: tasks, phases, projects)")
        exitProcess(1)
      }
    }
  }

  private fun fetchTasksFromCache(): List<String> {
    // 1. Try the engine (fastest when running)
    val fromEngine = runCatching {
      if (engineCommandClient == null) return@runCatching emptyList()
      val projectPath = System.getProperty("user.dir")
      val projectName = extractProjectName?.invoke(projectPath) ?: return@runCatching emptyList()
      val tasks = engineCommandClient.getAllTasks(projectName).map { it.id }
      if (tasks.isNotEmpty()) writeTaskCache(tasks)
      tasks
    }.getOrElse { emptyList() }
    if (fromEngine.isNotEmpty()) return fromEngine

    // 2. Fall back to cache file written by a previous run
    val cached = readTaskCache()
    if (cached.isNotEmpty()) return cached

    // 3. Last resort: static known sub-commands
    return listOf(
      "tasks", "info", "plan", "graph", "validate", "history",
      "watch", "affected", "cache", "engine", "plugin", "completion",
      "upgrade", "init", "help", "doctor", "config", "retry", "check"
    )
  }

  // ── Cache helpers ─────────────────────────────────────────────────────────

  private val cacheDir get() = java.io.File(System.getProperty("user.home"), ".architect")
  private val taskCacheFile get() = java.io.File(cacheDir, "task-cache.txt")
  private val projectCacheFile get() = java.io.File(cacheDir, "project-cache.txt")

  private fun writeTaskCache(tasks: List<String>) {
    runCatching {
      cacheDir.mkdirs()
      taskCacheFile.writeText(tasks.joinToString("\n"))
    }
  }

  private fun readTaskCache(): List<String> =
    runCatching {
      if (!taskCacheFile.exists()) return emptyList()
      taskCacheFile.readLines().filter { it.isNotBlank() }
    }.getOrElse { emptyList() }

  /** Called after successful project registration to persist name for completion. */
  fun cacheProjectName(name: String) {
    runCatching {
      cacheDir.mkdirs()
      val existing = readProjectCache().toMutableList()
      if (name !in existing) {
        existing.add(name)
        projectCacheFile.writeText(existing.joinToString("\n"))
      }
    }
  }

  private fun readProjectCache(): List<String> =
    runCatching {
      if (!projectCacheFile.exists()) return emptyList()
      projectCacheFile.readLines().filter { it.isNotBlank() }
    }.getOrElse { emptyList() }

  // ── Shell script generation ───────────────────────────────────────────────

  fun handleCompletion(args: List<String>, cliInstance: Runnable) {
    val subCmd = args.getOrNull(1)?.lowercase() ?: "bash"

    // Internal sub-command: `architect completion query <type>`
    if (subCmd == "query") {
      val type = args.getOrNull(2) ?: "tasks"
      handleCompletionQuery(type)
      return
    }

    val shell = subCmd
    when (shell) {
      "bash" -> printBashCompletion(cliInstance)
      "zsh" -> printZshCompletion(cliInstance)
      "fish" -> printFishCompletion()
      else -> {
        println("Unsupported shell: $shell")
        println("Supported: bash, zsh, fish")
        exitProcess(1)
      }
    }
  }

  private fun printBashCompletion(cliInstance: Runnable) {
    val base = picocli.AutoComplete.bash("architect", picocli.CommandLine(cliInstance))
    // Inject dynamic task and phase completion into the picocli-generated script.
    // The injected function wraps the picocli default to add live task-name candidates.
    val dynamic = """
# -- Dynamic task/phase completion (Architect) --
_architect_dynamic_tasks() {
  architect completion query tasks 2>/dev/null
}
_architect_dynamic_phases() {
  architect completion query phases 2>/dev/null
}
_architect_complete() {
  local cur prev
  COMPREPLY=()
  cur="${'$'}{COMP_WORDS[COMP_CWORD]}"
  prev="${'$'}{COMP_WORDS[COMP_CWORD-1]}"

  # --filter <phase> completion
  if [[ "${'$'}prev" == "--filter" || "${'$'}prev" == "--filter=" ]]; then
    local phases
    phases=${'$'}(_architect_dynamic_phases)
    COMPREPLY=(${'$'}(compgen -W "${'$'}phases" -- "${'$'}cur"))
    return 0
  fi

  # First positional argument: task name completion
  local wordnum=0
  for ((i=1; i<COMP_CWORD; i++)); do
    if [[ "${'$'}{COMP_WORDS[${'$'}i]}" != -* ]]; then
      ((wordnum++))
    fi
  done

  if [[ ${'$'}wordnum -eq 0 ]]; then
    local tasks
    tasks=${'$'}(_architect_dynamic_tasks)
    COMPREPLY=(${'$'}(compgen -W "${'$'}tasks" -- "${'$'}cur"))
    return 0
  fi
}
complete -F _architect_complete architect
""".trimIndent()
    println(base)
    println()
    println(dynamic)
  }

  private fun printZshCompletion(cliInstance: Runnable) {
    val bashScript = picocli.AutoComplete.bash("architect", picocli.CommandLine(cliInstance))
    println("# Generated zsh completion for architect")
    println("# Add to ~/.zshrc: eval \"\$(architect completion zsh)\"")
    println("autoload -U +X bashcompinit && bashcompinit")
    println("autoload -U +X compinit && compinit")
    println()
    println("# Dynamic task/phase completions")
    println("_architect_zsh() {")
    println("  local -a tasks phases")
    println("  tasks=(\${(f)\"\$(architect completion query tasks 2>/dev/null)\"})")
    println("  case \"\$words[2]\" in")
    println("    '') _describe 'task' tasks ;;")
    println("    *) _describe 'task' tasks ;;")
    println("  esac")
    println("}")
    println()
    println(bashScript)
    println()
    println("# Override with zsh native function for better UX")
    println("compdef _architect_zsh architect")
  }

  private fun printFishCompletion() {
    println("# Generated fish completion for architect")
    println("# Save to: ~/.config/fish/completions/architect.fish")
    println("# Or run: architect completion install")
    println()

    // Dynamic task completions — call architect to get live task names
    println("# Dynamic task completions (queries the engine or uses cache)")
    println("complete -c architect -f -n 'not __fish_seen_subcommand_from " +
      "tasks info plan graph validate history watch affected cache engine plugin completion upgrade init help doctor config retry check" +
      "' -a '(architect completion query tasks 2>/dev/null)' -d 'Task'")
    println()

    // Static sub-commands
    val subcommands = listOf(
      "tasks" to "List available tasks",
      "info" to "Show project information",
      "plan" to "Show execution plan for a task",
      "graph" to "Render task dependency graph",
      "validate" to "Validate project configuration",
      "history" to "Show execution history",
      "watch" to "Watch and re-run task on changes",
      "affected" to "List affected projects",
      "cache" to "Manage task output cache",
      "engine" to "Manage the Architect Engine",
      "plugin" to "Manage plugins",
      "completion" to "Generate shell completion scripts",
      "upgrade" to "Upgrade architect to the latest release",
      "init" to "Scaffold a new project interactively",
      "help" to "Show help",
      "doctor" to "Run environment diagnostics",
      "config" to "Manage configuration",
      "retry" to "Retry the last failed execution",
      "check" to "Run precondition checks",
    )
    println("# Sub-commands")
    subcommands.forEach { (sub, desc) ->
      println("complete -c architect -f -a $sub -d '$desc'")
    }
    println()

    // Flags
    println("# Flags")
    println("complete -c architect -l json -d 'Output in JSON format'")
    println("complete -c architect -l no-color -d 'Disable colored output'")
    println("complete -c architect -l plain -s p -d 'Plain output for CI'")
    println("complete -c architect -l embedded -d 'Run in embedded mode'")
    println("complete -c architect -l no-daemon -d 'Skip daemon startup'")
    println("complete -c architect -l watch -s w -d 'Watch and re-run on changes'")
    println("complete -c architect -l version -s v -d 'Print version information'")
    println("complete -c architect -l no-cache -d 'Bypass task output cache'")
    println("complete -c architect -l dry-run -d 'Show plan without running'")
    println("complete -c architect -l verbose -d 'Increase verbosity'")
    println("complete -c architect -l quiet -s q -d 'Quiet mode'")
    println("complete -c architect -l timing -d 'Show timing breakdown'")
    println("complete -c architect -l parallel -d 'Control task parallelism'")
    println("complete -c architect -l output -s o -d 'Save output to file'")
    println("complete -c architect -l tee -d 'Output to file and terminal'")
    println("complete -c architect -l affected -d 'Only run for affected projects'")
    println("complete -c architect -l base -d 'Base ref for affected detection'")
    println()

    // Dynamic phase completion for --filter
    println("# Dynamic phase completion for --filter")
    println("complete -c architect -l filter -x -a '(architect completion query phases 2>/dev/null)' -d 'Filter by phase'")
    println()

    // Dynamic env profile completions could be added here in future
    println("complete -c architect -l env -d 'Active environment profile'")
  }

  @Suppress("UNCHECKED_CAST")
  fun handleUpgrade(args: List<String>) {
    val checkOnly = args.contains("--check")
    val currentVersion = javaClass.`package`?.implementationVersion ?: "dev"

    println("Checking for updates...")

    try {
      val url = java.net.URI("https://api.github.com/repos/architect-platform/architect/releases/latest").toURL()
      val connection = url.openConnection() as java.net.HttpURLConnection
      connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
      connection.setRequestProperty("User-Agent", "architect-cli/$currentVersion")
      connection.connectTimeout = 10_000
      connection.readTimeout = 10_000

      if (connection.responseCode != 200) {
        println("Failed to check for updates (HTTP ${connection.responseCode})")
        exitProcess(1)
      }

      val responseBody = connection.inputStream.bufferedReader().readText()
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
      val release = mapper.readValue(responseBody, Map::class.java)

      val latestTag = release["tag_name"] as? String ?: run {
        println("Could not determine latest version")
        exitProcess(1)
      }
      val latestVersion = latestTag.removePrefix("v")

      println("Current version : $currentVersion")
      println("Latest version  : $latestVersion")

      if (currentVersion == latestVersion || currentVersion == "dev") {
        if (currentVersion == "dev") {
          println("Running a dev build — skipping upgrade.")
        } else {
          println("✅ Already up to date.")
        }
        return
      }

      if (checkOnly) {
        println("Upgrade available: $latestTag")
        return
      }

      println("Upgrading architect from $currentVersion → $latestVersion ...")

      val osName = System.getProperty("os.name").lowercase()
      val osArch = System.getProperty("os.arch").lowercase()
      val platform = when {
        osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm")) -> "macos-arm64"
        osName.contains("mac") -> "macos-x86_64"
        osName.contains("linux") && (osArch.contains("aarch64") || osArch.contains("arm")) -> "linux-arm64"
        osName.contains("linux") -> "linux-x86_64"
        osName.contains("win") -> "windows-x86_64"
        else -> {
          println("Unsupported platform: $osName $osArch")
          exitProcess(1)
        }
      }
      val assetName = if (osName.contains("win")) "architect-$platform.exe" else "architect-$platform"

      val assets = release["assets"] as? List<Map<String, Any>> ?: emptyList()
      val asset = assets.firstOrNull { (it["name"] as? String) == assetName } ?: run {
        println("No release asset found for platform: $platform")
        println("Available assets:")
        assets.forEach { a -> println("  - ${a["name"]}") }
        exitProcess(1)
      }
      val downloadUrl = asset["browser_download_url"] as? String ?: run {
        println("Could not determine download URL for $assetName")
        exitProcess(1)
      }

      val currentBinary = ProcessHandle.current().info().command().orElse(null)
        ?.let { java.io.File(it) }
        ?: java.io.File(System.getProperty("user.home"), ".architect/bin/architect")

      val tempFile = java.io.File.createTempFile("architect-upgrade-", if (osName.contains("win")) ".exe" else "")
      tempFile.deleteOnExit()

      println("Downloading $assetName ...")
      val dlUrl = java.net.URI(downloadUrl).toURL()
      val dlConn = dlUrl.openConnection() as java.net.HttpURLConnection
      dlConn.setRequestProperty("User-Agent", "architect-cli/$currentVersion")
      dlConn.connectTimeout = 30_000
      dlConn.readTimeout = 60_000
      dlConn.inputStream.use { inp -> tempFile.outputStream().use { out -> inp.copyTo(out) } }

      val checksumAssetName = "$assetName.sha256"
      val checksumAsset = assets.firstOrNull { (it["name"] as? String) == checksumAssetName }
      if (checksumAsset != null) {
        val checksumUrl = checksumAsset["browser_download_url"] as? String
        if (checksumUrl != null) {
          val expectedHash = java.net.URI(checksumUrl).toURL().openStream()
            .bufferedReader().readText().trim().split("\\s+".toRegex()).first()
          val digest = java.security.MessageDigest.getInstance("SHA-256")
          val actualHash = digest.digest(tempFile.readBytes())
            .joinToString("") { "%02x".format(it) }
          if (expectedHash != actualHash) {
            println("❌ Checksum mismatch — upgrade aborted for security")
            tempFile.delete()
            exitProcess(1)
          }
          println("✅ Checksum verified")
        }
      }

      tempFile.setExecutable(true)
      val backupFile = java.io.File("${currentBinary.absolutePath}.bak")
      if (currentBinary.exists()) currentBinary.copyTo(backupFile, overwrite = true)
      tempFile.copyTo(currentBinary, overwrite = true)
      backupFile.delete()

      println("✅ Upgraded to $latestVersion")
    } catch (e: java.net.UnknownHostException) {
      println("No network access — cannot check for updates")
      exitProcess(1)
    } catch (e: Exception) {
      println("Upgrade failed: ${e.message}")
      exitProcess(1)
    }
  }
}
