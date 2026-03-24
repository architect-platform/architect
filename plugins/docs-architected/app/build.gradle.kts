plugins {
  kotlin("jvm") version "1.9.25"
  jacoco
}

version = libs.versions.pluginDocsArtifact.get()

apply(from = "../../../gradle/architect-plugin-conventions.gradle.kts")

dependencies {
  implementation(libs.jackson.databind) // core Jackson
  implementation(libs.jackson.module.kotlin) // Kotlin support

  testImplementation("org.mockito:mockito-core:5.8.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}
