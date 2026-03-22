package io.github.architectplatform.engine.core.tasks.infrastructure

import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.engine.core.plugin.app.RemoteContentFetcher
import io.github.architectplatform.engine.core.tasks.application.RemoteOutputCache
import org.slf4j.LoggerFactory

/**
 * HTTP-based remote output cache implementation.
 *
 * Communicates with a simple REST API:
 * - `GET  {baseUrl}/{key}` — returns `success\nmessage\n---\nstdout` or 404
 * - `PUT  {baseUrl}/{key}` — body is `success\nmessage\n---\nstdout`
 *
 * Can be self-hosted or backed by architect-cloud.
 */
class HttpRemoteOutputCache(
  private val baseUrl: String,
  private val fetcher: RemoteContentFetcher,
) : RemoteOutputCache {

  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun fetchResult(key: String): RemoteOutputCache.CachedTaskResult? {
    return try {
      val url = "${baseUrl.trimEnd('/')}/$key"
      val body = fetcher.fetchText(url)
      if (body.isBlank()) return null
      parseBody(body)
    } catch (e: Exception) {
      logger.debug("Remote cache miss or error for key {}: {}", key, e.message)
      null
    }
  }

  override fun storeResult(key: String, result: TaskResult, stdout: String?) {
    try {
      val url = "${baseUrl.trimEnd('/')}/$key"
      val body = buildBody(result, stdout)
      fetcher.fetchText(url, headers = mapOf(
        "X-Cache-Method" to "PUT",
        "Content-Type" to "text/plain",
        "X-Cache-Body" to body,
      ))
    } catch (e: Exception) {
      logger.debug("Failed to store result in remote cache for key {}: {}", key, e.message)
    }
  }

  private fun parseBody(body: String): RemoteOutputCache.CachedTaskResult? {
    val parts = body.split("\n---\n", limit = 2)
    val header = parts[0].lines()
    val success = header.getOrNull(0)?.toBooleanStrictOrNull() ?: return null
    val message = header.drop(1).joinToString("\n").ifBlank { null }
    val stdout = parts.getOrNull(1)
    return RemoteOutputCache.CachedTaskResult(success = success, message = message, stdout = stdout)
  }

  private fun buildBody(result: TaskResult, stdout: String?): String {
    val header = "${result.success}\n${result.message.orEmpty()}"
    return if (stdout != null) "$header\n---\n$stdout" else header
  }
}
