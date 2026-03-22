package io.github.architectplatform.plugins.go

data class GoContext(
  val module: String = "",
  val ldflags: String = "",
  val outputBinary: String = "",
  val enabled: Boolean = true,
)
