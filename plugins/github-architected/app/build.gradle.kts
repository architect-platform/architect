plugins {
  kotlin("jvm") version "1.9.25"
  jacoco
}

version = libs.versions.pluginGithubArtifact.get()

apply(from = "../../../gradle/architect-plugin-conventions.gradle.kts")

dependencies {
  implementation(libs.jackson.databind) // core Jackson
  implementation(libs.jackson.module.kotlin) // Kotlin support

}
