package io.github.architectplatform.cli.command

import io.github.architectplatform.cli.embedded.JdkRemoteContentFetcher
import io.github.architectplatform.cli.plugin.TestPluginContext
import io.github.architectplatform.cli.plugin.TestPluginWithSchema
import io.github.architectplatform.cli.plugin.TestPluginTask
import io.github.architectplatform.cli.plugin.PluginJarValidator
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import kotlin.io.path.outputStream

class PluginCommandHandlerTest {

  @Test
  fun `outdated prints available updates`(@TempDir tmpDir: Path) {
    val config = File(tmpDir.toFile(), "architect.yml")
    config.writeText(
      """
      project:
        name: sample
      plugins:
        - name: docs-architected
          version: 1.0.0
        - name: git-architected
          version: latest
      """.trimIndent(),
    )

    val handler = PluginCommandHandler(
      remoteContentFetcher = JdkRemoteContentFetcher(FakeHttpClient(registryJson())),
      registryUrl = "https://registry.architect.dev/registry.json",
    )

    val output = withUserDir(tmpDir) { handler.handle(listOf("plugin", "outdated")) }

    assertTrue(output.contains("Plugin Updates Available"))
    assertTrue(output.contains("docs-architected"))
    assertTrue(output.contains("2.0.0"))
  }

  @Test
  fun `update all rewrites plugin versions to latest`(@TempDir tmpDir: Path) {
    val config = File(tmpDir.toFile(), "architect.yml")
    config.writeText(
      """
      project:
        name: sample
      plugins:
        - name: docs-architected
          version: 1.0.0
      """.trimIndent(),
    )

    val handler = PluginCommandHandler(
      remoteContentFetcher = JdkRemoteContentFetcher(FakeHttpClient(registryJson())),
      registryUrl = "https://registry.architect.dev/registry.json",
    )

    val output = withUserDir(tmpDir) { handler.handle(listOf("plugin", "update", "--all")) }

    assertTrue(output.contains("Updated 1 plugin"))
    assertTrue(config.readText().contains("version: 2.0.0"))
  }

  @Test
  fun `update single plugin only updates selected id`(@TempDir tmpDir: Path) {
    val config = File(tmpDir.toFile(), "architect.yml")
    config.writeText(
      """
      project:
        name: sample
      plugins:
        - name: docs-architected
          version: 1.0.0
        - name: git-architected
          version: 1.0.0
      """.trimIndent(),
    )

    val handler = PluginCommandHandler(
      remoteContentFetcher = JdkRemoteContentFetcher(FakeHttpClient(registryJson())),
      registryUrl = "https://registry.architect.dev/registry.json",
    )

    withUserDir(tmpDir) { handler.handle(listOf("plugin", "update", "docs-architected")) }
    val content = config.readText()
    assertTrue(content.contains("name: docs-architected"))
    assertTrue(content.contains("version: 2.0.0"))
    assertTrue(content.contains("name: git-architected"))
  }

  @Test
  fun `plugin test validates contract schema and task registration`(@TempDir tmpDir: Path) {
    val jarPath = createPluginJar(tmpDir.resolve("plugin-test.jar"))
    val handler = PluginCommandHandler()

    val output = withUserDir(tmpDir) {
      handler.handle(listOf("plugin", "test", jarPath.toString()))
    }

    assertTrue(output.contains("Plugin test passed"))
    assertTrue(output.contains("contract"))
    assertTrue(output.contains("config-schema"))
    assertTrue(output.contains("task-registration"))
  }

  private fun registryJson(): String =
    """
    {
      "plugins": [
        { "id": "docs-architected", "version": "2.0.0", "asset": "docs-architected.jar" },
        { "id": "git-architected", "version": "1.0.0", "asset": "git-architected.jar" }
      ]
    }
    """.trimIndent()

  private fun withUserDir(dir: Path, block: () -> Unit): String {
    val original = System.getProperty("user.dir")
    System.setProperty("user.dir", dir.toString())
    return try {
      captureStdout(block)
    } finally {
      System.setProperty("user.dir", original)
    }
  }

