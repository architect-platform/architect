package io.github.architectplatform.plugins.javascript

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.project.resolvePath
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase
import io.github.architectplatform.api.core.utils.ShellArgumentSanitizer
import java.nio.file.Files
import java.nio.file.Path

/**
 * Task implementation for executing JavaScript/Node.js commands via package managers.
 *
 * Executes npm/yarn/pnpm commands in the configured working directory.
 *
 * @property command The command to execute (e.g., "install", "build", "test")
 * @property phase The workflow phase this task belongs to
 * @property context The JavaScript context containing package manager configuration
 */
class JavaScriptTask(
  private val command: String,
  private val phase: Phase,
  private val context: JavaScriptContext,
) : Task {
  override val id: String = "javascript-$command"

  override fun phase(): Phase = phase

  override fun description(): String = when (command) {
    "install" -> "Install JavaScript dependencies"
    "workspace-check" -> "Detect JavaScript workspace/monorepo configuration"
    "lockfile-check" -> "Validate lockfile presence and consistency for configured package manager"
    "audit" -> "Run dependency security audit"
    "build" -> "Run project build script"
    "test" -> "Run project test script"
    "lint" -> "Run project lint script"
    "dev" -> "Run project development script"
    "version" -> "Bump package version"
    "publish" -> "Publish package to registry"
    else -> "Run JavaScript task '$command'"
  }

  /**
   * Executes the JavaScript command using the configured package manager.
   *
   * @param environment Execution environment providing CommandExecutor service
   * @param projectContext The project context
   * @param args Additional arguments to pass to the command
   * @return TaskResult indicating success or failure
   */
  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult {
    val commandExecutor = environment.service(CommandExecutor::class.java)
    val workingDir = try {
      projectContext.resolvePath(context.workingDirectory, "JavaScript working directory")
    } catch (e: IllegalArgumentException) {
      return TaskResult.failure(
        "JavaScript task: $id failed with invalid working directory: ${e.message}",
      )
    }

    if (command == "workspace-check") {
      val detected = detectWorkspace(workingDir)
      return TaskResult.success(
        "JavaScript workspace detection: $detected",
        data = mapOf("workspaceType" to detected),
      )
    }

    if (command == "lockfile-check") {
      val validation = validateLockfile(workingDir)
      return if (validation.valid) {
        TaskResult.success(validation.message)
      } else {
        TaskResult.failure(validation.message)
      }
    }

    val fullCommand = buildCommand(command, args)

    try {
      commandExecutor.execute(fullCommand, workingDir = workingDir.toString())
    } catch (e: Exception) {
      return TaskResult.failure(
        "JavaScript task: $id failed with exception: ${e.message}",
      )
    }

    return TaskResult.success("JavaScript task: $id completed successfully")
  }

  /**
   * Builds the full command string based on package manager and command type.
   *
   * @param command The base command (install, build, test, run, etc.)
   * @param args Additional arguments
   * @return The complete command string to execute
   */
  private fun buildCommand(command: String, args: List<String>): String {
    val packageManager = context.normalizedPackageManager()
    val yarnMode = context.normalizedYarnMode()
    val argsString = if (args.isNotEmpty()) " ${ShellArgumentSanitizer.escapeShellArgs(args)}" else ""
    val firstArg = args.firstOrNull()?.trim()?.lowercase()
    val versionBump = firstArg?.takeIf { it in setOf("major", "minor", "patch", "prerelease") } ?: context.defaultVersionBump
    val versionArgs = if (firstArg in setOf("major", "minor", "patch", "prerelease")) args.drop(1) else args
    val versionArgsString = if (versionArgs.isNotEmpty()) " ${ShellArgumentSanitizer.escapeShellArgs(versionArgs)}" else ""

    return when (command) {
      "install" -> when (packageManager) {
        "npm" -> "npm install$argsString"
        "yarn" -> if (yarnMode == "berry") "yarn install --immutable$argsString" else "yarn install$argsString"
        "pnpm" -> "pnpm install$argsString"
        "bun" -> "bun install$argsString"
        else -> "npm install$argsString"
      }
      "ci" -> when (packageManager) {
        "npm" -> "npm ci$argsString"
        "yarn" -> if (yarnMode == "berry") "yarn install --immutable$argsString" else "yarn install --frozen-lockfile$argsString"
        "pnpm" -> "pnpm install --frozen-lockfile$argsString"
        "bun" -> "bun install --frozen-lockfile$argsString"
        else -> "npm ci$argsString"
      }
      "audit" -> when (packageManager) {
        "npm" -> "npm audit$argsString"
        "yarn" -> "yarn npm audit$argsString"
        "pnpm" -> "pnpm audit$argsString"
        "bun" -> "bun audit$argsString"
        else -> "npm audit$argsString"
      }
      "build" -> when (packageManager) {
        "npm" -> "npm run build$argsString"
        "yarn" -> "yarn build$argsString"
        "pnpm" -> "pnpm build$argsString"
        "bun" -> "bun run build$argsString"
        else -> "$packageManager run build$argsString"
      }
      "test" -> when (packageManager) {
        "npm" -> "npm test$argsString"
        "yarn" -> "yarn test$argsString"
        "pnpm" -> "pnpm test$argsString"
        "bun" -> "bun test$argsString"
        else -> "$packageManager test$argsString"
      }
      "lint" -> when (packageManager) {
        "npm" -> "npm run lint$argsString"
        "yarn" -> "yarn lint$argsString"
        "pnpm" -> "pnpm lint$argsString"
        "bun" -> "bun run lint$argsString"
        else -> "$packageManager run lint$argsString"
      }
      "dev" -> when (packageManager) {
        "npm" -> "npm run dev$argsString"
        "yarn" -> "yarn dev$argsString"
        "pnpm" -> "pnpm dev$argsString"
        "bun" -> "bun run dev$argsString"
        else -> "$packageManager run dev$argsString"
      }
      "start" -> when (packageManager) {
        "npm" -> "npm start$argsString"
        "yarn" -> "yarn start$argsString"
        "pnpm" -> "pnpm start$argsString"
        "bun" -> "bun run start$argsString"
        else -> "$packageManager start$argsString"
      }
      "version" -> when (packageManager) {
        "npm" -> "npm version $versionBump$versionArgsString"
        "yarn" -> "yarn version --$versionBump$versionArgsString"
        "pnpm" -> "pnpm version $versionBump$versionArgsString"
        "bun" -> "bun version $versionBump$versionArgsString"
        else -> "npm version $versionBump$versionArgsString"
      }
      "publish" -> when (packageManager) {
        "npm" -> "npm publish --access ${context.publishAccess}$argsString"
        "yarn" -> "yarn npm publish --access ${context.publishAccess}$argsString"
        "pnpm" -> "pnpm publish --access ${context.publishAccess}$argsString"
        "bun" -> "bun publish$argsString"
        else -> "npm publish --access ${context.publishAccess}$argsString"
      }
      else -> "$packageManager run $command$argsString"
    }
  }

  private fun detectWorkspace(workingDir: Path): String {
    val pnpmWorkspace = workingDir.resolve("pnpm-workspace.yaml")
    val rush = workingDir.resolve("rush.json")
    val turbo = workingDir.resolve("turbo.json")
    val nx = workingDir.resolve("nx.json")
    val packageJson = workingDir.resolve("package.json")
    val packageJsonText = runCatching { Files.readString(packageJson) }.getOrDefault("")

    return when {
      Files.exists(pnpmWorkspace) -> "pnpm-workspace"
      Files.exists(rush) -> "rush"
      Files.exists(turbo) -> "turbo"
      Files.exists(nx) -> "nx"
      "\"workspaces\"" in packageJsonText -> "package-json-workspaces"
      else -> "single-package"
    }
  }

  private data class LockfileValidation(
    val valid: Boolean,
    val message: String,
  )

  private fun validateLockfile(workingDir: Path): LockfileValidation {
    val packageManager = context.normalizedPackageManager()
    val lockfile = when (packageManager) {
      "npm" -> "package-lock.json"
      "yarn" -> "yarn.lock"
      "pnpm" -> "pnpm-lock.yaml"
      "bun" -> "bun.lockb"
      else -> "package-lock.json"
    }
    val lockfilePath = workingDir.resolve(lockfile)
    if (!Files.exists(lockfilePath)) {
      return LockfileValidation(
        valid = false,
        message = "JavaScript task: $id failed because required lockfile '$lockfile' was not found in $workingDir",
      )
    }
    return LockfileValidation(
      valid = true,
      message = "Lockfile validation passed: found $lockfile",
    )
  }
}
