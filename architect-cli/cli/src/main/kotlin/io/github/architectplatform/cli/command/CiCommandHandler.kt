package io.github.architectplatform.cli.command

import io.github.architectplatform.cli.embedded.EmbeddedTaskExecutor
import io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher
import io.github.architectplatform.core.ci.CiPipeline
import io.github.architectplatform.core.ci.GitHubActionsRenderer
import io.github.architectplatform.core.ci.PipelineGenerator
import io.github.architectplatform.core.execution.EmbeddedExecutionContext
import java.io.File
import kotlin.system.exitProcess

/**
 * Handles the `architect ci` command: generates, diffs, and syncs CI/CD pipeline workflows.
 *
 * Subcommands:
 *   generate  — Generate a pipeline YAML from the current project's architect.yml
 *   diff      — Show differences between the generated pipeline and the existing file
 *   sync      — Regenerate and overwrite the existing pipeline file
 */
class CiCommandHandler(
  private val embeddedTaskExecutor: EmbeddedTaskExecutor,
  private val extractProjectName: (String) -> String,
  private val exit: (Int) -> Unit = { code -> exitProcess(code) },
) {

  fun handle(args: List<String>) {
    val subCommand = args.getOrNull(1) ?: "generate"
    when (subCommand) {
      "generate" -> generate(args)
      "diff" -> diff(args)
      "sync" -> sync(args)
      else -> {
        println("Unknown ci subcommand: '$subCommand'")
        println()
        println("Usage: architect ci <subcommand> [options]")
        println()
        println("Subcommands:")
        println("  generate  Generate a CI/CD pipeline workflow file")
        println("  diff      Show drift between generated and existing pipeline")
        println("  sync      Regenerate and overwrite the existing pipeline file")
        println()
        println("Options:")
        println("  --provider <name>   CI provider (default: github-actions)")
        println("  --output <path>     Output file path (generate/sync only)")
        println("  --dry-run           Print to stdout instead of writing to file (generate only)")
        exit(1)
      }
    }
  }

  // ── subcommands ─────────────────────────────────────────────────────────────

  private fun generate(args: List<String>) {
    val provider = parseOption(args, "--provider") ?: "github-actions"
    val output = parseOption(args, "--output") ?: defaultOutputPath(provider)
    val dryRun = args.contains("--dry-run")

    val pipeline = loadPipeline(provider) ?: return
    val yaml = renderPipeline(pipeline, provider) ?: return

    if (dryRun) {
      println(yaml)
    } else {
      writeFile(output, yaml)
      println("✅ Pipeline written to $output")
    }
  }

  private fun diff(args: List<String>) {
    val provider = parseOption(args, "--provider") ?: "github-actions"
    val output = parseOption(args, "--output") ?: defaultOutputPath(provider)

    val pipeline = loadPipeline(provider) ?: return
    val generated = renderPipeline(pipeline, provider) ?: return

    val existing = File(output).takeIf { it.exists() }?.readText()
    if (existing == null) {
      println("⚠  No existing file at $output — pipeline has not been generated yet.")
      println()
      println("Run `architect ci generate` to create it.")
      exit(1)
      return
    }

    if (generated == existing) {
      println("✅ No drift detected — $output is up to date.")
    } else {
      println("⚠  Drift detected in $output:")
      println()
      println(unifiedDiff(existing, generated, output))
      exit(1)
    }
  }

  private fun sync(args: List<String>) {
    val provider = parseOption(args, "--provider") ?: "github-actions"
    val output = parseOption(args, "--output") ?: defaultOutputPath(provider)

    val pipeline = loadPipeline(provider) ?: return
    val generated = renderPipeline(pipeline, provider) ?: return

    val existing = File(output).takeIf { it.exists() }?.readText()
    writeFile(output, generated)

    if (existing == null) {
      println("✅ Pipeline created at $output")
    } else if (generated == existing) {
      println("✅ Pipeline is already up to date — $output unchanged.")
    } else {
      println("✅ Pipeline synced — $output updated.")
      println()
      println(unifiedDiff(existing, generated, output))
    }
  }

  // ── helpers ──────────────────────────────────────────────────────────────────

  private fun loadPipeline(provider: String): CiPipeline? {
    if (provider != "github-actions") {
      println("Unsupported CI provider: '$provider'")
      println("Currently supported: github-actions")
      exit(1)
      return null
    }

    val projectPath = System.getProperty("user.dir")
    val projectName = extractProjectName(projectPath)

    return try {
      val context = EmbeddedExecutionContext.create(
        remoteContentFetcher = JdkRemoteContentFetcher(),
        activeProfile = embeddedTaskExecutor.activeProfile,
      )
      context.projectService.registerProject(projectName, projectPath)
      val project = context.projectService.getProject(projectName)
        ?: throw IllegalArgumentException("Project '$projectName' is not registered")

      PipelineGenerator.generate(
        registry = project.taskRegistry,
        name = "$projectName CI",
      )
    } catch (e: Exception) {
      println("❌ Failed to load project: ${e.message}")
      exit(1)
      null
    }
  }

  private fun renderPipeline(pipeline: CiPipeline, provider: String): String? {
    if (provider != "github-actions") {
      println("Unsupported CI provider: '$provider'")
      exit(1)
      return null
    }
    return GitHubActionsRenderer.render(pipeline)
  }

  private fun writeFile(path: String, content: String) {
    val file = File(path)
    file.parentFile?.mkdirs()
    file.writeText(content)
  }

  private fun defaultOutputPath(provider: String): String = when (provider) {
    "github-actions" -> ".github/workflows/architect.yml"
    else -> "ci-pipeline.yml"
  }

  private fun parseOption(args: List<String>, name: String): String? {
    val index = args.indexOf(name)
    return if (index >= 0 && index + 1 < args.size) args[index + 1] else null
  }

  /**
   * Produces a simple unified-diff-style output comparing [before] and [after].
   */
  internal fun unifiedDiff(before: String, after: String, label: String = "pipeline"): String {
    val beforeLines = before.lines()
    val afterLines = after.lines()
    val sb = StringBuilder()
    sb.appendLine("--- $label (existing)")
    sb.appendLine("+++ $label (generated)")
    sb.appendLine()

    var i = 0
    var j = 0
    while (i < beforeLines.size || j < afterLines.size) {
      val beforeLine = beforeLines.getOrNull(i)
      val afterLine = afterLines.getOrNull(j)
      when {
        beforeLine == null -> {
          sb.appendLine("+ $afterLine")
          j++
        }
        afterLine == null -> {
          sb.appendLine("- $beforeLine")
          i++
        }
        beforeLine == afterLine -> {
          sb.appendLine("  $beforeLine")
          i++
          j++
        }
        else -> {
          sb.appendLine("- $beforeLine")
          sb.appendLine("+ $afterLine")
          i++
          j++
        }
      }
    }
    return sb.toString().trimEnd()
  }
}
