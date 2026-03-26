package io.github.architectplatform.core.execution

import io.github.architectplatform.api.core.tasks.TaskPermission
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import org.slf4j.LoggerFactory

internal data class LaunchedProcess(
  val process: Process,
  val cleanup: () -> Unit = {},
)

internal object SandboxedProcessLauncher {
  private val logger = LoggerFactory.getLogger(SandboxedProcessLauncher::class.java)
  private val sandboxExecBinary = File("/usr/bin/sandbox-exec")

  fun launch(
    command: List<String>,
    workingDir: String? = null,
    redirectErrorStream: Boolean = false,
    env: Map<String, String> = emptyMap(),
  ): LaunchedProcess {
    val permissionContext = TaskPermissionScope.current()
    permissionContext?.let { context ->
      if (TaskPermission.PROCESS_EXEC !in context.permissions) {
        throw IllegalStateException(
          "Task '${context.taskId}' requires permission '${TaskPermission.PROCESS_EXEC.wireName}' to launch subprocesses"
        )
      }
    }

    val profilePath = permissionContext?.let { createSandboxProfile(it, workingDir) }
    val actualCommand = if (profilePath != null) {
      listOf(sandboxExecBinary.absolutePath, "-f", profilePath.toString()) + command
    } else {
      command
    }

    val processBuilder = ProcessBuilder(actualCommand)
    workingDir?.let { processBuilder.directory(File(it)) }
    processBuilder.redirectErrorStream(redirectErrorStream)
    if (env.isNotEmpty()) {
      processBuilder.environment().putAll(env)
    }

    val process = processBuilder.start()
    return LaunchedProcess(process) {
      profilePath?.let { Files.deleteIfExists(it) }
    }
  }

  private fun createSandboxProfile(
    context: TaskPermissionContext,
    workingDir: String?,
  ): Path? {
    val needsSandbox =
      TaskPermission.NETWORK_OUTBOUND !in context.permissions ||
        TaskPermission.FILE_SYSTEM_WRITE !in context.permissions ||
        TaskPermission.FILE_SYSTEM_READ !in context.permissions

    if (!needsSandbox) {
      return null
    }

    if (!isMacOs() || !sandboxExecBinary.canExecute()) {
      throw IllegalStateException(
        "Task '${context.taskId}' requires subprocess sandboxing, but sandbox-exec is unavailable on this platform"
      )
    }

    val profile = buildMacOsProfile(context.permissions, workingDir)
    return Files.createTempFile("architect-sandbox-", ".sb").also { path ->
      Files.writeString(path, profile)
    }
  }

  private fun buildMacOsProfile(
    permissions: Set<TaskPermission>,
    workingDir: String?,
  ): String {
    val lines = mutableListOf(
      "(version 1)",
      "(allow default)",
    )

    if (TaskPermission.NETWORK_OUTBOUND !in permissions) {
      lines += "(deny network-outbound)"
    }

    if (TaskPermission.FILE_SYSTEM_WRITE !in permissions) {
      lines += "(deny file-write*)"
    }

    if (TaskPermission.FILE_SYSTEM_READ !in permissions && workingDir != null) {
      lines += "(deny file-read* (subpath \"${escapeSbpl(workingDir)}\"))"
    } else if (TaskPermission.FILE_SYSTEM_READ !in permissions) {
      logger.warn("Skipping file read restriction for subprocess without a working directory")
    }

    return lines.joinToString("\n")
  }

  private fun escapeSbpl(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

  private fun isMacOs(): Boolean = System.getProperty("os.name").contains("mac", ignoreCase = true)
}