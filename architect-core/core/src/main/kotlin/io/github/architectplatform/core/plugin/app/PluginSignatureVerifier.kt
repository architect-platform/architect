package io.github.architectplatform.core.plugin.app

import jakarta.inject.Singleton
import java.io.File
import java.io.IOException

interface PluginSignatureVerifier {
  fun verify(plugin: PluginConfig, pluginFile: File, signatureFile: File)
}

data class CommandResult(
  val exitCode: Int,
  val output: String,
)

@Singleton
open class GpgCommandRunner {
  open fun run(command: List<String>): CommandResult {
    try {
      val process = ProcessBuilder(command).redirectErrorStream(true).start()
      val output = process.inputStream.bufferedReader().use { it.readText() }
      val exitCode = process.waitFor()
      return CommandResult(exitCode, output)
    } catch (error: IOException) {
      throw IllegalStateException("Failed to execute '${command.first()}': ${error.message}", error)
    }
  }
}

@Singleton
open class GpgPluginSignatureVerifier(
  private val commandRunner: GpgCommandRunner,
  private val gpgCommand: String = System.getProperty("architect.plugins.signature.gpg-command", "gpg"),
) : PluginSignatureVerifier {

  override fun verify(plugin: PluginConfig, pluginFile: File, signatureFile: File) {
    if (plugin.trustedKeys.isEmpty()) {
      throw IllegalArgumentException(
        "Plugin '${plugin.name}' must declare at least one trusted key when verify-signature is true",
      )
    }
    if (!signatureFile.exists()) {
      throw IllegalArgumentException(
        "Signature file not found for plugin '${plugin.name}': ${signatureFile.absolutePath}",
      )
    }

    val result =
      commandRunner.run(
        listOf(
          gpgCommand,
          "--batch",
          "--no-tty",
          "--status-fd=1",
          "--verify",
          signatureFile.absolutePath,
          pluginFile.absolutePath,
        ),
      )

    if (result.exitCode != 0) {
      throw IllegalArgumentException(
        "Signature verification failed for plugin '${plugin.name}': ${result.output.trim()}",
      )
    }

    val signerKeyIds = parseSignerKeyIds(result.output)
    if (signerKeyIds.isEmpty()) {
      throw IllegalArgumentException(
        "Signature verification for plugin '${plugin.name}' succeeded but gpg reported no signer key",
      )
    }

    val trustedKeys = plugin.trustedKeys.map(::normalizeKey)
    val matchesTrustedKey = signerKeyIds.any { signerKey -> trustedKeys.any { signerKey.endsWith(it) } }
    if (!matchesTrustedKey) {
      throw IllegalArgumentException(
        "Plugin '${plugin.name}' was signed by untrusted key(s): ${signerKeyIds.joinToString(", ")}",
      )
    }
  }

  internal fun parseSignerKeyIds(output: String): Set<String> =
    output
      .lineSequence()
      .mapNotNull { line ->
        when {
          line.startsWith("[GNUPG:] VALIDSIG ") -> {
            line.substringAfter("[GNUPG:] VALIDSIG ").substringBefore(' ')
          }
          line.startsWith("[GNUPG:] GOODSIG ") -> {
            line.substringAfter("[GNUPG:] GOODSIG ").substringBefore(' ')
          }
          else -> null
        }
      }
      .map(::normalizeKey)
      .filter { it.isNotBlank() }
      .toSet()

  internal fun normalizeKey(key: String): String =
    key.replace("0x", "", ignoreCase = true).replace(Regex("[^A-Fa-f0-9]"), "").uppercase()
}