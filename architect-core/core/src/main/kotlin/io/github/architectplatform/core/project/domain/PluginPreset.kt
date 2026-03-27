package io.github.architectplatform.core.project.domain

data class PluginPreset(
  val id: String,
  val description: String,
  val plugins: List<PluginPresetPlugin>,
)

data class PluginPresetPlugin(
  val id: String,
  val repo: String = "architectplatform/$id",
)
