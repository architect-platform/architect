package io.github.architectplatform.plugins.gradlearchitected

/**
 * Configuration for a single Gradle project.
 *
 * Defines the settings needed to execute Gradle commands for a specific
 * project within a multi-project setup.
 *
 * @property name Identifier for this Gradle project
 * @property path Relative path from the Architect project root to the Gradle project
 * @property githubPackageRelease Whether this project should be published to GitHub Packages
 * @property gradlePath Path to the Gradle wrapper script (gradlew or gradlew.bat)
 */
data class GradleProjectContext(
    val name: String,
    val path: String = ".",
    val githubPackageRelease: Boolean = false,
    val gradlePath: String = "./gradlew"
)
