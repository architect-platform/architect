package io.github.architectplatform.cli.embedded

import io.github.architectplatform.core.plugin.app.RemoteContentFetcher
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import jakarta.inject.Singleton

@Singleton
class JdkRemoteContentFetcher(
  private val httpClient: HttpClient = HttpClient.newHttpClient(),
) : RemoteContentFetcher {
  override fun fetchText(url: String, headers: Map<String, String>): String {
    val reqBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET()
    headers.forEach { (key, value) -> reqBuilder.header(key, value) }
    val response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() !in 200..299) {
      throw IllegalStateException("Failed fetching $url (status: ${response.statusCode()})")
    }
    return response.body()
  }

  override fun fetchBytes(url: String, headers: Map<String, String>): ByteArray {
    val reqBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET()
    headers.forEach { (key, value) -> reqBuilder.header(key, value) }
    val response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
    if (response.statusCode() !in 200..299) {
      throw IllegalStateException("Failed fetching $url (status: ${response.statusCode()})")
    }
    return response.body()
  }
}
