package io.github.architectplatform.cli.engine

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.ServerSocket

/**
 * Unit tests for [EngineHealthChecker].
 *
 * Covers: HTTP 200 success path, HTTP 500 server error, connection refused, and read timeout.
 */
class EngineHealthCheckerTest {

  @Test
  fun `should return true when engine responds with HTTP 200`() {
    // Arrange: minimal HTTP server on a random port that responds 200
    val server = ServerSocket(0)
    val port = server.localPort
    val checker = EngineHealthChecker()
    checker.engineUrl = "http://localhost:$port"

    Thread {
      val conn = server.accept()
      val response = "HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
      conn.getOutputStream().write(response.toByteArray())
      conn.close()
      server.close()
    }.apply { isDaemon = true }.start()

    // Act & Assert
    assertTrue(checker.isRunning())
  }

  @Test
  fun `should return false when engine responds with HTTP 500`() {
    val server = ServerSocket(0)
    val port = server.localPort
    val checker = EngineHealthChecker()
    checker.engineUrl = "http://localhost:$port"

    Thread {
      val conn = server.accept()
      val response = "HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
      conn.getOutputStream().write(response.toByteArray())
      conn.close()
      server.close()
    }.apply { isDaemon = true }.start()

    assertFalse(checker.isRunning())
  }

  @Test
  fun `should return false when connection is refused`() {
    // Arrange: get a free port, then immediately close the socket so the port is unused
    val port = ServerSocket(0).use { it.localPort }
    val checker = EngineHealthChecker()
    checker.engineUrl = "http://localhost:$port"

    // Act & Assert
    assertFalse(checker.isRunning())
  }

  @Test
  fun `should return false when server does not respond within timeout`() {
    // Arrange: server that accepts TCP but never writes back → triggers read timeout
    val server = ServerSocket(0)
    val port = server.localPort
    val checker = EngineHealthChecker()
    checker.engineUrl = "http://localhost:$port"

    Thread {
      server.accept() // accept without writing — causes read timeout on client
    }.apply { isDaemon = true }.start()

    // Act
    val start = System.currentTimeMillis()
    val result = checker.isRunning()
    val elapsed = System.currentTimeMillis() - start

    // Assert
    assertFalse(result)
    assertTrue(elapsed >= 1_500, "Expected read timeout (~2s), got ${elapsed}ms")

    server.close()
  }
}
