package io.github.architectplatform.plugins.security

data class SecurityContext(
  val enabled: Boolean = true,
  val workingDirectory: String = ".",
  val scan: SecurityScan = SecurityScan(),
  val audit: SecurityAudit = SecurityAudit(),
  val sbom: SecuritySbom = SecuritySbom(),
)

data class SecurityScan(
  val enabled: Boolean = true,
  val tools: List<String> = listOf("trivy"),
  val failOn: String = "critical",
  val codeqlDatabase: String? = null,
  val codeqlQuerySuite: String? = null,
  val codeqlOutput: String = "build/reports/security/codeql.sarif",
  val commands: SecurityScanCommands = SecurityScanCommands(),
) {
  fun normalizedTools(): List<String> = tools.map { it.trim().lowercase() }.filter { it.isNotBlank() }

  fun threshold(): SecuritySeverity = SecuritySeverity.fromConfig(failOn, "security.scan.failOn")
}

data class SecurityAudit(
  val enabled: Boolean = true,
  val tools: List<String> = emptyList(),
  val failOn: String = "high",
  val commands: SecurityAuditCommands = SecurityAuditCommands(),
) {
  fun normalizedTools(): List<String> = tools.map { it.trim().lowercase() }.filter { it.isNotBlank() }

  fun threshold(): SecuritySeverity = SecuritySeverity.fromConfig(failOn, "security.audit.failOn")
}

data class SecuritySbom(
  val enabled: Boolean = true,
  val format: String = "cyclonedx",
  val output: String = "sbom.json",
  val tool: String = "trivy",
  val command: String? = null,
) {
  fun normalizedFormat(): String = format.trim().lowercase()

  fun normalizedTool(): String = tool.trim().lowercase()
}

data class SecurityScanCommands(
  val trivy: String? = null,
  val snyk: String? = null,
  val codeql: String? = null,
)

data class SecurityAuditCommands(
  val npmAudit: String? = null,
  val pipAudit: String? = null,
  val cargoAudit: String? = null,
)

enum class SecuritySeverity(private val rank: Int) {
  UNKNOWN(0),
  LOW(1),
  MEDIUM(2),
  HIGH(3),
  CRITICAL(4),
  ;

  fun breaches(threshold: SecuritySeverity): Boolean = this != UNKNOWN && rank >= threshold.rank

  companion object {
    fun fromConfig(value: String, description: String): SecuritySeverity {
      return fromText(value)
        ?: throw IllegalArgumentException(
          "Invalid $description: '$value'. Expected one of critical, high, medium, or low.",
        )
    }

    fun fromText(value: String?): SecuritySeverity? =
      when (value?.trim()?.lowercase()) {
        "critical" -> CRITICAL
        "high" -> HIGH
        "medium", "moderate" -> MEDIUM
        "low" -> LOW
        else -> null
      }
  }
}
