package io.github.architectplatform.api.core.project

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for Config utility functions.
 */
class ConfigTest {
  @Test
  fun `getKey retrieves top-level string value`() {
    val config: Config = mapOf("name" to "TestProject")

    val result = config.getKey<String>("name")

    assertEquals("TestProject", result)
  }

  @Test
  fun `getKey retrieves nested value using dot notation`() {
    val config: Config =
      mapOf(
        "database" to
          mapOf(
            "host" to "localhost",
            "port" to 5432,
          ),
      )

    val host = config.getKey<String>("database.host")
    val port = config.getKey<Int>("database.port")

    assertEquals("localhost", host)
    assertEquals(5432, port)
  }

  @Test
  fun `getKey retrieves deeply nested values`() {
    val config: Config =
      mapOf(
        "server" to
          mapOf(
            "connection" to
              mapOf(
                "pool" to
                  mapOf(
                    "size" to 10,
                  ),
              ),
          ),
      )

    val poolSize = config.getKey<Int>("server.connection.pool.size")

    assertEquals(10, poolSize)
  }

  @Test
  fun `getKey retrieves list element by index`() {
    val config: Config =
      mapOf(
        "servers" to listOf("server1", "server2", "server3"),
      )

    val firstServer = config.getKey<String>("servers.0")
    val secondServer = config.getKey<String>("servers.1")

    assertEquals("server1", firstServer)
    assertEquals("server2", secondServer)
  }

  @Test
  fun `getKey retrieves nested value in list`() {
    val config: Config =
      mapOf(
        "users" to
          listOf(
            mapOf("name" to "Alice", "age" to 30),
            mapOf("name" to "Bob", "age" to 25),
          ),
      )

    val firstUserName = config.getKey<String>("users.0.name")
    val secondUserAge = config.getKey<Int>("users.1.age")

    assertEquals("Alice", firstUserName)
    assertEquals(25, secondUserAge)
  }

  @Test
  fun `getKey returns null for non-existent key`() {
    val config: Config = mapOf("name" to "TestProject")

    val result = config.getKey<String>("nonExistent")

    assertNull(result)
  }

  @Test
  fun `getKey returns null for non-existent nested key`() {
    val config: Config =
      mapOf(
        "database" to mapOf("host" to "localhost"),
      )

    val result = config.getKey<String>("database.port")

    assertNull(result)
  }

  @Test
  fun `getKey throws IllegalArgumentException for invalid list index`() {
    val config: Config =
      mapOf(
        "servers" to listOf("server1", "server2"),
      )

    assertThrows(IllegalArgumentException::class.java) {
      config.getKey<String>("servers.abc")
    }
  }

  @Test
  fun `getKey throws IndexOutOfBoundsException for out of range list index`() {
    val config: Config =
      mapOf(
        "servers" to listOf("server1", "server2"),
      )

    assertThrows(IndexOutOfBoundsException::class.java) {
      config.getKey<String>("servers.5")
    }
  }

  @Test
  fun `getKey throws IndexOutOfBoundsException for negative list index`() {
    val config: Config =
      mapOf(
        "servers" to listOf("server1", "server2"),
      )

    assertThrows(IndexOutOfBoundsException::class.java) {
      config.getKey<String>("servers.-1")
    }
  }

  @Test
  fun `getKey throws IllegalStateException for invalid path through primitive value`() {
    val config: Config =
      mapOf(
        "name" to "TestProject",
      )

    assertThrows(IllegalStateException::class.java) {
      config.getKey<String>("name.nested")
    }
  }

  @Test
  fun `getKey handles empty map`() {
    val config: Config = emptyMap()

    val result = config.getKey<String>("anyKey")

    assertNull(result)
  }

  @Test
  fun `getKey handles mixed types correctly`() {
    val config: Config =
      mapOf(
        "string" to "text",
        "number" to 42,
        "boolean" to true,
        "list" to listOf(1, 2, 3),
        "map" to mapOf("nested" to "value"),
      )

    assertEquals("text", config.getKey<String>("string"))
    assertEquals(42, config.getKey<Int>("number"))
    assertEquals(true, config.getKey<Boolean>("boolean"))
    assertEquals(listOf(1, 2, 3), config.getKey<List<Int>>("list"))
    assertEquals("value", config.getKey<String>("map.nested"))
  }
}
