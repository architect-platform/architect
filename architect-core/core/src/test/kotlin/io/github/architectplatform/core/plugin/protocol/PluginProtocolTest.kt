package io.github.architectplatform.core.plugin.protocol

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PluginProtocolTest {

  private val objectMapper = ObjectMapper().registerKotlinModule()

  @Test
  fun `protocol constants define APP v1 contract`() {
    assertEquals("2.0", PluginProtocol.JSON_RPC_VERSION)
    assertEquals("1.0.0", PluginProtocol.VERSION)
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
  fun `init params serialize protocol version field`() {
    val json = objectMapper.writeValueAsString(InitParams(config = mapOf("enabled" to true)))

    assertTrue(json.contains("\"protocol_version\":\"1.0.0\""))
    assertTrue(json.contains("\"enabled\":true"))
  }

  @Test
  fun `task descriptor uses requires confirmation wire name`() {
    val descriptor = objectMapper.readValue(
      """{"id":"deploy","requires_confirmation":true,"permissions":["process:exec"]}""",
      TaskDescriptor::class.java,
    )

    assertEquals("deploy", descriptor.id)
    assertTrue(descriptor.requiresConfirmation)
    assertEquals(listOf("process:exec"), descriptor.permissions)
  }

  @Test
  fun `task event supports progress and completion payloads`() {
    val progressEvent = objectMapper.readValue(
      """{"type":"progress","progress":0.5}""",
      TaskEvent::class.java,
    )
    val completedEvent = objectMapper.readValue(
      """{"type":"completed","exitCode":0}""",
      TaskEvent::class.java,
    )

    assertEquals(PluginProtocol.EVENT_PROGRESS, progressEvent.type)
    assertEquals(0.5, progressEvent.progress)
    assertEquals(PluginProtocol.EVENT_COMPLETED, completedEvent.type)
    assertEquals(0, completedEvent.exitCode)
    assertFalse(completedEvent.exitCode != 0)
  }
}