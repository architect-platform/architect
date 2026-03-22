package io.github.architectplatform.plugins.terraform

data class TerraformContext(
  val workspace: String = "default",
  val backend: String = "",
  val vars: Map<String, String> = emptyMap(),
  val varFile: String = "",
  val autoApprove: Boolean = false,
  val enabled: Boolean = true,
)
