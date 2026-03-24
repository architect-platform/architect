package io.github.architectplatform.core.plugin.protocol

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ProcessPluginAdapterTest {

  private val objectMapper = ObjectMapper().registerKotlinModule()

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `protocol version constant is set`() {
    assertEquals("1.0.0", PluginProtocol.VERSION)
  }

  @Test
  fun `protocol method constants are defined`() {
    assertEquals("init", PluginProtocol.METHOD_INIT)
    assertEquals("listTasks", PluginProtocol.METHOD_LIST_TASKS)
    assertEquals("executeTask", PluginProtocol.METHOD_EXECUTE_TASK)
    assertEquals("shutdown", PluginProtocol.METHOD_SHUTDOWN)
  }

  @Test
  fun `JsonRpcRequest serializes correctly`() {
    val request = JsonRpcRequest(id = 1, method = "init", params = mapOf("key" to "value"))
    val json = objectMapper.writeValueAsString(request)
    assertTrue(json.contains("\"jsonrpc\":\"2.0\""))
    assertTrue(json.contains("\"id\":1"))
    assertTrue(json.contains("\"method\":\"init\""))
  }

  @Test
  fun `JsonRpcResponse deserializes correctly`() {
    val json = """{"jsonrpc":"2.0","id":1,"result":{"ok":true}}"""
    val response = objectMapper.readValue(json, JsonRpcResponse::class.java)
    assertEquals("2.0", response.jsonrpc)
    assertEquals(1, response.id)
    assertNotNull(response.result)
    assertNull(response.error)
  }

  @Test
  fun `JsonRpcResponse with error deserializes correctly`() {
    val json = """{"jsonrpc":"2.0","id":1,"error":{"code":-32600,"message":"Invalid request"}}"""
    val response = objectMapper.readValue(json, JsonRpcResponse::class.java)
    assertNotNull(response.error)
    assertEquals(-32600, response.error!!.code)
    assertEquals("Invalid request", response.error!!.message)
  }

  @Test
  fun `TaskDescriptor deserializes with defaults`() {
    val json = """{"id":"my-task","description":"A test task"}"""
    val descriptor = objectMapper.readValue(json, TaskDescriptor::class.java)
    assertEquals("my-task", descriptor.id)
    assertEquals("A test task", descriptor.description)
    assertNull(descriptor.phase)
    assertTrue(descriptor.dependencies.isEmpty())
    assertTrue(descriptor.permissions.isEmpty())
    assertFalse(descriptor.requiresConfirmation)
  }

  @Test
  fun `TaskDescriptor deserializes with all fields`() {
    val json = """{"id":"build","description":"Build project","phase":"BUILD","dependencies":["init"],"permissions":["process:exec","network:outbound"],"requires_confirmation":true}"""
    val descriptor = objectMapper.readValue(json, TaskDescriptor::class.java)
    assertEquals("build", descriptor.id)
    assertEquals("BUILD", descriptor.phase)
    assertEquals(listOf("init"), descriptor.dependencies)
    assertEquals(listOf("process:exec", "network:outbound"), descriptor.permissions)
    assertTrue(descriptor.requiresConfirmation)
  }

  @Test
  fun `TaskEvent deserializes output event`() {
    val json = """{"type":"output","data":"Hello world"}"""
    val event = objectMapper.readValue(json, TaskEvent::class.java)
    assertEquals("output", event.type)
    assertEquals("Hello world", event.data)
  }

  @Test
  fun `TaskEvent deserializes completed event with exit code`() {
    val json = """{"type":"completed","exitCode":0}"""
    val event = objectMapper.readValue(json, TaskEvent::class.java)
    assertEquals("completed", event.type)
    assertEquals(0, event.exitCode)
  }

  @Test
  fun `TaskEvent deserializes progress event`() {
    val json = """{"type":"progress","progress":0.75}"""
    val event = objectMapper.readValue(json, TaskEvent::class.java)
    assertEquals("progress", event.type)
    assertEquals(0.75, event.progress)
  }

  @Test
  fun `InitParams has correct defaults`() {
    val params = InitParams()
    assertTrue(params.config.isEmpty())
    assertEquals(PluginProtocol.VERSION, params.protocolVersion)
  }

  @Test
  fun `ProcessPluginAdapter has correct id and contextKey`() {
    val adapter = ProcessPluginAdapter(
      pluginId = "my-go-plugin",
      command = "./my-go-plugin",
    )
    assertEquals("my-go-plugin", adapter.id)
    assertEquals("mygoplugin", adapter.contextKey)
    assertEquals(Any::class.java, adapter.ctxClass)
  }

  @Test
  fun `ProcessPluginAdapter with mock subprocess lists tasks`() {
    // Create a mock plugin script that responds to JSON-RPC
    val script = createMockPluginScript(
      tasks = listOf(
        mapOf("id" to "hello", "description" to "Say hello"),
        mapOf("id" to "build", "description" to "Build it", "phase" to "BUILD"),
      )
    )

    val adapter = ProcessPluginAdapter(
      pluginId = "mock-plugin",
      command = script.absolutePath,
    )

    val tasks = adapter.listTasks()
    assertEquals(2, tasks.size)
    assertEquals("hello", tasks[0].id)
    assertEquals("Say hello", tasks[0].description)
    assertEquals("build", tasks[1].id)
    assertEquals("BUILD", tasks[1].phase)
  }

  @Test
  fun `ProcessPluginAdapter executes task successfully`() {
    val script = createMockPluginScript(
      tasks = listOf(mapOf("id" to "greet", "description" to "Greet")),
      executeResult = """{"type":"output","data":"Hello!"}
{"type":"completed","exitCode":0}"""
    )

    val adapter = ProcessPluginAdapter(
      pluginId = "mock-plugin",
      command = script.absolutePath,
    )

    val result = adapter.executeTask("greet", emptyList(), emptyMap())
    assertTrue(result.success)
  }

  @Test
  fun `ProcessPluginAdapter reports task failure`() {
    val script = createMockPluginScript(
      tasks = listOf(mapOf("id" to "fail-task", "description" to "Will fail")),
      executeResult = """{"type":"error","data":"Something went wrong"}
{"type":"completed","exitCode":1}"""
    )

    val adapter = ProcessPluginAdapter(
      pluginId = "mock-plugin",
      command = script.absolutePath,
    )

    val result = adapter.executeTask("fail-task", emptyList(), emptyMap())
    assertFalse(result.success)
  }

  /**
   * Creates a bash script that acts as a mock plugin process,
   * responding to JSON-RPC requests on stdin.
   */
  private fun createMockPluginScript(
    tasks: List<Map<String, Any>>,
    executeResult: String = """{"type":"completed","exitCode":0}""",
  ): File {
    val tasksJson = objectMapper.writeValueAsString(tasks)
    val script = tempDir.resolve("mock-plugin.sh").toFile()
    script.writeText("""#!/bin/bash
# Mock plugin process for testing APP v1 protocol
while IFS= read -r line; do
  method=$(echo "${'$'}line" | python3 -c "import sys,json; print(json.loads(sys.stdin.read())['method'])" 2>/dev/null)
  id=$(echo "${'$'}line" | python3 -c "import sys,json; print(json.loads(sys.stdin.read())['id'])" 2>/dev/null)

  case "${'$'}method" in
    init)
      echo '{"jsonrpc":"2.0","id":'${'$'}id',"result":{"ok":true}}'
      ;;
    listTasks)
      echo '{"jsonrpc":"2.0","id":'${'$'}id',"result":$tasksJson}'
      ;;
    executeTask)
      echo '{"jsonrpc":"2.0","id":'${'$'}id',"result":null}'
      cat <<'EOF'
$executeResult
EOF
      ;;
    shutdown)
      exit 0
      ;;
  esac
done
""")
    script.setExecutable(true)
    return script
  }
}
