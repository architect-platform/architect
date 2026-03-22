package io.github.architectplatform.engine.core.plugin.protocol

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Architect Plugin Protocol v1 (APP v1)
 *
 * JSON-RPC 2.0 based protocol over stdin/stdout for language-agnostic plugins.
 * Plugin processes communicate via newline-delimited JSON messages.
 *
 * Protocol flow:
 * 1. Engine launches plugin process
 * 2. Engine sends `init` request with config
 * 3. Engine sends `listTasks` request
 * 4. Engine sends `executeTask` requests as needed
 * 5. Engine sends `shutdown` notification to terminate
 */
object PluginProtocol {
  const val JSON_RPC_VERSION = "2.0"
  const val VERSION = "1.0.0"
  const val METHOD_INIT = "init"
  const val METHOD_LIST_TASKS = "listTasks"
  const val METHOD_EXECUTE_TASK = "executeTask"
  const val METHOD_SHUTDOWN = "shutdown"

  const val EVENT_OUTPUT = "output"
  const val EVENT_PROGRESS = "progress"
  const val EVENT_ERROR = "error"
  const val EVENT_COMPLETED = "completed"
}

// --- JSON-RPC 2.0 Messages ---

data class JsonRpcRequest(
  val jsonrpc: String = PluginProtocol.JSON_RPC_VERSION,
  val id: Int,
  val method: String,
  val params: Map<String, Any?> = emptyMap(),
)

data class JsonRpcResponse(
  val jsonrpc: String = PluginProtocol.JSON_RPC_VERSION,
  val id: Int? = null,
  val result: Any? = null,
  val error: JsonRpcError? = null,
)

data class JsonRpcError(
  val code: Int,
  val message: String,
  val data: Any? = null,
)

// --- Protocol Data Types ---

data class InitParams(
  val config: Map<String, Any?> = emptyMap(),
  @JsonProperty("protocol_version")
  val protocolVersion: String = PluginProtocol.VERSION,
)

data class InitResult(
  val ok: Boolean,
  val name: String? = null,
  val version: String? = null,
)

data class TaskDescriptor(
  val id: String,
  val description: String = "",
  val phase: String? = null,
  val dependencies: List<String> = emptyList(),
  @JsonProperty("requires_confirmation")
  val requiresConfirmation: Boolean = false,
)

data class ExecuteTaskParams(
  val id: String,
  val args: List<String> = emptyList(),
  val env: Map<String, String> = emptyMap(),
)

// --- Streaming Task Events (newline-delimited JSON on stdout) ---
//
// After the JSON-RPC ack for executeTask, the plugin emits one TaskEvent per line
// until EVENT_COMPLETED is sent. EVENT_OUTPUT and EVENT_ERROR carry text, while
// EVENT_PROGRESS reports fractional completion.

data class TaskEvent(
  val type: String,
  val data: String? = null,
  val progress: Double? = null,
  val exitCode: Int? = null,
)
