package io.github.architectplatform.plugins.quality

data class QualityContext(
  val tools: List<QualityTool> = emptyList(),
  val gates: QualityGates = QualityGates(),
  val enabled: Boolean = true,
)

data class QualityTool(
  val name: String,
  val url: String? = null,
  val projectKey: String? = null,
  val configFile: String? = null,
)

data class QualityGates(
  val coverage: Int = 80,
  val duplications: Int = 3,
  val bugs: Int = 0,
  val vulnerabilities: Int = 0,
)
