package io.github.architectplatform.cli.command

import io.github.architectplatform.core.secrets.SecretStore
import io.github.architectplatform.core.secrets.SecretStoreBackend
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SecretCommandHandlerTest {
  @Test
  fun `set list and delete work end to end`() {
    val backend = InMemorySecretStoreBackend()
    val handler = SecretCommandHandler(store = SecretStore(backends = listOf(backend)))

    val setOutput = captureStdout {
      handler.handle(listOf("secret", "set", "API_TOKEN", "super-secret"))
    }
    val listOutput = captureStdout {
      handler.handle(listOf("secret", "list"))
    }
    val deleteOutput = captureStdout {
      handler.handle(listOf("secret", "delete", "API_TOKEN"))
    }

    assertTrue(setOutput.contains("stored successfully"))
    assertTrue(listOutput.contains("API_TOKEN"))
    assertTrue(deleteOutput.contains("deleted"))
    assertFalse(backend.values.containsKey("API_TOKEN"))
  }

  @Test
  fun `get prints stored value`() {
    val backend = InMemorySecretStoreBackend(mutableMapOf("API_TOKEN" to "resolved-value"))
    val handler = SecretCommandHandler(store = SecretStore(backends = listOf(backend)))

    val output = captureStdout {
      handler.handle(listOf("secret", "get", "API_TOKEN"))
    }

    assertEquals("resolved-value", output.trim())
  }

  private fun captureStdout(block: () -> Unit): String {
    val original = System.out
    val output = ByteArrayOutputStream()
    System.setOut(PrintStream(output))
    try {
      block()
    } finally {
      System.setOut(original)
    }
    return output.toString()
  }

  private class InMemorySecretStoreBackend(
    initialValues: MutableMap<String, String> = linkedMapOf(),
  ) : SecretStoreBackend {
    val values = initialValues

    override fun isAvailable(): Boolean = true

    override fun set(name: String, value: String) {
      values[name] = value
    }

    override fun get(name: String): String? = values[name]

    override fun delete(name: String): Boolean = values.remove(name) != null

    override fun listKeys(): List<String> = values.keys.toList()
  }
}
