package io.github.architectplatform.plugins.nx

data class NxContext(
  val targets: List<String> = emptyList(),
  val parallel: Int = 3,
  val nxCloud: Boolean = false,
  val affected: Boolean = false,
  val enabled: Boolean = true,
)
