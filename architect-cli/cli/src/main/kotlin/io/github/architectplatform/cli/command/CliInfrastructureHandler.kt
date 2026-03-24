package io.github.architectplatform.cli.command

import kotlin.system.exitProcess

/**
 * Handles the `architect completion` and `architect upgrade` commands.
 */
class CliInfrastructureHandler {

  fun handleCompletion(args: List<String>, cliInstance: Runnable) {
    val shell = args.getOrNull(1)?.lowercase() ?: "bash"
    when (shell) {
      "bash" -> {
        val script = picocli.AutoComplete.bash("architect", picocli.CommandLine(cliInstance))
        println(script)
      }
      "zsh" -> {
        val bashScript = picocli.AutoComplete.bash("architect", picocli.CommandLine(cliInstance))
        println("# Generated zsh completion for architect")
        println("# Add to ~/.zshrc: eval \"\$(architect completion zsh)\"")
        println("autoload -U +X bashcompinit && bashcompinit")
        println("autoload -U +X compinit && compinit")
        println(bashScript)
      }
      "fish" -> {
        println("# Generated fish completion for architect")
        println("# Save to: ~/.config/fish/completions/architect.fish")
        println()
        val subcommands = listOf(
          "tasks" to "List available tasks",
          "info" to "Show project information",
          "plan" to "Show execution plan for a task",
          "graph" to "Render task dependency graph",
          "validate" to "Validate project configuration",
          "history" to "Show execution history",
          "run" to "Run a task",
          "watch" to "Watch and re-run task on changes",
          "affected" to "List affected projects",
          "cache" to "Manage task output cache",
          "engine" to "Manage the Architect Engine",
          "plugin" to "Manage plugins",
          "completion" to "Generate shell completion scripts",
          "upgrade" to "Upgrade architect to the latest release",
        )
        subcommands.forEach { (sub, desc) ->
          println("complete -c architect -f -n '__fish_use_subcommand architect' -a $sub -d '$desc'")
        }
        println()
        println("complete -c architect -l json -d 'Output in JSON format'")
        println("complete -c architect -l no-color -d 'Disable colored output'")
        println("complete -c architect -l embedded -d 'Run in embedded mode'")
        println("complete -c architect -l no-daemon -d 'Skip daemon startup'")
        println("complete -c architect -l watch -s w -d 'Watch and re-run on changes'")
        println("complete -c architect -l version -s v -d 'Print version information'")
        println("complete -c architect -l filter -d 'Filter tasks by phase'")
        println("complete -c architect -l env -d 'Active environment profile'")
        println("complete -c architect -l affected -d 'Only run for affected projects'")
        println("complete -c architect -l base -d 'Base ref for affected detection'")
        println("complete -c architect -l no-cache -d 'Bypass task output cache'")
      }
      else -> {
        println("Unsupported shell: $shell")
        println("Supported: bash, zsh, fish")
        exitProcess(1)
      }
    }
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
