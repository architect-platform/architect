package io.github.architectplatform.cli.client

import com.sun.net.httpserver.HttpServer
import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class EngineCommandClientConnectionPoolingTest {

  @Test
  fun `should reuse the same tcp connection for sequential engine requests`() {
    val remotePorts = CopyOnWriteArrayList<Int>()
    val requestsServed = CountDownLatch(2)
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val executor = Executors.newSingleThreadExecutor()

    server.executor = executor
    server.createContext("/api/projects") { exchange ->
      remotePorts += exchange.remoteAddress.port
      val body = "[]".toByteArray()
      exchange.responseHeaders.add("Content-Type", "application/json")
      exchange.responseHeaders.add("Connection", "keep-alive")
      exchange.sendResponseHeaders(200, body.size.toLong())
      exchange.responseBody.use { it.write(body) }
      requestsServed.countDown()
    }
    server.start()

    ApplicationContext.run(
      mapOf(
        "micronaut.http.services.engine.url" to "http://127.0.0.1:${server.address.port}",
        "micronaut.http.services.engine.read-timeout" to "5s",
        "micronaut.http.services.engine.connect-timeout" to "5s",
        "micronaut.http.services.engine.pool.enabled" to true,
        "micronaut.http.services.engine.pool.max-connections" to 1,
        "micronaut.http.services.engine.pool.max-concurrent-http1-connections" to 1,
      )
    ).use { context ->
      val client = context.getBean(EngineCommandClient::class.java)

      assertEquals(emptyList<Any>(), client.getAllProjects())
      assertEquals(emptyList<Any>(), client.getAllProjects())
      assertTrue(requestsServed.await(5, TimeUnit.SECONDS), "Expected both requests to reach the server")
      assertEquals(1, remotePorts.distinct().size)
    }

    server.stop(0)
    executor.shutdownNow()
  }
}