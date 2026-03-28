package io.github.architectplatform.cli

object CliVersion {
  fun current(): String {
    return System.getProperty("architect.cli.version")
      ?: CliVersion::class.java.`package`?.implementationVersion
      ?: "dev"
  }
}
