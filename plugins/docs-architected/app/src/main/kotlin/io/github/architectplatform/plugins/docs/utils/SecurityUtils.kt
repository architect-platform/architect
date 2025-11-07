package io.github.architectplatform.plugins.docs.utils

/**
 * Utility class for security-related operations like sanitization and validation.
 */
object SecurityUtils {

    // Domain validation regex (relaxed RFC 1035 style)
    // - Must have at least one dot (no single-label domains)
    // - No leading/trailing dots or hyphens
    // - Allows multiple subdomains
    private val DOMAIN_VALIDATION_REGEX =
        Regex("^(?!-)(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,}$")

    /**
     * Sanitizes a path to prevent command injection and directory traversal.
     * - Removes absolute path indicators
     * - Removes parent directory references
     * - Removes disallowed characters like ; & |
     *
     * @param path The path to sanitize
     * @return Sanitized path safe for shell commands
     */
    fun sanitizePath(path: String): String {
        // Remove any absolute path indicator
        var sanitized = path.removePrefix("/")
        // Remove parent directory references
        sanitized = sanitized.replace("../", "")
            .replace("/..", "")
        // Remove everything not allowed: letters, numbers, /, _, ., or -
        sanitized = sanitized.replace(Regex("[^a-zA-Z0-9/_.-]"), "")
        // Normalize duplicate slashes if any
        sanitized = sanitized.replace(Regex("/{2,}"), "/")
        return sanitized
    }

    /**
     * Sanitizes a Git branch name for safe shell execution.
     * Allows alphanumeric characters, underscores, hyphens, and forward slashes.
     * Removes special characters like ; & |
     */
    fun sanitizeBranch(branch: String): String {
        // Remove everything except allowed characters
        var sanitized = branch.replace(Regex("[^a-zA-Z0-9/_-]"), "")
        // Trim trailing slashes or dots if they appear after sanitization
        sanitized = sanitized.trimEnd('/', '.')
        return sanitized
    }

    /**
     * Sanitizes a version string for safe shell execution.
     * Allows alphanumeric characters, dots, hyphens, and underscores.
     */
    fun sanitizeVersion(version: String): String {
        return version.replace(Regex("[^a-zA-Z0-9._-]"), "")
    }

    /**
     * Validates a domain name using simplified RFC rules.
     *
     * - Must not start or end with a hyphen or dot
     * - Must contain at least one dot
     * - Each label: 1–63 chars, alphanumeric or hyphen, cannot start/end with hyphen
     */
    fun isValidDomain(domain: String): Boolean {
        return DOMAIN_VALIDATION_REGEX.matches(domain)
    }
}
