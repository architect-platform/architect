package io.github.architectplatform.engine.core.plugin.app

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class GpgPluginSignatureVerifierTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `should accept signer when trusted key matches fingerprint suffix`() {
    val pluginFile = tempDir.resolve("plugin.jar")
    val signatureFile = tempDir.resolve("plugin.jar.asc")
    pluginFile.writeText("plugin")
    signatureFile.writeText("signature")

    val verifier =
      GpgPluginSignatureVerifier(
        commandRunner =
          object : GpgCommandRunner() {
            override fun run(command: List<String>): CommandResult =
              CommandResult(
                exitCode = 0,
                output = "[GNUPG:] VALIDSIG 0123456789ABCDEF0123456789ABCDEFABCD1234 test test",
              )
          },
        gpgCommand = "gpg",
      )

    assertDoesNotThrow {
      verifier.verify(
        PluginConfig(
          name = "signed-plugin",
          verifySignature = true,
          trustedKeys = listOf("0xABCD1234"),
        ),
        pluginFile.toFile(),
        signatureFile.toFile(),
      )
    }
  }

  @Test
  fun `should reject signer when key is not trusted`() {
    val pluginFile = tempDir.resolve("plugin.jar")
    val signatureFile = tempDir.resolve("plugin.jar.asc")
    pluginFile.writeText("plugin")
    signatureFile.writeText("signature")

    val verifier =
      GpgPluginSignatureVerifier(
        commandRunner =
          object : GpgCommandRunner() {
            override fun run(command: List<String>): CommandResult =
              CommandResult(
                exitCode = 0,
                output = "[GNUPG:] VALIDSIG 0123456789ABCDEF0123456789ABCDEFABCD1234 test test",
              )
          },
        gpgCommand = "gpg",
      )

    assertThrows(IllegalArgumentException::class.java) {
      verifier.verify(
        PluginConfig(
          name = "signed-plugin",
          verifySignature = true,
          trustedKeys = listOf("0xDEADBEEF"),
        ),
        pluginFile.toFile(),
        signatureFile.toFile(),
      )
    }
  }
}