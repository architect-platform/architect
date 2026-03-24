package io.github.architectplatform.engine.core.plugin.infra

import io.github.architectplatform.engine.core.plugin.app.RemoteContentFetcher
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.client.HttpClient
import jakarta.inject.Singleton

@Singleton
class MicronautRemoteContentFetcher(
  private val httpClient: HttpClient,
) : RemoteContentFetcher {
  override fun fetchText(url: String, headers: Map<String, String>): String =
      exchange(url, headers, String::class.java)

  override fun fetchBytes(url: String, headers: Map<String, String>): ByteArray =
      exchange(url, headers, ByteArray::class.java)

  private fun <T : Any> exchange(
    url: String,
    headers: Map<String, String>,
    bodyType: Class<T>,
  ): T {
    var request: MutableHttpRequest<Any> = HttpRequest.GET<Any>(url)
    headers.forEach { (name, value) ->
      request = request.header(name, value)
    }
    val response = httpClient.toBlocking().exchange(request, bodyType)
    return response.body() ?: error("Failed to fetch remote content from $url")
  }
}