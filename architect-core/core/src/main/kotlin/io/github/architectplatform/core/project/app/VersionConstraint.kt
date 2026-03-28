package io.github.architectplatform.core.project.app

data class VersionConstraint(
  val raw: String,
  val requirements: List<Requirement>,
) {
  fun isSatisfiedBy(version: String): Boolean {
    val normalized = normalize(version)
    return requirements.all { requirement ->
      val comparison = compareVersions(normalized, requirement.version)
      when (requirement.operator) {
        Operator.EQ -> comparison == 0
        Operator.LT -> comparison < 0
        Operator.LTE -> comparison <= 0
        Operator.GT -> comparison > 0
        Operator.GTE -> comparison >= 0
      }
    }
  }

  data class Requirement(
    val operator: Operator,
    val version: String,
  )

  enum class Operator {
    EQ,
    LT,
    LTE,
    GT,
    GTE,
  }

  companion object {
    fun parse(rawConstraint: String?): VersionConstraint? {
      val trimmed = rawConstraint?.trim().orEmpty()
      if (trimmed.isBlank()) return null
      val normalized = trimmed.replace(",", " ")
      val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
      val requirements = tokens.map { parseRequirement(it) }
      return VersionConstraint(trimmed, requirements)
    }

    private fun parseRequirement(token: String): Requirement {
      val (operator, version) = when {
        token.startsWith(">=") -> Operator.GTE to token.removePrefix(">=")
        token.startsWith("<=") -> Operator.LTE to token.removePrefix("<=")
        token.startsWith("==") -> Operator.EQ to token.removePrefix("==")
        token.startsWith("=") -> Operator.EQ to token.removePrefix("=")
        token.startsWith(">") -> Operator.GT to token.removePrefix(">")
        token.startsWith("<") -> Operator.LT to token.removePrefix("<")
        else -> Operator.EQ to token
      }
      val cleaned = version.trim()
      require(cleaned.isNotEmpty()) { "Invalid version constraint segment '$token'" }
      return Requirement(operator, normalize(cleaned))
    }

    private fun normalize(version: String): String =
      version.removePrefix("v").substringBefore("-").trim()

    private fun compareVersions(a: String, b: String): Int {
      val partsA = a.removePrefix("v").split(".")
      val partsB = b.removePrefix("v").split(".")
      for (i in 0 until maxOf(partsA.size, partsB.size)) {
        val nA = partsA.getOrNull(i)?.toIntOrNull() ?: 0
        val nB = partsB.getOrNull(i)?.toIntOrNull() ?: 0
        if (nA != nB) return nA - nB
      }
      return 0
    }
  }
}
