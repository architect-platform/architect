package io.github.architectplatform.plugins.javascriptarchitected

/**
 * Context configuration for JavaScript/Node.js projects.
 *
 * Defines the package manager and working directory for JavaScript projects
 * that use npm, yarn, or pnpm.
 *
 * @property packageManager The package manager to use (npm, yarn, pnpm). Defaults to npm.
 * @property workingDirectory The directory containing package.json. Defaults to current directory.
 */
data class JavaScriptContext(
    val packageManager: String = "npm",
    val workingDirectory: String = "."
)
