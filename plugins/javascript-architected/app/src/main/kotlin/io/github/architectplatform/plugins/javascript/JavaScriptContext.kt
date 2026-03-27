package io.github.architectplatform.plugins.javascript

/**
 * Context configuration for JavaScript/Node.js projects.
 *
 * Defines the package manager and working directory for JavaScript projects
 * that use npm, yarn (classic/berry), pnpm, or bun.
 *
 * @property packageManager The package manager to use. Defaults to npm.
 * @property yarnMode Yarn mode, only used when packageManager=yarn. Defaults to auto.
 * @property workingDirectory The directory containing package.json. Defaults to current directory.
 * @property publishAccess Package publish access level for package managers that support it.
 * @property defaultVersionBump Default bump for javascript-version task when no arg is supplied.
 */
data class JavaScriptContext(
  val packageManager: String = "npm",
  val yarnMode: String = "auto",
  val workingDirectory: String = ".",
  val publishAccess: String = "public",
  val defaultVersionBump: String = "patch",
) {
  fun normalizedPackageManager(): String = packageManager.trim().lowercase()

  fun normalizedYarnMode(): String = yarnMode.trim().lowercase()
}
