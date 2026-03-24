package io.github.architectplatform.core.plugin.protocol

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Compatibility tests that validate the Kotlin engine data classes
 * round-trip against the shared protocol fixtures in docs/protocol-fixtures.json.
 *
 * These fixtures are consumed identically by the TypeScript, Python, and Go SDKs,
 * ensuring all four implementations agree on wire format.
 */
class ProtocolCompatibilityTest {

  private val objectMapper = ObjectMapper().registerKotlinModule()
  private val fixtures: Map<String, Any> by lazy {
    val fixturesFile = locateFixturesFile()
    objectMapper.readValue<Map<String, Any>>(fixturesFile)
  }

  private fun locateFixturesFile(): File {
    // Walk up from the test working directory to find the repo root
    var dir = File(System.getProperty("user.dir"))
    while (dir.parentFile != null) {
      val candidate = File(dir, "docs/protocol-fixtures.json")
      if (candidate.exists()) return candidate
      dir = dir.parentFile
    }
    // Fallback: try relative from working dir (e.g. when run from architect-core/core)
    val fallback = File("../../docs/protocol-fixtures.json")
    if (fallback.exists()) return fallback
    throw IllegalStateException("Cannot locate docs/protocol-fixtures.json from ${System.getProperty("user.dir")}")
  }

  @Test
  fun `protocol constants match shared fixtures`() {
    assertEquals(fixtures["json_rpc_version"], PluginProtocol.JSON_RPC_VERSION)
    assertEquals(fixtures["protocol_version"], PluginProtocol.VERSION)
    assertEquals("init", PluginProtocol.METHOD_INIT)
    assertEquals("listTasks", PluginProtocol.METHOD_LIST_TASKS)
    assertEquals("executeTask", PluginProtocol.METHOD_EXECUTE_TASK)
    assertEquals("shutdown", PluginProtocol.METHOD_SHUTDOWN)
    assertEquals("output", PluginProtocol.EVENT_OUTPUT)
    assertEquals("progress", PluginProtocol.EVENT_PROGRESS)
    assertEquals("error", PluginProtocol.EVENT_ERROR)
    assertEquals("completed", PluginProtocol.EVENT_COMPLETED)
  }

  @Test
  fun `init request fixture deserializes into InitParams correctly`() {
    val requests = fixtures["requests"] as Map<*, *>
    val initRequest = requests["init"] as Map<*, *>
    val params = initRequest["params"] as Map<*, *>
    val json = objectMapper.writeValueAsString(params)
    val parsed = objectMapper.readValue<InitParams>(json)

    assertEquals("1.0.0", parsed.protocolVersion)
    assertNotNull(parsed.config)
  }

  @Test
  fun `init request fixture round-trips through JsonRpcRequest`() {
    val requests = fixtures["requests"] as Map<*, *>
    val initRequest = requests["init"] as Map<*, *>
    val json = objectMapper.writeValueAsString(initRequest)
    val parsed = objectMapper.readValue<JsonRpcRequest>(json)

    assertEquals("2.0", parsed.jsonrpc)
    assertEquals(1, parsed.id)
    assertEquals("init", parsed.method)
    assertNotNull(parsed.params)
  }

  @Test
  fun `executeTask request fixture deserializes into ExecuteTaskParams`() {
    val requests = fixtures["requests"] as Map<*, *>
    val execRequest = requests["executeTask"] as Map<*, *>
    val params = execRequest["params"] as Map<*, *>
    val json = objectMapper.writeValueAsString(params)
    val parsed = objectMapper.readValue<ExecuteTaskParams>(json)

    assertEquals("build", parsed.id)
    assertTrue(parsed.args.isNotEmpty())
    assertTrue(parsed.env.isNotEmpty())
  }

  @Test
  fun `task descriptor fixtures deserialize with all fields`() {
    val taskDescriptors = fixtures["task_descriptors"] as Map<*, *>

    // Minimal descriptor
    val minimalJson = objectMapper.writeValueAsString(taskDescriptors["minimal"])
    val minimal = objectMapper.readValue<TaskDescriptor>(minimalJson)
    assertEquals("hello", minimal.id)
    assertEquals("A minimal task", minimal.description)

    // Full descriptor
    val fullJson = objectMapper.writeValueAsString(taskDescriptors["full"])
    val full = objectMapper.readValue<TaskDescriptor>(fullJson)
    assertEquals("deploy", full.id)
    assertEquals("release", full.phase)
    assertTrue(full.dependencies.isNotEmpty())
    assertTrue(full.permissions.isNotEmpty())
    assertTrue(full.requiresConfirmation)
  }

  @Test
  fun `task event fixtures deserialize correctly`() {
    val events = fixtures["events"] as Map<*, *>

    // Output event
    val outputJson = objectMapper.writeValueAsString(events["output"])
    val output = objectMapper.readValue<TaskEvent>(outputJson)
    assertEquals("output", output.type)
    assertEquals("Compiling sources...", output.data)

    // Progress event
    val progressJson = objectMapper.writeValueAsString(events["progress"])
    val progress = objectMapper.readValue<TaskEvent>(progressJson)
    assertEquals("progress", progress.type)
    assertEquals(0.5, progress.progress)

    // Completed success
    val completedJson = objectMapper.writeValueAsString(events["completed_success"])
    val completed = objectMapper.readValue<TaskEvent>(completedJson)
    assertEquals("completed", completed.type)
    assertEquals(0, completed.exitCode)

    // Completed failure
    val failedJson = objectMapper.writeValueAsString(events["completed_failure"])
    val failed = objectMapper.readValue<TaskEvent>(failedJson)
    assertEquals("completed", failed.type)
    assertEquals(1, failed.exitCode)
  }

  @Test
  fun `error response fixture contains expected error shape`() {
    val responses = fixtures["responses"] as Map<*, *>
    val errorResponse = responses["protocol_version_error"] as Map<*, *>
    val json = objectMapper.writeValueAsString(errorResponse)
    val parsed = objectMapper.readValue<JsonRpcResponse>(json)

    assertNotNull(parsed.error)
    assertEquals(-32600, (parsed.error as JsonRpcError).code)
  }

  @Test
  fun `InitParams serializes to wire format matching fixture shape`() {
    val params = InitParams(config = mapOf("project_name" to "test"))
    val json = objectMapper.writeValueAsString(params)
    val roundTrip = objectMapper.readValue<Map<String, Any>>(json)

    // Verify snake_case wire names
    assertTrue(roundTrip.containsKey("protocol_version"))
    assertEquals("1.0.0", roundTrip["protocol_version"])
    assertTrue(roundTrip.containsKey("config"))
  }

  @Test
  fun `TaskDescriptor serializes requires_confirmation in snake_case`() {
    val descriptor = TaskDescriptor(
      id = "deploy",
      description = "Deploy to production",
      phase = "release",
      dependencies = listOf("build"),
      permissions = listOf("process:exec"),
      requiresConfirmation = true,
    )
    val json = objectMapper.writeValueAsString(descriptor)
    val roundTrip = objectMapper.readValue<Map<String, Any>>(json)

    assertTrue(roundTrip.containsKey("requires_confirmation"))
    assertEquals(true, roundTrip["requires_confirmation"])
  }
}
