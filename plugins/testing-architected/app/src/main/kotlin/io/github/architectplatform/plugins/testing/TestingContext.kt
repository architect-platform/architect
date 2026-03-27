package io.github.architectplatform.plugins.testing

data class TestingContext(
  val enabled: Boolean = true,
  val framework: String = "auto",
  val workingDirectory: String = ".",
  val parallel: Boolean = true,
  val retryFlaky: Int = 2,
  val commands: TestingCommands = TestingCommands(),
  val coverage: TestingCoverage = TestingCoverage(),
) {
  fun normalizedFramework(): String = framework.trim().lowercase()
}

data class TestingCommands(
  val unit: String? = null,
  val integration: String? = null,
  val e2e: String? = null,
  val coverage: String? = null,
)

data class TestingCoverage(
  val enabled: Boolean = true,
  val threshold: Int = 80,
  val reporter: List<String> = listOf("html", "lcov", "cobertura"),
  val reportPaths: List<String> = emptyList(),
) {
  fun normalizedReporters(): List<String> = reporter.map { it.trim().lowercase() }.filter { it.isNotBlank() }
}
