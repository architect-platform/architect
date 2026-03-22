package io.github.architectplatform.plugins.python

data class PythonContext(
  val tool: String = "uv",
  val pythonVersion: String = "",
  val testRunner: String = "pytest",
  val linter: String = "ruff",
  val enabled: Boolean = true,
)
