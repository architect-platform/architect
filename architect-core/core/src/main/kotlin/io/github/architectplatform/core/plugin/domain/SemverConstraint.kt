package io.github.architectplatform.core.plugin.domain

/**
 * Simple semver version representation and constraint matching.
 *
 * Supports constraint operators: `^` (compatible), `~` (patch-level), `>=`, `>`, `<=`, `<`, `=` (exact).
 * Multiple constraints can be combined with a space (AND logic).
 */
data class SemverVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<SemverVersion> {

  override fun compareTo(other: SemverVersion): Int {
    if (major != other.major) return major - other.major
    if (minor != other.minor) return minor - other.minor
    return patch - other.patch
  }

  override fun toString(): String = "$major.$minor.$patch"

  companion object {
    fun parse(version: String): SemverVersion? {
      val cleaned = version.trimStart('v', 'V')
      val parts = cleaned.split(".")
      if (parts.size < 2) return null
      val major = parts[0].toIntOrNull() ?: return null
      val minor = parts[1].toIntOrNull() ?: return null
      val patch = if (parts.size >= 3) (parts[2].split("-")[0].toIntOrNull() ?: 0) else 0
      return SemverVersion(major, minor, patch)
    }
  }
}

/**
 * Parses and evaluates semver constraint strings.
 *
 * Examples:
 * - `"^1.2.0"` → `>=1.2.0 <2.0.0`
 * - `"~1.2.0"` → `>=1.2.0 <1.3.0`
 * - `">=1.0.0"` → `>=1.0.0`
 * - `">=1.0.0 <2.0.0"` → range
 * - `"1.2.3"` or `"=1.2.3"` → exact match
 */
object SemverConstraint {

  /**
   * Returns true if [version] satisfies the [constraint].
   */
  fun satisfies(version: String, constraint: String): Boolean {
    val v = SemverVersion.parse(version) ?: return false
    return satisfies(v, constraint)
  }

  fun satisfies(version: SemverVersion, constraint: String): Boolean {
    val parts = constraint.trim().split("\\s+".toRegex())
    return parts.all { matchesSingle(version, it) }
  }

  /**
   * Filters and returns the best (highest) version satisfying the constraint.
   */
  fun bestMatch(versions: List<String>, constraint: String): String? {
    return versions
      .mapNotNull { v -> SemverVersion.parse(v)?.let { it to v } }
      .filter { (sv, _) -> satisfies(sv, constraint) }
      .maxByOrNull { (sv, _) -> sv }
      ?.second
  }

  private fun matchesSingle(version: SemverVersion, constraint: String): Boolean {
    return when {
      constraint.startsWith("^") -> matchCaret(version, constraint.substring(1))
      constraint.startsWith("~") -> matchTilde(version, constraint.substring(1))
      constraint.startsWith(">=") -> {
        val target = SemverVersion.parse(constraint.substring(2)) ?: return false
        version >= target
      }
      constraint.startsWith(">") -> {
        val target = SemverVersion.parse(constraint.substring(1)) ?: return false
        version > target
      }
      constraint.startsWith("<=") -> {
        val target = SemverVersion.parse(constraint.substring(2)) ?: return false
        version <= target
      }
      constraint.startsWith("<") -> {
        val target = SemverVersion.parse(constraint.substring(1)) ?: return false
        version < target
      }
      constraint.startsWith("=") -> {
        val target = SemverVersion.parse(constraint.substring(1)) ?: return false
        version == target
      }
      else -> {
        // Exact match
        val target = SemverVersion.parse(constraint) ?: return false
        version == target
      }
    }
  }

  /** `^1.2.3` → `>=1.2.3 <2.0.0` */
  private fun matchCaret(version: SemverVersion, constraintVersion: String): Boolean {
    val target = SemverVersion.parse(constraintVersion) ?: return false
    if (version < target) return false
    return when {
      target.major > 0 -> version.major == target.major
      target.minor > 0 -> version.major == 0 && version.minor == target.minor
      else -> version == target
    }
  }

  /** `~1.2.3` → `>=1.2.3 <1.3.0` */
  private fun matchTilde(version: SemverVersion, constraintVersion: String): Boolean {
    val target = SemverVersion.parse(constraintVersion) ?: return false
    if (version < target) return false
    return version.major == target.major && version.minor == target.minor
  }
}
