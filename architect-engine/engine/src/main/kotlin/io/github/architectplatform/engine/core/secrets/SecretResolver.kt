package io.github.architectplatform.engine.core.secrets

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.inject.Singleton
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

interface SecretResolver {
  fun resolve(name: String, projectDir: Path? = null): String?
}

@Singleton
class CompositeSecretResolver(
  private val resolvers: List<SecretResolver> = defaultResolvers(),
) : SecretResolver {
  override fun resolve(name: String, projectDir: Path?): String? =
    resolvers.firstNotNullOfOrNull { it.resolve(name, projectDir) }

  companion object {
    fun defaultResolvers(
      env: Map<String, String> = System.getenv(),
      commandRunner: SecretCommandRunner = ProcessSecretCommandRunner(),
      vaultClient: VaultSecretClient = HttpVaultSecretClient(),
      dotEnvLoader: DotEnvLoader = DotEnvLoader(),
    ): List<SecretResolver> =
      listOf(
        EnvironmentVariableSecretResolver(env),
        DotEnvSecretResolver(dotEnvLoader),
        VaultSecretResolver(env, vaultClient),
        AwsSecretsManagerSecretResolver(env, commandRunner),
        GcpSecretManagerSecretResolver(env, commandRunner),
      )
  }
}

class EnvironmentVariableSecretResolver(
  private val env: Map<String, String> = System.getenv(),
) : SecretResolver {
  override fun resolve(name: String, projectDir: Path?): String? = env[name]
}

class DotEnvSecretResolver(
  private val loader: DotEnvLoader = DotEnvLoader(),
) : SecretResolver {
  override fun resolve(name: String, projectDir: Path?): String? =
    projectDir?.let(loader::load)?.get(name)
}

class DotEnvLoader {
  private val cache = ConcurrentHashMap<Path, Map<String, String>>()

  fun load(projectDir: Path): Map<String, String> =
    cache.computeIfAbsent(projectDir.toAbsolutePath().normalize()) { dir ->
      val envFile = dir.resolve(".env")
      if (!Files.exists(envFile)) {
        emptyMap()
      } else {
        Files.readAllLines(envFile)
          .mapNotNull(::parseLine)
          .toMap(linkedMapOf())
      }
    }

  private fun parseLine(line: String): Pair<String, String>? {
    val trimmed = line.trim()
    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
      return null
    }

    val normalized = if (trimmed.startsWith("export ")) trimmed.removePrefix("export ") else trimmed
    val separator = normalized.indexOf('=')
    if (separator <= 0) {
      return null
    }

    val key = normalized.substring(0, separator).trim()
    val rawValue = normalized.substring(separator + 1).trim()
    return key to rawValue.removeSurrounding("\"").removeSurrounding("'")
  }
}

class VaultSecretResolver(
  private val env: Map<String, String> = System.getenv(),
  private val client: VaultSecretClient = HttpVaultSecretClient(),
) : SecretResolver {
  override fun resolve(name: String, projectDir: Path?): String? {
    val address = env["VAULT_ADDR"] ?: return null
    val token = env["VAULT_TOKEN"] ?: return null
    val prefix = env["ARCHITECT_VAULT_PATH_PREFIX"]?.trim('/') ?: "secret/data"
    return client.read(address, token, "$prefix/$name")
  }
}

interface VaultSecretClient {
  fun read(address: String, token: String, path: String): String?
}

class HttpVaultSecretClient(
  private val httpClient: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
  private val objectMapper: ObjectMapper = jacksonObjectMapper(),
) : VaultSecretClient {
  override fun read(address: String, token: String, path: String): String? {
    val request = HttpRequest.newBuilder()
      .uri(URI.create(address.trimEnd('/') + "/v1/" + path.trimStart('/')))
      .timeout(Duration.ofSeconds(10))
      .header("X-Vault-Token", token)
      .GET()
      .build()
    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() !in 200..299) {
      return null
    }

    val body = objectMapper.readTree(response.body())
    return body.path("data").path("data").path("value").takeIf { !it.isMissingNode && !it.isNull }?.asText()
      ?: body.path("data").path("data").fields().asSequence().firstOrNull()?.value?.asText()
  }
}

class AwsSecretsManagerSecretResolver(
  private val env: Map<String, String> = System.getenv(),
  private val commandRunner: SecretCommandRunner = ProcessSecretCommandRunner(),
) : SecretResolver {
  override fun resolve(name: String, projectDir: Path?): String? {
    if (env["AWS_REGION"] == null && env["AWS_DEFAULT_REGION"] == null && env["AWS_PROFILE"] == null) {
      return null
    }

    val result = commandRunner.run(
      listOf(
        "aws",
        "secretsmanager",
        "get-secret-value",
        "--secret-id",
        name,
        "--query",
        "SecretString",
        "--output",
        "text",
      ),
    )
    return result.output.takeIf { result.exitCode == 0 && it.isNotBlank() && it != "None" }
  }
}

class GcpSecretManagerSecretResolver(
  private val env: Map<String, String> = System.getenv(),
  private val commandRunner: SecretCommandRunner = ProcessSecretCommandRunner(),
) : SecretResolver {
  override fun resolve(name: String, projectDir: Path?): String? {
    if (env["GOOGLE_CLOUD_PROJECT"] == null && env["GCP_PROJECT"] == null && env["GOOGLE_APPLICATION_CREDENTIALS"] == null) {
      return null
    }

    val result = commandRunner.run(
      listOf(
        "gcloud",
        "secrets",
        "versions",
        "access",
        "latest",
        "--secret=$name",
      ),
    )
    return result.output.takeIf { result.exitCode == 0 && it.isNotBlank() }
  }
}

data class SecretCommandResult(
  val exitCode: Int,
  val output: String,
)

fun interface SecretCommandRunner {
  fun run(command: List<String>): SecretCommandResult
}

class ProcessSecretCommandRunner : SecretCommandRunner {
  override fun run(command: List<String>): SecretCommandResult {
    val process = ProcessBuilder(command)
      .redirectErrorStream(true)
      .start()
    if (!process.waitFor(10, TimeUnit.SECONDS)) {
      process.destroyForcibly()
      return SecretCommandResult(exitCode = 124, output = "")
    }
    return SecretCommandResult(process.exitValue(), process.inputStream.bufferedReader().readText().trim())
  }
}