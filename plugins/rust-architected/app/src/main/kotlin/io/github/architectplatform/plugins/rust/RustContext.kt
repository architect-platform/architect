package io.github.architectplatform.plugins.rust

data class RustContext(
  val profile: String = "release",
  val features: List<String> = emptyList(),
  val target: String = "",
  val enabled: Boolean = true,
)
