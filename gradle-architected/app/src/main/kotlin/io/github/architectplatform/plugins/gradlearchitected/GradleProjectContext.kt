package io.github.architectplatform.plugins.gradlearchitected

data class GradleProjectContext(
    val name: String,
    val path: String = ".",
    val githubPackageRelease: Boolean = false,
    val gradlePath: String = "./gradlew"
)
