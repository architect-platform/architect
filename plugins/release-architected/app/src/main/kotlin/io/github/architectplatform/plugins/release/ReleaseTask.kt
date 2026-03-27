package io.github.architectplatform.plugins.release

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.components.execution.CommandResult
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.project.resolvePath
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase
import io.github.architectplatform.api.core.utils.ShellArgumentSanitizer
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.io.path.name

class ReleaseTask(
  private val operation: Operation,
  private val phase: Phase,
  private val context: ReleaseContext,
) : Task {
  override val id: String = operation.taskId

  override fun phase(): Phase = phase

  override fun description(): String = operation.description

  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult {
    if (!context.enabled) {
      return TaskResult.skipped("Release tasks disabled")
    }

    val workingDir = try {
      projectContext.resolvePath(context.workingDirectory, "Release working directory")
    } catch (e: IllegalArgumentException) {
      return TaskResult.failure("Release task $id failed with invalid working directory: ${e.message}")
    }

    return try {
      val executor = environment.service(CommandExecutor::class.java)
      when (operation) {
        Operation.PREPARE -> executePrepare(projectContext, executor, workingDir, args)
        Operation.PUBLISH -> executePublish(projectContext, executor, workingDir, args)
        Operation.ROLLBACK -> executeRollback(projectContext, executor, workingDir, args)
      }
    } catch (e: IllegalArgumentException) {
      TaskResult.failure("Release task $id failed: ${e.message}")
    }
  }

  private fun executePrepare(
    projectContext: ProjectContext,
    executor: CommandExecutor,
    workingDir: Path,
    args: List<String>,
  ): TaskResult {
    val plan = buildReleasePlan(projectContext, executor, workingDir, args)
    plan.skipReason?.let {
      return TaskResult.skipped(it)
    }

    val updatedFiles = mutableListOf<String>()
    if (context.versionFiles.isNotEmpty()) {
      context.versionFiles.forEach { file ->
        val path = resolveWorkingPath(projectContext, file, "Release version file")
        updateVersionFile(path, plan.nextVersion)
        updatedFiles += projectContext.dir.relativize(path).toString()
      }
    }

    var changelogPath: String? = null
    if (context.changelog) {
      val path = resolveWorkingPath(projectContext, context.changelogPath, "Release changelog path")
      updateChangelog(path, plan.changelogSection)
      changelogPath = projectContext.dir.relativize(path).toString()
    }

    var releaseNotesPath: String? = null
    if (context.releaseNotesPath.isNotBlank()) {
      val path = resolveWorkingPath(projectContext, context.releaseNotesPath, "Release notes path")
      Files.createDirectories(requireNotNull(path.parent))
      Files.writeString(path, plan.changelogSection + "\n")
      releaseNotesPath = projectContext.dir.relativize(path).toString()
    }

    val data = mutableMapOf<String, Any>(
      "operation" to operation.taskId,
      "strategy" to plan.strategy,
      "previousVersion" to (plan.previousVersion ?: "0.0.0"),
      "nextVersion" to plan.nextVersion,
      "tag" to plan.tag,
      "recommendedBump" to plan.bump.name.lowercase(),
      "commitCount" to plan.commits.size,
      "commitRange" to plan.commitRange,
      "commits" to plan.commits.map { it.toData() },
      "versionFiles" to updatedFiles,
    )
    changelogPath?.let { data["changelogPath"] = it }
    releaseNotesPath?.let { data["releaseNotesPath"] = it }

    return TaskResult.success(
      "Prepared release ${plan.tag} from ${plan.commits.size} commit(s)",
      data = data,
    )
  }

  private fun executePublish(
    projectContext: ProjectContext,
    executor: CommandExecutor,
    workingDir: Path,
    args: List<String>,
  ): TaskResult {
    if (!context.publish.enabled) {
      return TaskResult.skipped("Release publishing disabled")
    }

    val version = resolveRequestedVersion(projectContext, executor, workingDir, args)
      ?: return TaskResult.failure("Release task $id failed: unable to resolve a release version")
    val tag = buildTag(version)
    val artifacts = context.artifacts.filter { it.enabled }
    if (artifacts.isEmpty()) {
      return TaskResult.skipped("No release artifacts are configured")
    }

    val summaries = mutableListOf<CommandSummary>()
    val failures = mutableListOf<String>()
    val sharedNotesPath = resolveExistingNotesPath(projectContext)

    context.publish.beforeCommands.forEachIndexed { index, command ->
      val summary = executeCommand(executor, command, workingDir, "before-command-$index")
      summaries += summary
      if (!summary.success) failures += summary.failureMessage("before publish hook")
    }

    val executions = artifacts.map { buildPublishExecution(projectContext, workingDir, version, tag, sharedNotesPath, it) }
    executions.forEach { execution ->
      val summary = executeCommand(executor, execution.command, execution.workingDir, execution.type)
      summaries += summary.copy(data = execution.toData(summary.exitCode))
      if (!summary.success) {
        failures += summary.failureMessage("${execution.type} publish")
      }
    }

    context.publish.afterCommands.forEachIndexed { index, command ->
      val summary = executeCommand(executor, command, workingDir, "after-command-$index")
      summaries += summary
      if (!summary.success) failures += summary.failureMessage("after publish hook")
    }

    val data = mapOf(
      "operation" to operation.taskId,
      "version" to version,
      "tag" to tag,
      "artifactCount" to executions.size,
      "artifacts" to executions.mapIndexed { index, execution ->
        val exitCode = summaries.filter { it.type == execution.type }.getOrNull(index)?.exitCode
        execution.toData(exitCode)
      },
      "commands" to summaries.map { it.toData() },
    )

    return if (failures.isEmpty()) {
      TaskResult.success("Published release $tag across ${executions.size} artifact target(s)", data = data)
    } else {
      TaskResult.failure("Release task $id failed: ${failures.joinToString("; ")}", data = data)
    }
  }

  private fun executeRollback(
    projectContext: ProjectContext,
    executor: CommandExecutor,
    workingDir: Path,
    args: List<String>,
  ): TaskResult {
    if (!context.rollback.enabled) {
      return TaskResult.skipped("Release rollback disabled")
    }

    val version = resolveRequestedVersion(projectContext, executor, workingDir, args)
      ?: return TaskResult.failure("Release task $id failed: unable to resolve a release version for rollback")
    val tag = buildTag(version)
    val summaries = mutableListOf<CommandSummary>()
    val failures = mutableListOf<String>()

    context.rollback.beforeCommands.forEachIndexed { index, command ->
      val summary = executeCommand(executor, command, workingDir, "before-command-$index")
      summaries += summary
      if (!summary.success) failures += summary.failureMessage("before rollback hook")
    }

    if (context.rollback.deleteTag) {
      val existingTag = executor.executeWithResult(
        "git tag --list ${ShellArgumentSanitizer.escapeShellArg(tag)}",
        workingDir.toString(),
      )
      val lookupSummary = CommandSummary(
        type = "tag-lookup",
        command = "git tag --list ${ShellArgumentSanitizer.escapeShellArg(tag)}",
        workingDirectory = workingDir.toString(),
        exitCode = existingTag.exitCode,
        stdout = existingTag.stdout,
        stderr = existingTag.stderr,
      )
      summaries += lookupSummary
      if (existingTag.success && existingTag.stdout.lines().any { it.trim() == tag }) {
        val deleteSummary = executeCommand(
          executor,
          "git tag -d ${ShellArgumentSanitizer.escapeShellArg(tag)}",
          workingDir,
          "delete-tag",
        )
        summaries += deleteSummary
        if (!deleteSummary.success) failures += deleteSummary.failureMessage("delete tag")
      }
    }

    val restoreTargets = mutableListOf<String>()
    if (context.rollback.restoreVersionFiles) {
      restoreTargets += context.versionFiles.mapNotNull { file ->
        val path = resolveWorkingPath(projectContext, file, "Release version file")
        if (Files.exists(path)) projectContext.dir.relativize(path).toString() else null
      }
    }
    if (context.rollback.restoreChangelog && context.changelog) {
      val path = resolveWorkingPath(projectContext, context.changelogPath, "Release changelog path")
      if (Files.exists(path)) {
        restoreTargets += projectContext.dir.relativize(path).toString()
      }
    }
    if (restoreTargets.isNotEmpty()) {
      val restoreCommand = "git checkout -- ${ShellArgumentSanitizer.escapeShellArgs(restoreTargets.sorted())}"
      val restoreSummary = executeCommand(executor, restoreCommand, workingDir, "restore-files")
      summaries += restoreSummary
      if (!restoreSummary.success) failures += restoreSummary.failureMessage("restore files")
    }

    context.rollback.afterCommands.forEachIndexed { index, command ->
      val summary = executeCommand(executor, command, workingDir, "after-command-$index")
      summaries += summary
      if (!summary.success) failures += summary.failureMessage("after rollback hook")
    }

    val performedActions = summaries.map { it.type }
    if (performedActions.none { it != "tag-lookup" }) {
      return TaskResult.skipped("No rollback actions were required for $tag")
    }

    val data = mapOf(
      "operation" to operation.taskId,
      "version" to version,
      "tag" to tag,
      "commands" to summaries.map { it.toData() },
      "restoredFiles" to restoreTargets.sorted(),
    )

    return if (failures.isEmpty()) {
      TaskResult.success("Rolled back local release state for $tag", data = data)
    } else {
      TaskResult.failure("Release task $id failed: ${failures.joinToString("; ")}", data = data)
    }
  }

  private fun buildReleasePlan(
    projectContext: ProjectContext,
    executor: CommandExecutor,
    workingDir: Path,
    args: List<String>,
  ): ReleasePlan {
    val lastTag = findLastTag(executor, workingDir)
    val commitRange = lastTag?.let { "$it..HEAD" } ?: "HEAD"
    val commits = readCommits(executor, workingDir, commitRange)
    val strategy = context.normalizedStrategy()

    if (strategy !in setOf("semantic", "calendar", "manual")) {
      throw IllegalArgumentException(
        "Unsupported release.strategy '${context.strategy}'. Expected semantic, calendar, or manual.",
      )
    }

    if (strategy != "manual" && commits.isEmpty()) {
      return ReleasePlan(
        strategy = strategy,
        previousVersion = resolveCurrentVersion(projectContext, executor, workingDir, lastTag),
        nextVersion = "",
        tag = "",
        commitRange = commitRange,
        commits = commits,
        bump = ReleaseBump.NONE,
        changelogSection = "",
        skipReason = if (lastTag != null) {
          "No commits found since $lastTag"
        } else {
          "No commits found to prepare a release"
        },
      )
    }

    val previousVersion = resolveCurrentVersion(projectContext, executor, workingDir, lastTag)
    val nextVersion = when (strategy) {
      "semantic" -> {
        val base = previousVersion ?: "0.0.0"
        bumpSemver(base, determineBump(commits))
      }
      "calendar" -> nextCalendarVersion(previousVersion)
      "manual" -> resolveManualVersion(args)
      else -> error("Unhandled strategy: $strategy")
    }
    val bump = if (strategy == "semantic") determineBump(commits) else ReleaseBump.PATCH
    val tag = buildTag(nextVersion)

    return ReleasePlan(
      strategy = strategy,
      previousVersion = previousVersion,
      nextVersion = nextVersion,
      tag = tag,
      commitRange = commitRange,
      commits = commits,
      bump = bump,
      changelogSection = renderChangelogSection(tag, commits),
    )
  }

  private fun resolveRequestedVersion(
    projectContext: ProjectContext,
    executor: CommandExecutor,
    workingDir: Path,
    args: List<String>,
  ): String? {
    val explicit = args.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    if (explicit != null) return explicit
    context.currentVersion?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    context.versionFiles.forEach { file ->
      val path = resolveWorkingPath(projectContext, file, "Release version file")
      if (Files.exists(path)) {
        parseVersion(Files.readString(path))?.let { return it }
      }
    }
    val lastTag = findLastTag(executor, workingDir)
    return when {
      lastTag == null -> null
      context.tagPrefix.isNotEmpty() && lastTag.startsWith(context.tagPrefix) -> lastTag.removePrefix(context.tagPrefix)
      else -> parseVersion(lastTag)
    }
  }

  private fun resolveCurrentVersion(
    projectContext: ProjectContext,
    executor: CommandExecutor,
    workingDir: Path,
    lastTag: String?,
  ): String? {
    context.currentVersion?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    context.versionFiles.forEach { file ->
      val path = resolveWorkingPath(projectContext, file, "Release version file")
      if (Files.exists(path)) {
        parseVersion(Files.readString(path))?.let { return it }
      }
    }
    if (lastTag != null && lastTag.startsWith(context.tagPrefix)) {
      return lastTag.removePrefix(context.tagPrefix)
    }
    if (lastTag != null && context.tagPrefix.isEmpty()) {
      return lastTag
    }
    val describe = executor.executeWithResult("git describe --tags --abbrev=0", workingDir.toString())
    return if (describe.success) parseVersion(describe.stdout.trim()) else null
  }

  private fun findLastTag(executor: CommandExecutor, workingDir: Path): String? {
    val matchFlag = if (context.tagPrefix.isBlank()) {
      ""
    } else {
      " --match ${ShellArgumentSanitizer.escapeShellArg("${context.tagPrefix}*")}"
    }
    val result = executor.executeWithResult(
      "git describe --tags --abbrev=0$matchFlag",
      workingDir.toString(),
    )
    return result.stdout.trim().takeIf { result.success && it.isNotEmpty() }
  }

  private fun readCommits(
    executor: CommandExecutor,
    workingDir: Path,
    range: String,
  ): List<ReleaseCommit> {
    val command = buildString {
      append("git log ")
      append(ShellArgumentSanitizer.escapeShellArg(range))
      append(" --pretty=format:%H%x1f%s%x1f%b%x1e")
    }
    val result = executor.executeWithResult(command, workingDir.toString())
    if (!result.success || result.stdout.isBlank()) {
      return emptyList()
    }
    return result.stdout
      .split('\u001e')
      .mapNotNull { raw ->
        if (raw.isBlank()) return@mapNotNull null
        val fields = raw.split('\u001f')
        val hash = fields.getOrNull(0)?.trim().orEmpty()
        val subject = fields.getOrNull(1)?.trim().orEmpty()
        val body = fields.getOrNull(2)?.trim().orEmpty()
        if (hash.isBlank() || subject.isBlank()) return@mapNotNull null
        parseCommit(hash, subject, body)
      }
  }

  private fun parseCommit(hash: String, subject: String, body: String): ReleaseCommit {
    val match = Regex("""^([a-z]+)(\(([^)]+)\))?(!)?:\s+(.+)$""").matchEntire(subject)
    val type = match?.groupValues?.get(1)?.lowercase()
    val scope = match?.groupValues?.get(3)?.ifBlank { null }
    val description = match?.groupValues?.get(5) ?: subject
    val breaking = subject.contains("!:") || body.contains("BREAKING CHANGE") || body.contains("BREAKING-CHANGE")
    return ReleaseCommit(
      hash = hash,
      subject = subject,
      body = body,
      type = type,
      scope = scope,
      description = description,
      breaking = breaking,
    )
  }

  private fun determineBump(commits: List<ReleaseCommit>): ReleaseBump =
    when {
      commits.any { it.breaking } -> ReleaseBump.MAJOR
      commits.any { it.type == "feat" } -> ReleaseBump.MINOR
      commits.isNotEmpty() -> ReleaseBump.PATCH
      else -> ReleaseBump.NONE
    }

  private fun bumpSemver(version: String, bump: ReleaseBump): String {
    val parsed = parseSemver(version)
      ?: throw IllegalArgumentException("Unable to derive semantic version from '$version'")
    return when (bump) {
      ReleaseBump.MAJOR -> "${parsed.major + 1}.0.0"
      ReleaseBump.MINOR -> "${parsed.major}.${parsed.minor + 1}.0"
      ReleaseBump.PATCH, ReleaseBump.NONE -> "${parsed.major}.${parsed.minor}.${parsed.patch + 1}"
    }
  }

  private fun nextCalendarVersion(previousVersion: String?): String {
    val today = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    if (previousVersion == null || !previousVersion.startsWith(today)) {
      return today
    }
    val suffix = previousVersion.removePrefix(today).removePrefix(".")
    val sequence = suffix.toIntOrNull() ?: 0
    return "$today.${sequence + 1}"
  }

  private fun resolveManualVersion(args: List<String>): String {
    val version = args.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
      ?: context.manualVersion?.trim()?.takeIf { it.isNotEmpty() }
      ?: throw IllegalArgumentException("release.strategy=manual requires release.manualVersion or a task argument")
    require(version.matches(Regex("^[0-9A-Za-z][0-9A-Za-z._-]*$"))) {
      "Invalid manual release version '$version'"
    }
    return version
  }

  private fun renderChangelogSection(tag: String, commits: List<ReleaseCommit>): String {
    val date = LocalDate.now(ZoneOffset.UTC)
    val breaking = commits.filter { it.breaking }
    val features = commits.filter { !it.breaking && it.type == "feat" }
    val fixes = commits.filter { !it.breaking && it.type in setOf("fix", "perf", "revert") }
    val maintenance = commits.filter { it !in breaking && it !in features && it !in fixes }

    return buildString {
      append("## $tag - $date\n")
      appendSection("Breaking Changes", breaking)
      appendSection("Features", features)
      appendSection("Fixes", fixes)
      appendSection("Maintenance", maintenance)
    }.trimEnd()
  }

  private fun StringBuilder.appendSection(title: String, commits: List<ReleaseCommit>) {
    if (commits.isEmpty()) return
    append("\n### $title\n")
    commits.forEach { append("- ${it.subject} (${it.hash.take(7)})\n") }
  }

  private fun updateVersionFile(path: Path, version: String) {
    Files.createDirectories(requireNotNull(path.parent))
    val updated = when {
      Files.notExists(path) -> "$version\n"
      else -> {
        val existing = Files.readString(path)
        when {
          Regex(""""version"\s*:\s*"[^"]+"""").containsMatchIn(existing) ->
            existing.replaceFirst(Regex(""""version"\s*:\s*"[^"]+""""), "\"version\": \"$version\"")
          Regex("(?m)^version\\s*=\\s*.+$").containsMatchIn(existing) ->
            existing.replaceFirst(Regex("(?m)^version\\s*=\\s*.+$"), "version=$version")
          Regex("(?m)^version\\s*:\\s*.+$").containsMatchIn(existing) ->
            existing.replaceFirst(Regex("(?m)^version\\s*:\\s*.+$"), "version: $version")
          else -> "$version\n"
        }
      }
    }
    Files.writeString(path, updated)
  }

  private fun updateChangelog(path: Path, section: String) {
    Files.createDirectories(requireNotNull(path.parent))
    val existing = if (Files.exists(path)) Files.readString(path) else ""
    if (existing.contains("## ${section.lineSequence().first().removePrefix("## ")}")) {
      return
    }
    val updated = when {
      existing.isBlank() -> "# Changelog\n\n$section\n"
      existing.startsWith("# Changelog") -> {
        val remainder = existing.removePrefix("# Changelog").trimStart('\n', '\r')
        "# Changelog\n\n$section\n\n$remainder".trimEnd() + "\n"
      }
      else -> "$section\n\n$existing".trimEnd() + "\n"
    }
    Files.writeString(path, updated)
  }

  private fun resolveExistingNotesPath(projectContext: ProjectContext): Path? {
    val releaseNotes = if (context.releaseNotesPath.isBlank()) null else {
      resolveWorkingPath(projectContext, context.releaseNotesPath, "Release notes path")
    }
    if (releaseNotes != null && Files.exists(releaseNotes)) {
      return releaseNotes
    }
    if (context.changelog) {
      val changelog = resolveWorkingPath(projectContext, context.changelogPath, "Release changelog path")
      if (Files.exists(changelog)) {
        return changelog
      }
    }
    return null
  }

  private fun buildPublishExecution(
    projectContext: ProjectContext,
    workingDir: Path,
    version: String,
    tag: String,
    sharedNotesPath: Path?,
    artifact: ReleaseArtifact,
  ): PublishExecution {
    return when (artifact.normalizedType()) {
      "npm" -> {
        val artifactWorkingDir = artifact.directory
          ?.takeIf { it.isNotBlank() }
          ?.let { resolveWorkingPath(projectContext, it, "npm artifact directory") }
          ?: workingDir
        val command = buildString {
          append("npm publish")
          artifact.registry?.takeIf { it.isNotBlank() }?.let {
            append(" --registry ")
            append(ShellArgumentSanitizer.escapeShellArg(it))
          }
        }
        PublishExecution(type = "npm", command = command, workingDir = artifactWorkingDir, metadata = mapOf("registry" to (artifact.registry ?: "")))
      }
      "docker" -> {
        val image = artifact.image?.trim()?.takeIf { it.isNotEmpty() }
          ?: throw IllegalArgumentException("Docker release artifacts require artifacts[].image")
        val repository = artifact.registry?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }?.let { "$it/$image" } ?: image
        val tags = (listOf(version) + artifact.tags.map { if (it == "version") version else it }).distinct()
        val dockerContext = artifact.context?.takeIf { it.isNotBlank() }
          ?.let { resolveWorkingPath(projectContext, it, "Docker build context") }
          ?: workingDir
        val command = buildString {
          append("docker build")
          artifact.dockerfile?.takeIf { it.isNotBlank() }?.let {
            val dockerfile = resolveWorkingPath(projectContext, it, "Dockerfile path")
            append(" -f ")
            append(ShellArgumentSanitizer.escapeShellArg(dockerfile.toString()))
          }
          tags.forEach { releaseTag ->
            append(" -t ")
            append(ShellArgumentSanitizer.escapeShellArg("$repository:$releaseTag"))
          }
          append(' ')
          append(ShellArgumentSanitizer.escapeShellArg(dockerContext.toString()))
          tags.forEach { releaseTag ->
            append(" && docker push ")
            append(ShellArgumentSanitizer.escapeShellArg("$repository:$releaseTag"))
          }
        }
        PublishExecution(
          type = "docker",
          command = command,
          workingDir = workingDir,
          metadata = mapOf(
            "repository" to repository,
            "tags" to tags,
          ),
        )
      }
      "github-release" -> {
        val notesPath = artifact.notesPath?.takeIf { it.isNotBlank() }
          ?.let { resolveWorkingPath(projectContext, it, "GitHub release notes path") }
          ?.takeIf(Files::exists)
          ?: sharedNotesPath
        val assets = expandGlobs(projectContext.dir, artifact.assets)
        val command = buildString {
          append("gh release create ")
          append(ShellArgumentSanitizer.escapeShellArg(tag))
          append(" --title ")
          append(ShellArgumentSanitizer.escapeShellArg(tag))
          notesPath?.let {
            append(" --notes-file ")
            append(ShellArgumentSanitizer.escapeShellArg(it.toString()))
          }
          assets.matches.forEach {
            append(' ')
            append(ShellArgumentSanitizer.escapeShellArg(it.toString()))
          }
        }
        PublishExecution(
          type = "github-release",
          command = command,
          workingDir = workingDir,
          metadata = mapOf(
            "assets" to assets.matches.map { projectContext.dir.relativize(it).toString() },
            "unmatchedAssets" to assets.unmatchedPatterns,
            "notesPath" to (notesPath?.let { projectContext.dir.relativize(it).toString() } ?: ""),
            "tag" to tag,
          ),
        )
      }
      else -> throw IllegalArgumentException(
        "Unsupported release artifact type '${artifact.type}'. Expected npm, docker, or github-release.",
      )
    }
  }

  private fun executeCommand(
    executor: CommandExecutor,
    command: String,
    workingDir: Path,
    type: String,
  ): CommandSummary {
    val result = executor.executeWithResult(command, workingDir.toString())
    return CommandSummary(
      type = type,
      command = command,
      workingDirectory = workingDir.toString(),
      exitCode = result.exitCode,
      stdout = result.stdout,
      stderr = result.stderr,
    )
  }

  private fun resolveWorkingPath(projectContext: ProjectContext, candidate: String, description: String): Path =
    projectContext.resolvePath(Path.of(context.workingDirectory).resolve(candidate).normalize().toString(), description)

  private fun buildTag(version: String): String = context.tagPrefix + version

  private fun parseVersion(text: String): String? {
    val trimmed = text.trim().removePrefix(context.tagPrefix)
    val semver = Regex("""\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?""").find(trimmed)?.value
    if (semver != null) return semver
    val calendar = Regex("""\d{4}\.\d{2}\.\d{2}(?:\.\d+)?""").find(trimmed)?.value
    return calendar
  }

  private fun parseSemver(version: String): SemverVersion? {
    val match = Regex("""^(\d+)\.(\d+)\.(\d+)(?:[-+].*)?$""").matchEntire(version) ?: return null
    return SemverVersion(
      major = match.groupValues[1].toInt(),
      minor = match.groupValues[2].toInt(),
      patch = match.groupValues[3].toInt(),
    )
  }

  private fun expandGlobs(root: Path, patterns: List<String>): GlobResolution {
    if (patterns.isEmpty()) return GlobResolution(emptyList(), emptyList())
    val matches = mutableListOf<Path>()
    val unmatched = mutableListOf<String>()
    patterns.distinct().forEach { pattern ->
      val matcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
      val found = Files.walk(root, 8).use { paths ->
        paths
          .filter(Files::isRegularFile)
          .filter { matcher.matches(root.relativize(it)) }
          .toList()
      }
      if (found.isEmpty()) unmatched += pattern else matches += found
    }
    return GlobResolution(matches.distinct().sortedBy { root.relativize(it).toString() }, unmatched)
  }

  private data class ReleasePlan(
    val strategy: String,
    val previousVersion: String?,
    val nextVersion: String,
    val tag: String,
    val commitRange: String,
    val commits: List<ReleaseCommit>,
    val bump: ReleaseBump,
    val changelogSection: String,
    val skipReason: String? = null,
  )

  private data class ReleaseCommit(
    val hash: String,
    val subject: String,
    val body: String,
    val type: String?,
    val scope: String?,
    val description: String,
    val breaking: Boolean,
  ) {
    fun toData(): Map<String, Any> = buildMap {
      put("hash", hash)
      put("subject", subject)
      put("description", description)
      put("breaking", breaking)
      type?.let { put("type", it) }
      scope?.let { put("scope", it) }
    }
  }

  private data class PublishExecution(
    val type: String,
    val command: String,
    val workingDir: Path,
    val metadata: Map<String, Any>,
  ) {
    fun toData(exitCode: Int?): Map<String, Any> = buildMap {
      put("type", type)
      put("command", command)
      put("workingDirectory", workingDir.toString())
      metadata.forEach { (key, value) ->
        if (value is String && value.isBlank()) return@forEach
        put(key, value)
      }
      exitCode?.let { put("exitCode", it) }
    }
  }

  private data class CommandSummary(
    val type: String,
    val command: String,
    val workingDirectory: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val data: Map<String, Any> = emptyMap(),
  ) {
    val success: Boolean get() = exitCode == 0

    fun failureMessage(action: String): String = "$action failed: ${commandFailureMessage(exitCode, stdout, stderr)}"

    fun toData(): Map<String, Any> = buildMap {
      put("type", type)
      put("command", command)
      put("workingDirectory", workingDirectory)
      put("exitCode", exitCode)
      if (stdout.isNotBlank()) put("stdout", stdout.trim())
      if (stderr.isNotBlank()) put("stderr", stderr.trim())
      data.forEach { (key, value) -> put(key, value) }
    }
  }

  private data class SemverVersion(val major: Int, val minor: Int, val patch: Int)

  private data class GlobResolution(
    val matches: List<Path>,
    val unmatchedPatterns: List<String>,
  )

  private enum class ReleaseBump {
    NONE,
    PATCH,
    MINOR,
    MAJOR,
  }

  enum class Operation(
    val taskId: String,
    val description: String,
  ) {
    PREPARE("release-prepare", "Analyze commits, compute the next version, and prepare release files"),
    PUBLISH("release-publish", "Publish configured artifacts for the prepared release"),
    ROLLBACK("release-rollback", "Safely roll back local release files and tags"),
  }

  companion object {
    private fun commandFailureMessage(exitCode: Int, stdout: String, stderr: String): String {
      val detail = listOf(stderr.trim(), stdout.trim()).firstOrNull { it.isNotBlank() }
      return if (detail != null) {
        "exit code $exitCode: $detail"
      } else {
        "exit code $exitCode"
      }
    }
  }
}
