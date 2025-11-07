package io.github.architectplatform.plugins.docs.utils

/**
 * Utility class for security-related operations like sanitization and validation.
 */
object SecurityUtils {
    
    // Domain validation regex - validates RFC-compliant domain names
    private val DOMAIN_VALIDATION_REGEX =
        Regex("^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?(\\.[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?)*$")
    
    /**
     * Sanitizes a path to prevent command injection and directory traversal.
     * Allows alphanumeric characters, underscores, hyphens, forward slashes, and dots.
     * Prevents absolute paths, parent directory references, and special characters.
     *
     * @param path The path to sanitize
     * @return Sanitized path safe for shell commands
     */
    fun sanitizePath(path: String): String {
        // Remove any absolute path indicators
        val relativePath = path.removePrefix("/")
        // Remove parent directory references
        val noParentRefs = relativePath.replace("../", "").replace("/..", "")
        // Remove disallowed characters
        return noParentRefs.replace(Regex("[^a-zA-Z0-9/_.-]"), "")
    }
    
    /**
     * Sanitizes a Git branch name for safe shell execution.
     * Allows alphanumeric characters, underscores, hyphens, and forward slashes.
     *
     * @param branch The branch name to sanitize
     * @return Sanitized branch name
     */
    fun sanitizeBranch(branch: String): String {
        return branch.replace(Regex("[^a-zA-Z0-9/_-]"), "")
    }
    
    /**
     * Sanitizes a version string for safe shell execution.
     * Allows alphanumeric characters, dots, hyphens, and underscores.
     *
     * @param version The version string to sanitize
     * @return Sanitized version string
     */
    fun sanitizeVersion(version: String): String {
        return version.replace(Regex("[^a-zA-Z0-9._-]"), "")
    }
    
    /**
     * Validates a domain name using RFC-compliant rules.
     *
     * @param domain The domain to validate
     * @return True if the domain is valid, false otherwise
     */
    fun isValidDomain(domain: String): Boolean {
        return DOMAIN_VALIDATION_REGEX.matches(domain)
    }
}
