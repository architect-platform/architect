package io.github.architectplatform.engine.core.secrets

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SecretResolverTest {
  @Test
  fun `environment variables take precedence over dotenv`(@TempDir tempDir: Path) {
    tempDir.resolve(".env").toFile().writeText("API_TOKEN=from-dotenv\n")
    val resolver = CompositeSecretResolver(CompositeSecretResolver.defaultResolvers(env = mapOf("API_TOKEN" to "from-env")))

    assertEquals("from-env", resolver.resolve("API_TOKEN", tempDir))
  }

  @Test
  fun `dotenv fallback resolves project secret`(@TempDir tempDir: Path) {
    tempDir.resolve(".env").toFile().writeText("API_TOKEN=from-dotenv\n")
    val resolver = DotEnvSecretResolver(DotEnvLoader())

    assertEquals("from-dotenv", resolver.resolve("API_TOKEN", tempDir))
    assertNull(resolver.resolve("MISSING", tempDir))
  }

  @Test
  fun `vault resolver returns first secret value from response`() {
    val resolver = VaultSecretResolver(
      env = mapOf(
        "VAULT_ADDR" to "https://vault.example.com",
        "VAULT_TOKEN" to "token",
      ),
      client = object : VaultSecretClient {
        override fun read(address: String, token: String, path: String): String? {
          assertEquals("https://vault.example.com", address)
          assertEquals("token", token)
          assertEquals("secret/data/API_TOKEN", path)
          return "vault-secret"
        }
      },
    )

    assertEquals("vault-secret", resolver.resolve("API_TOKEN"))
  }

  @Test
  fun `aws secrets manager resolver uses cli output when configured`() {
    val resolver = AwsSecretsManagerSecretResolver(
      env = mapOf("AWS_REGION" to "eu-west-1"),
      commandRunner = SecretCommandRunner { command ->
        assertEquals("aws", command.first())
        SecretCommandResult(0, "aws-secret")
      },
    )

    assertEquals("aws-secret", resolver.resolve("API_TOKEN"))
  }

  @Test
  fun `gcp secrets manager resolver uses cli output when configured`() {
    val resolver = GcpSecretManagerSecretResolver(
      env = mapOf("GOOGLE_CLOUD_PROJECT" to "architect-dev"),
      commandRunner = SecretCommandRunner { command ->
        assertEquals("gcloud", command.first())
        SecretCommandResult(0, "gcp-secret")
      },
    )

    assertEquals("gcp-secret", resolver.resolve("API_TOKEN"))
  }
}