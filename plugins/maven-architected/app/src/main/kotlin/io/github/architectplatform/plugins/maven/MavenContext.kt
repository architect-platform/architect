package io.github.architectplatform.plugins.maven

data class MavenContext(
  val profiles: List<String> = emptyList(),
  val settings: String = "",
  val skipTests: Boolean = false,
  val enabled: Boolean = true,
)
