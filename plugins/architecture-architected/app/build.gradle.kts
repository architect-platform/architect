plugins {
  kotlin("jvm") version "1.9.25"
  jacoco
}

version = libs.versions.pluginDefaultArtifact.get()

apply(from = "../../../gradle/architect-plugin-conventions.gradle.kts")

kotlin { jvmToolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

dependencies {
  implementation(libs.architect.api.contract)

  testImplementation(libs.junit.jupiter.api)
  testRuntimeOnly(libs.junit.jupiter.engine)
}
