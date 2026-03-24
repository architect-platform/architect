package io.github.architectplatform.core.plugin.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * Smoke / integration tests for the ProcessPluginAdapter.
 *
 * These tests launch a real subprocess (a small shell script that implements
 * the APP v1 protocol) and verify that the adapter can discover tasks,
 * execute them, and read streaming events — exercising the "process" plugin
 * loading path end-to-end.
 */
class ProcessPluginAdapterSmokeTest {

  @TempDir
  lateinit var tempDir: Path

  /**
   * Creates a minimal APP v1 process plugin as a shell script.
   * The script reads JSON-RPC requests from stdin and writes responses to stdout.
   */
  private fun createProcessPluginScript(): Path {
    val script = tempDir.resolve("test-plugin.sh")
    Files.writeString(
      script,
      """
      #!/bin/sh
      # Minimal APP v1 process plugin for testing
      while IFS= read -r line; do
        method=$(echo "${'$'}line" | sed -n 's/.*"method"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
        id=$(echo "${'$'}line" | sed -n 's/.*"jsonrpc":"2.0","id":\([0-9]*\).*/\1/p')

        case "${'$'}method" in
          init)
            echo "{\"jsonrpc\":\"2.0\",\"id\":${'$'}id,\"result\":{\"ok\":true,\"name\":\"shell-test\",\"version\":\"0.1.0\"}}"
            ;;
          listTasks)
            echo "{\"jsonrpc\":\"2.0\",\"id\":${'$'}id,\"result\":[{\"id\":\"greet\",\"description\":\"Say hello\",\"phase\":\"run\",\"dependencies\":[],\"permissions\":[\"process:exec\"],\"requires_confirmation\":false}]}"
            ;;
          executeTask)
            echo "{\"jsonrpc\":\"2.0\",\"id\":${'$'}id,\"result\":null}"
            echo "{\"type\":\"output\",\"data\":\"Hello from process plugin!\"}"
            echo "{\"type\":\"progress\",\"progress\":1.0}"
            echo "{\"type\":\"completed\",\"exitCode\":0}"
            ;;
          shutdown)
            echo "{\"jsonrpc\":\"2.0\",\"id\":${'$'}id,\"result\":null}"
            exit 0
            ;;
          *)
            echo "{\"jsonrpc\":\"2.0\",\"id\":${'$'}id,\"error\":{\"code\":-32601,\"message\":\"Unknown method\"}}"
            ;;
        esac
      done
      """.trimIndent(),
    )
    script.toFile().setExecutable(true)
    return script
  }

  @Test
  fun `should list tasks from a process plugin subprocess`() {
    val script = createProcessPluginScript()
    val adapter = ProcessPluginAdapter(
      pluginId = "shell-test",
      command = script.toAbsolutePath().toString(),
    )

    val tasks = adapter.listTasks()

    assertEquals(1, tasks.size)
    assertEquals("greet", tasks[0].id)
    assertEquals("Say hello", tasks[0].description)
    assertEquals("run", tasks[0].phase)
    assertEquals(listOf("process:exec"), tasks[0].permissions)
  }

  @Test
  fun `should execute task and receive streaming events from process plugin`() {
    val script = createProcessPluginScript()
    val adapter = ProcessPluginAdapter(
      pluginId = "shell-test",
      command = script.toAbsolutePath().toString(),
    )

    val result = adapter.executeTask("greet", emptyList(), emptyMap())

    assertTrue(result.success, "Expected success but got: ${result.message}")
  }

  @Test
  fun `adapter metadata matches plugin id and context key`() {
    val adapter = ProcessPluginAdapter(
      pluginId = "my-process-plugin",
      command = "echo noop",
    )

    assertEquals("my-process-plugin", adapter.id)
    assertEquals("myprocessplugin", adapter.contextKey)
    assertEquals(Any::class.java, adapter.ctxClass)
  }
}