  private fun createPluginJar(jarPath: Path): Path {
    JarOutputStream(jarPath.outputStream().buffered()).use { output ->
      writeClass(output, TestPluginWithSchema::class.java)
      writeClass(output, TestPluginContext::class.java)
      writeClass(output, TestPluginTask::class.java)
      writeTextEntry(output, PluginJarValidator.SPI_RESOURCE, TestPluginWithSchema::class.java.name + "\n")
    }
    return jarPath
  }

  private fun writeClass(output: JarOutputStream, type: Class<*>) {
    val resourcePath = type.name.replace('.', '/') + ".class"
    val bytes = type.classLoader.getResourceAsStream(resourcePath)?.use { it.readBytes() }
      ?: error("Missing compiled class resource $resourcePath")
    writeBytesEntry(output, resourcePath, bytes)
  }

  private fun writeTextEntry(output: JarOutputStream, entryName: String, content: String) {
    writeBytesEntry(output, entryName, content.toByteArray())
  }

  private fun writeBytesEntry(output: JarOutputStream, entryName: String, content: ByteArray) {
    output.putNextEntry(JarEntry(entryName))
    output.write(content)
    output.closeEntry()
  }

  private fun captureStdout(block: () -> Unit): String {
    val original = System.out
    val baos = java.io.ByteArrayOutputStream()
    val ps = java.io.PrintStream(baos, true)
    System.setOut(ps)
    try {
      block()
    } finally {
      System.out.flush()
      System.setOut(original)
    }
    return baos.toString()
  }

  private class FakeHttpClient(private val responseBody: String) : HttpClient() {
    override fun <T : Any?> send(
      request: HttpRequest?,
      responseBodyHandler: HttpResponse.BodyHandler<T>?,
    ): HttpResponse<T> {
      @Suppress("UNCHECKED_CAST")
      return FakeHttpResponse(responseBody as T)
    }

    override fun <T : Any?> sendAsync(
      request: HttpRequest?,
      responseBodyHandler: HttpResponse.BodyHandler<T>?,
    ): java.util.concurrent.CompletableFuture<HttpResponse<T>> =
      java.util.concurrent.CompletableFuture.completedFuture(send(request, responseBodyHandler))

    override fun <T : Any?> sendAsync(
      request: HttpRequest?,
      responseBodyHandler: HttpResponse.BodyHandler<T>?,
      pushPromiseHandler: HttpResponse.PushPromiseHandler<T>?,
    ): java.util.concurrent.CompletableFuture<HttpResponse<T>> =
      java.util.concurrent.CompletableFuture.completedFuture(send(request, responseBodyHandler))

    override fun cookieHandler(): java.util.Optional<java.net.CookieHandler> = java.util.Optional.empty()
    override fun connectTimeout(): java.util.Optional<java.time.Duration> = java.util.Optional.empty()
    override fun followRedirects(): HttpClient.Redirect = HttpClient.Redirect.NEVER
    override fun proxy(): java.util.Optional<java.net.ProxySelector> = java.util.Optional.empty()
    override fun sslContext(): javax.net.ssl.SSLContext? = null
    override fun sslParameters(): javax.net.ssl.SSLParameters = javax.net.ssl.SSLParameters()
    override fun authenticator(): java.util.Optional<java.net.Authenticator> = java.util.Optional.empty()
    override fun version(): HttpClient.Version = HttpClient.Version.HTTP_1_1
    override fun executor(): java.util.Optional<java.util.concurrent.Executor> = java.util.Optional.empty()
  }

  private class FakeHttpResponse<T>(private val bodyValue: T) : HttpResponse<T> {
    override fun statusCode(): Int = 200
    override fun request(): HttpRequest = HttpRequest.newBuilder(URI.create("https://registry.architect.dev/")).build()
    override fun previousResponse(): java.util.Optional<HttpResponse<T>> = java.util.Optional.empty()
    override fun headers(): HttpHeaders = HttpHeaders.of(mapOf(), { _, _ -> true })
    override fun body(): T = bodyValue
    override fun sslSession(): java.util.Optional<javax.net.ssl.SSLSession> = java.util.Optional.empty()
    override fun uri(): URI = URI.create("https://registry.architect.dev/")
    override fun version(): HttpClient.Version = HttpClient.Version.HTTP_1_1
  }
}
