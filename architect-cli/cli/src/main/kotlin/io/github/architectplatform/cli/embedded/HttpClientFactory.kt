package io.github.architectplatform.cli.embedded

import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import java.net.http.HttpClient

@Factory
class HttpClientFactory {

  @Singleton
  fun httpClient(): HttpClient = HttpClient.newHttpClient()
}
