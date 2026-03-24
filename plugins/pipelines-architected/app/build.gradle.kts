plugins {
  kotlin("jvm") version "1.9.25"
  jacoco
}

version = libs.versions.pluginDefaultArtifact.get()

apply(from = "../../../gradle/architect-plugin-conventions.gradle.kts")

kotlin { jvmToolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

repositories {
  mavenLocal()
}

dependencies {
  implementation(libs.architect.api.contract)
  implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")

  testImplementation(libs.junit.jupiter.api)
  testRuntimeOnly(libs.junit.jupiter.engine)
}
