package io.github.architectplatform.engine.core.plugin.protocol

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.project.ProjectContext
import io.github.architectplatform.api.core.tasks.Environment
import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.phase.Phase
import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlin.concurrent.thread
import java.util.concurrent.atomic.AtomicInteger

/**
 * Bridges an external process plugin (any language) to the JVM ArchitectPlugin interface.
 *
 * Communicates with the subprocess using Architect Plugin Protocol v1 (JSON-RPC 2.0
 * over stdin/stdout). The subprocess is launched once, kept alive for task listing,
 * and re-launched per task execution (or kept as a long-lived daemon).
 */
class ProcessPluginAdapter(
  private val pluginId: String,
  private val command: List<String>,
  private val workingDir: String? = null,
) : ArchitectPlugin<Any> {

  constructor(
    pluginId: String,
    command: String,
    workingDir: String? = null,
  ) : this(
    pluginId = pluginId,
    command = shellCommand(command),
    workingDir = workingDir,
  )

  override val id: String = pluginId
  override val contextKey: String = pluginId.replace("-", "")
  override val ctxClass: Class<Any> = Any::class.java
  override var context: Any = Unit

  private val logger = LoggerFactory.getLogger(this::class.java)
  private val objectMapper = ObjectMapper().registerKotlinModule()
  private val requestId = AtomicInteger(0)

  private var cachedTasks: List<TaskDescriptor> = emptyList()

  override fun register(registry: TaskRegistry) {
    val tasks = listTasks()
    cachedTasks = tasks
    tasks.forEach { descriptor ->
      registry.add(ProcessBridgeTask(descriptor, this))
    }
  }

  /**
   * Launches the plugin process, sends init + listTasks, returns task descriptors.
   */
  internal fun listTasks(): List<TaskDescriptor> {
    return withProcess { reader, writer ->
      // 1. init
      val initReq = JsonRpcRequest(
        id = requestId.incrementAndGet(),
        method = PluginProtocol.METHOD_INIT,
        params = mapOf("config" to context, "protocol_version" to PluginProtocol.VERSION),
      )
      sendRequest(writer, initReq)
      val initResp = readResponse(reader)
      if (initResp.error != null) {
        throw IllegalStateException("Plugin $pluginId init failed: ${initResp.error.message}")
      }

      // 2. listTasks
      val listReq = JsonRpcRequest(
        id = requestId.incrementAndGet(),
        method = PluginProtocol.METHOD_LIST_TASKS,
      )
      sendRequest(writer, listReq)
      val listResp = readResponse(reader)
      if (listResp.error != null) {
        throw IllegalStateException("Plugin $pluginId listTasks failed: ${listResp.error.message}")
      }

      // 3. shutdown
      val shutdownReq = JsonRpcRequest(
        id = requestId.incrementAndGet(),
        method = PluginProtocol.METHOD_SHUTDOWN,
      )
      sendRequest(writer, shutdownReq)

      @Suppress("UNCHECKED_CAST")
      val rawList = listResp.result as? List<Map<String, Any?>> ?: emptyList()
      rawList.map { objectMapper.convertValue(it, TaskDescriptor::class.java) }
    }
  }

  /**
   * Executes a task by launching the plugin process and streaming events.
   */
  internal fun executeTask(
    taskId: String,
    args: List<String>,
    env: Map<String, String>,
  ): TaskResult {
    return withProcess { reader, writer ->
      // init
      val initReq = JsonRpcRequest(
        id = requestId.incrementAndGet(),
        method = PluginProtocol.METHOD_INIT,
        params = mapOf("config" to context, "protocol_version" to PluginProtocol.VERSION),
      )
      sendRequest(writer, initReq)
      readResponse(reader)

      // executeTask
      val execReq = JsonRpcRequest(
        id = requestId.incrementAndGet(),
        method = PluginProtocol.METHOD_EXECUTE_TASK,
        params = mapOf("id" to taskId, "args" to args, "env" to env),
      )
      sendRequest(writer, execReq)
      val execResp = readResponse(reader)
      if (execResp.error != null) {
        return@withProcess TaskResult.failure(execResp.error.message)
      }

      // Read streaming events until "completed" or EOF.
      var lastExitCode = 0
      var errorMessage: String? = null
      var completed = false
      while (true) {
        val line = reader.readLine() ?: break
        if (line.isBlank()) continue
        val event = try {
          objectMapper.readValue(line, TaskEvent::class.java)
        } catch (_: Exception) {
          // Not a structured event — treat as raw output
          logger.info("[{}] {}", pluginId, line)
          continue
        }
        when (event.type) {
          PluginProtocol.EVENT_OUTPUT -> logger.info("[{}] {}", pluginId, event.data ?: "")
          PluginProtocol.EVENT_ERROR -> {
            errorMessage = event.data
            logger.error("[{}] {}", pluginId, event.data ?: "")
          }
          PluginProtocol.EVENT_COMPLETED -> {
            lastExitCode = event.exitCode ?: 0
            completed = true
            break
          }
          PluginProtocol.EVENT_PROGRESS -> logger.debug("[{}] progress: {}%", pluginId, ((event.progress ?: 0.0) * 100).toInt())
        }
      }

      // shutdown
      val shutdownReq = JsonRpcRequest(
        id = requestId.incrementAndGet(),
        method = PluginProtocol.METHOD_SHUTDOWN,
      )
      sendRequest(writer, shutdownReq)

      if (!completed) {
        TaskResult.failure("Plugin $pluginId terminated before sending a completed event")
      } else if (lastExitCode != 0 || errorMessage != null) {
        TaskResult.failure(errorMessage ?: "Process exited with code $lastExitCode")
      } else {
        TaskResult.success()
      }
    }
  }

  private fun <T> withProcess(block: (BufferedReader, OutputStreamWriter) -> T): T {
    val pb = ProcessBuilder(command)
    workingDir?.let { pb.directory(java.io.File(it)) }
    pb.redirectErrorStream(false)
    val process = pb.start()
    val stderrDrainer = thread(name = "process-plugin-${pluginId}-stderr", isDaemon = true) {
      process.errorStream.bufferedReader().useLines { lines ->
        lines.forEach { logger.warn("[{}] {}", pluginId, it) }
      }
    }
    try {
      val reader = BufferedReader(InputStreamReader(process.inputStream))
      val writer = OutputStreamWriter(process.outputStream)
      return block(reader, writer)
    } finally {
      process.destroy()
      if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
        process.destroyForcibly()
        process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
      }
      stderrDrainer.join(1000)
    }
  }

  companion object {
    private fun shellCommand(command: String): List<String> =
      if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
        listOf("cmd", "/c", command)
      } else {
        listOf("sh", "-lc", command)
      }
  }

  private fun sendRequest(writer: OutputStreamWriter, request: JsonRpcRequest) {
    val json = objectMapper.writeValueAsString(request)
    writer.write(json)
    writer.write("\n")
    writer.flush()
  }

  private fun readResponse(reader: BufferedReader): JsonRpcResponse {
    val line = reader.readLine()
      ?: throw IllegalStateException("Plugin $pluginId: unexpected end of stream")
    return objectMapper.readValue(line, JsonRpcResponse::class.java)
  }
}

/**
 * Bridges a process plugin's TaskDescriptor to the JVM Task interface.
 */
internal class ProcessBridgeTask(
  private val descriptor: TaskDescriptor,
  private val adapter: ProcessPluginAdapter,
) : Task {
  override val id: String = descriptor.id

  override fun description(): String = descriptor.description

  override fun phase(): Phase? = descriptor.phase?.let { phaseName ->
    try {
      CoreWorkflow.valueOf(phaseName.uppercase())
    } catch (_: IllegalArgumentException) {
      null
    }
  }

  override fun depends(): List<String> {
    val phaseDeps = phase()?.depends() ?: emptyList()
    return (phaseDeps + descriptor.dependencies).distinct()
  }

  override fun requiresConfirmation(): Boolean = descriptor.requiresConfirmation

  override fun execute(
    environment: Environment,
    projectContext: ProjectContext,
    args: List<String>,
  ): TaskResult {
    val envVars = mutableMapOf<String, String>()
    envVars["ARCHITECT_PROJECT_DIR"] = projectContext.dir.toString()
    envVars["ARCHITECT_PROFILE"] = environment.profile()
    return adapter.executeTask(id, args, envVars)
  }
}
