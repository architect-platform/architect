package io.github.architectplatform.core.ci

/**
 * Renders a [CiPipeline] to a GitHub Actions workflow YAML string.
 *
 * Output is generated manually without any third-party YAML library.
 */
object GitHubActionsRenderer {

  /**
   * Renders the given [pipeline] to a GitHub Actions YAML string.
   */
  fun render(pipeline: CiPipeline): String {
    val sb = StringBuilder()

    sb.appendLine("name: ${pipeline.name}")
    sb.appendLine()
    sb.appendLine("on:")
    renderTrigger(sb, pipeline.on)
    sb.appendLine()
    sb.appendLine("jobs:")
    pipeline.jobs.forEach { job -> renderJob(sb, job) }

    return sb.toString()
  }

  private fun renderTrigger(sb: StringBuilder, trigger: CiTrigger) {
    trigger.push?.let { filter ->
      sb.appendLine("  push:")
      sb.appendLine("    branches:")
      filter.branches.forEach { branch -> sb.appendLine("      - $branch") }
    }
    trigger.pullRequest?.let { filter ->
      sb.appendLine("  pull_request:")
      sb.appendLine("    branches:")
      filter.branches.forEach { branch -> sb.appendLine("      - $branch") }
    }
    if (trigger.workflowDispatch) {
      sb.appendLine("  workflow_dispatch:")
    }
  }

  private fun renderJob(sb: StringBuilder, job: CiJob) {
    sb.appendLine("  ${job.id}:")
    sb.appendLine("    name: ${quoteIfNeeded(job.name)}")
    sb.appendLine("    runs-on: ${job.runsOn}")
    if (job.needs.isNotEmpty()) {
      sb.appendLine("    needs: [${job.needs.joinToString(", ")}]")
    }
    if (job.env.isNotEmpty()) {
      sb.appendLine("    env:")
      job.env.forEach { (key, value) -> sb.appendLine("      $key: ${quoteIfNeeded(value)}") }
    }
    sb.appendLine("    steps:")
    job.steps.forEach { step -> renderStep(sb, step) }
  }

  private fun renderStep(sb: StringBuilder, step: CiStep) {
    when {
      step.uses != null -> {
        sb.appendLine("      - uses: ${step.uses}")
        if (step.with.isNotEmpty()) {
          sb.appendLine("        with:")
          step.with.forEach { (key, value) -> sb.appendLine("          $key: ${quoteIfNeeded(value)}") }
        }
      }
      step.run != null -> {
        sb.appendLine("      - name: ${quoteIfNeeded(step.name)}")
        sb.appendLine("        run: ${step.run}")
      }
      else -> {
        sb.appendLine("      - name: ${quoteIfNeeded(step.name)}")
      }
    }
  }

  /**
   * Wraps a YAML scalar in double quotes if it contains special characters.
   */
  private fun quoteIfNeeded(value: String): String {
    val needsQuoting = value.contains(':') || value.contains('#') || value.startsWith('*') ||
      value.startsWith('&') || value.startsWith('!') || value.contains('"') ||
      value.contains('\'') || value.isBlank()
    return if (needsQuoting) "\"${value.replace("\"", "\\\"")}\"" else value
  }
}
